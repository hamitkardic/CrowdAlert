package com.example.crowdalert.data.repository

import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.room.IncidentDao
import com.example.crowdalert.data.room.IncidentEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val incidentDao: IncidentDao,
) : IncidentRepository {

    override fun observeIncidents(): Flow<List<Incident>> =
        channelFlow {
            val roomJob =
                launch {
                    incidentDao
                        .observeAllIncidents()
                        .map { entities -> entities.map { it.toIncident() } }
                        .collect { send(it) }
                }
            val reg = startFirestoreIncidentCacheSync()
            awaitClose {
                reg.remove()
                roomJob.cancel()
            }
        }

    override fun observeMyIncidents(userId: String): Flow<List<Incident>> =
        channelFlow {
            val roomJob =
                launch {
                    incidentDao
                        .observeMyIncidents(userId)
                        .map { entities -> entities.map { it.toIncident() } }
                        .collect { send(it) }
                }
            val reg = startFirestoreIncidentCacheSync()
            awaitClose {
                reg.remove()
                roomJob.cancel()
            }
        }

    override suspend fun getReporterProfile(userId: String): Result<ReporterProfile> =
        runCatching {
            getStoredReporterProfile(userId)
        }

    override suspend fun reportIncident(incident: NewIncident): Result<String> =
        runCatching {
            val currentUser = firebaseAuth.currentUser
            val currentUserName =
                currentUser
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser
                        ?.uid
                        ?.let { uid ->
                            runCatching { getStoredReporterProfile(uid).name }.getOrNull()
                        }
            val data =
                hashMapOf(
                    "title" to incident.title,
                    "type" to incident.type,
                    "severity" to incident.severity,
                    "description" to incident.description,
                    "latitude" to incident.latitude,
                    "longitude" to incident.longitude,
                    "reportedBy" to currentUser?.uid,
                    "reporterId" to currentUser?.uid,
                    "reportedByEmail" to currentUser?.email,
                    "reportedByName" to currentUserName,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            val ref = firestore.collection(COLLECTION_INCIDENTS).add(data).await()
            incidentDao.insertIncident(
                IncidentEntity(
                    id = ref.id,
                    title = incident.title,
                    type = incident.type,
                    severity = incident.severity,
                    description = incident.description,
                    latitude = incident.latitude,
                    longitude = incident.longitude,
                    reportedBy = currentUser?.uid,
                    reportedByEmail = currentUser?.email,
                    reportedByName = currentUserName,
                    createdAt = System.currentTimeMillis(),
                    isSyncedToFirestore = true,
                ),
            )
            ref.id
        }

    override suspend fun deleteIncidents(ids: Collection<String>): Result<Unit> =
        runCatching {
            val currentUid = firebaseAuth.currentUser?.uid ?: error("You must be signed in.")
            ids
                .distinct()
                .forEach { id ->
                    val ref = firestore.collection(COLLECTION_INCIDENTS).document(id)
                    val snapshot = ref.get().await()
                    snapshot.requireOwnedBy(currentUid)
                    ref.delete().await()
                    incidentDao.deleteById(id)
                }
        }

    override suspend fun updateIncident(
        id: String,
        update: IncidentUpdate,
    ): Result<Unit> =
        runCatching {
            val currentUid = firebaseAuth.currentUser?.uid ?: error("You must be signed in.")
            val ref = firestore.collection(COLLECTION_INCIDENTS).document(id)
            val snapshot = ref.get().await()
            snapshot.requireOwnedBy(currentUid)
            ref
                .update(
                    mapOf(
                        "title" to update.title,
                        "type" to update.type,
                        "severity" to update.severity,
                        "description" to update.description,
                    ),
                ).await()
        }

    private fun CoroutineScope.startFirestoreIncidentCacheSync(): ListenerRegistration =
        firestore
            .collection(COLLECTION_INCIDENTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }
                val incidents = snapshot.documents.mapNotNull { it.toIncidentOrNull() }
                launch {
                    incidentDao.insertAll(incidents.map { it.toEntity() })
                    snapshot
                        .documentChanges
                        .filter { it.type == DocumentChange.Type.REMOVED }
                        .forEach { incidentDao.deleteById(it.document.id) }
                }
            }

    private suspend fun getStoredReporterProfile(userId: String): ReporterProfile {
        val userDocument =
            firestore
                .collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
        val currentUser = firebaseAuth.currentUser?.takeIf { it.uid == userId }
        return ReporterProfile(
            name =
                currentUser
                    ?.displayName
                    ?.takeIf { it.isNotBlank() }
                    ?: userDocument
                        .getString("name")
                        ?.takeIf { it.isNotBlank() },
            email =
                userDocument
                    .getString("email")
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser
                        ?.email
                        ?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        const val COLLECTION_INCIDENTS = "incidents"
        const val COLLECTION_USERS = "users"
    }
}

private fun IncidentEntity.toIncident(): Incident =
    Incident(
        id = id,
        title = title,
        type = type,
        description = description,
        latitude = latitude,
        longitude = longitude,
        severity = severity,
        reporterId = reportedBy,
        reporterEmail = reportedByEmail,
        reporterName = reportedByName,
        createdAtMillis = createdAt,
    )

private fun Incident.toEntity(isSyncedToFirestore: Boolean = true): IncidentEntity =
    IncidentEntity(
        id = id,
        title = title,
        type = type,
        severity = severity,
        description = description,
        latitude = latitude,
        longitude = longitude,
        reportedBy = reporterId,
        reportedByEmail = reporterEmail,
        reportedByName = reporterName,
        createdAt = createdAtMillis,
        isSyncedToFirestore = isSyncedToFirestore,
    )

private fun DocumentSnapshot.requireOwnedBy(currentUid: String) {
    if (!exists()) error("Incident does not exist.")
    val ownerId = getString("reportedBy") ?: getString("reporterId")
    check(ownerId == currentUid) {
        "You can only manage incidents you reported."
    }
}

private fun DocumentSnapshot.toIncidentOrNull(): Incident? {
    val t = getString("title") ?: return null
    val ty = getString("type") ?: return null
    val desc = getString("description")
    val lat = (get("latitude") as? Number)?.toDouble() ?: return null
    val lon = (get("longitude") as? Number)?.toDouble() ?: return null
    val severity = getString("severity")
    val reporterId = getString("reportedBy") ?: getString("reporterId")
    val reporterEmail = getString("reportedByEmail") ?: getString("reporterEmail")
    val reporterName = getString("reportedByName")
    val created = getTimestamp("createdAt")?.toDate()?.time
    return Incident(
        id = id,
        title = t,
        type = ty,
        description = desc,
        latitude = lat,
        longitude = lon,
        severity = severity,
        reporterId = reporterId,
        reporterEmail = reporterEmail,
        reporterName = reporterName,
        createdAtMillis = created,
    )
}
