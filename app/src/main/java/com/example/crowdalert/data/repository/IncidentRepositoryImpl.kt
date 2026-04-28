package com.example.crowdalert.data.repository

import com.example.crowdalert.data.model.Incident
import com.google.firebase.auth.FirebaseAuth
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
                        close(error)
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

    private companion object {
        const val COLLECTION_INCIDENTS = "incidents"
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toIncidentOrNull(): Incident? {
    val t = getString("title") ?: return null
    val ty = getString("type") ?: return null
    val desc = getString("description")
    val lat = (get("latitude") as? Number)?.toDouble() ?: return null
    val lon = (get("longitude") as? Number)?.toDouble() ?: return null
    val reporterId = getString("reporterId")
    val created = getTimestamp("createdAt")?.toDate()?.time
    return Incident(
        id = id,
        title = t,
        type = ty,
        description = desc,
        latitude = lat,
        longitude = lon,
        reporterId = reporterId,
        createdAtMillis = created,
    )
}
