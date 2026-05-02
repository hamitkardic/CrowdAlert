package com.example.crowdalert.data.repository

import com.example.crowdalert.data.model.Incident
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class IncidentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : IncidentRepository {

    override fun observeIncidents(): Flow<List<Incident>> = callbackFlow {
        val reg: ListenerRegistration =
            firestore
                .collection(COLLECTION_INCIDENTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        close()
                        return@addSnapshotListener
                    }
                    if (snapshot == null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list =
                        snapshot.documents.mapNotNull { it.toIncidentOrNull() }
                    trySend(list)
                }
        awaitClose { reg.remove() }
    }

    override suspend fun reportIncident(incident: NewIncident): Result<String> =
        runCatching {
            val data =
                hashMapOf(
                    "title" to incident.title,
                    "type" to incident.type,
                    "description" to incident.description,
                    "latitude" to incident.latitude,
                    "longitude" to incident.longitude,
                    "reporterId" to firebaseAuth.currentUser?.uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            val ref = firestore.collection(COLLECTION_INCIDENTS).add(data).await()
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

    private companion object {
        const val COLLECTION_INCIDENTS = "incidents"
    }
}

private fun DocumentSnapshot.requireOwnedBy(currentUid: String) {
    if (!exists()) error("Incident does not exist.")
    val ownerId = getString("reporterId") ?: getString("reportedBy")
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
    val reporterId = getString("reporterId") ?: getString("reportedBy")
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
        createdAtMillis = created,
    )
}
