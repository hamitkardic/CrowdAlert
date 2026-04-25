package com.example.crowdalert.data.repository

import com.example.crowdalert.data.model.Incident
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Temporary stub. Replaced with Firestore [com.google.firebase.firestore.FirebaseFirestore]
 * snapshot listeners in the map step.
 */
@Singleton
class IncidentRepositoryImpl @Inject constructor() : IncidentRepository {
    override fun observeIncidents(): Flow<List<Incident>> = flowOf(emptyList())

    override suspend fun reportIncident(incident: NewIncident): Result<String> =
        Result.success("stub")
}
