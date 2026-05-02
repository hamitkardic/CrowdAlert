package com.example.crowdalert.data.repository

import com.example.crowdalert.data.model.Incident
import kotlinx.coroutines.flow.Flow

/** Abstraction for observing and writing incidents in Firestore. */
interface IncidentRepository {
    fun observeIncidents(): Flow<List<Incident>>

    suspend fun reportIncident(incident: NewIncident): Result<String>

    suspend fun deleteIncidents(ids: Collection<String>): Result<Unit>

    suspend fun updateIncident(
        id: String,
        update: IncidentUpdate,
    ): Result<Unit>
}

/**
 * Payload for creating a new incident (document id is assigned by Firestore).
 */
data class NewIncident(
    val title: String,
    val type: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
)

data class IncidentUpdate(
    val title: String,
    val type: String,
    val severity: String?,
    val description: String?,
)
