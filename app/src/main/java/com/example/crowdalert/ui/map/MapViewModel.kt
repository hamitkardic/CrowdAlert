package com.example.crowdalert.ui.map

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.repository.AuthRepository
import com.example.crowdalert.data.repository.IncidentRepository
import com.example.crowdalert.data.repository.IncidentUpdate
import com.example.crowdalert.data.repository.ReporterProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@HiltViewModel
class MapViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    @ApplicationContext context: Context,
    authRepository: AuthRepository,
) : ViewModel() {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private var incidentsJob: Job? = null
    private var hasInitializedSeenIncidentIds = false
    private var userLocation: Location? = null
    private val seenIncidentIds = mutableSetOf<String>()
    private val dismissedIncidentIds =
        preferences.getStringSet(KEY_DISMISSED_INCIDENT_IDS, emptySet()).orEmpty().toMutableSet()
    private val queuedAlerts = mutableListOf<Incident>()
    private var currentAlertIndex = 0

    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents: StateFlow<List<Incident>> = _incidents.asStateFlow()

    private val _incidentAlert = MutableStateFlow<IncidentAlertState?>(null)
    val incidentAlert: StateFlow<IncidentAlertState?> = _incidentAlert.asStateFlow()

    val currentUserId: StateFlow<String?> =
        authRepository
            .currentUser()
            .map { it?.uid }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val _focusedIncident = MutableStateFlow<IncidentMapTarget?>(null)
    val focusedIncident: StateFlow<IncidentMapTarget?> = _focusedIncident.asStateFlow()

    init {
        startIncidentListener()
    }

    private fun startIncidentListener() {
        incidentsJob?.cancel()
        incidentsJob =
            viewModelScope.launch {
                incidentRepository
                    .observeIncidents()
                    .catch { _incidents.value = emptyList() }
                    .collect { incidents ->
                        updateIncidentAlerts(incidents)
                        _incidents.value = incidents
                    }
            }
    }

    fun updateUserLocation(location: Location?) {
        userLocation = location
    }

    fun dismissCurrentIncidentAlert() {
        val incident = _incidentAlert.value?.incident ?: return
        dismissedIncidentIds += incident.id
        preferences
            .edit()
            .putStringSet(KEY_DISMISSED_INCIDENT_IDS, dismissedIncidentIds)
            .apply()

        currentAlertIndex += 1
        refreshCurrentAlert()
    }

    fun focusIncident(incident: Incident) {
        _focusedIncident.value =
            IncidentMapTarget(
                latitude = incident.latitude,
                longitude = incident.longitude,
            )
    }

    fun onFocusedIncidentHandled() {
        _focusedIncident.value = null
    }

    fun onSignedOut() {
        incidentsJob?.cancel()
        incidentsJob = null
        _incidents.value = emptyList()
        _focusedIncident.value = null
        _incidentAlert.value = null
        queuedAlerts.clear()
        currentAlertIndex = 0
        seenIncidentIds.clear()
        hasInitializedSeenIncidentIds = false
    }

    fun deleteIncidents(
        ids: Collection<String>,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            onComplete(incidentRepository.deleteIncidents(ids))
        }
    }

    fun updateIncident(
        id: String,
        update: IncidentUpdate,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            onComplete(incidentRepository.updateIncident(id, update))
        }
    }

    fun getReporterProfile(
        userId: String,
        onComplete: (ReporterProfile?) -> Unit,
    ) {
        viewModelScope.launch {
            onComplete(incidentRepository.getReporterProfile(userId).getOrNull())
        }
    }

    private fun updateIncidentAlerts(incidents: List<Incident>) {
        if (!hasInitializedSeenIncidentIds) {
            hasInitializedSeenIncidentIds = true
            seenIncidentIds += incidents.map { it.id }
            queueAlerts(incidents)
            return
        }

        val newIncidents = incidents.filter { seenIncidentIds.add(it.id) }
        queueAlerts(newIncidents)
    }

    private fun queueAlerts(incidents: List<Incident>) {
        val alerts =
            incidents
                .filter(::isAfterLastSeenTimestamp)
                .filterNot { it.id in dismissedIncidentIds }
                .filter(::isWithinAlertDistance)
                .filterNot { candidate -> queuedAlerts.any { it.id == candidate.id } }
                .sortedByDescending { it.createdAtMillis ?: 0L }
        if (alerts.isEmpty()) return

        val wasEmpty = queuedAlerts.isEmpty()
        queuedAlerts += alerts
        if (wasEmpty) {
            currentAlertIndex = 0
        }
        refreshCurrentAlert()
    }

    private fun refreshCurrentAlert() {
        while (currentAlertIndex < queuedAlerts.size && queuedAlerts[currentAlertIndex].id in dismissedIncidentIds) {
            currentAlertIndex += 1
        }

        _incidentAlert.value =
            queuedAlerts.getOrNull(currentAlertIndex)?.let { incident ->
                IncidentAlertState(
                    incident = incident,
                    position = currentAlertIndex + 1,
                    total = queuedAlerts.size,
                )
            }

        if (_incidentAlert.value == null) {
            queuedAlerts.clear()
            currentAlertIndex = 0
        }
    }

    private fun isWithinAlertDistance(incident: Incident): Boolean {
        val location = userLocation ?: return true
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            incident.latitude,
            incident.longitude,
            results,
        )
        return results.first() <= ALERT_DISTANCE_METERS
    }

    private fun isAfterLastSeenTimestamp(incident: Incident): Boolean {
        val createdAt = incident.createdAtMillis ?: return false
        return createdAt > preferences.getLong(KEY_LAST_SEEN_TIMESTAMP, 0L)
    }

    private companion object {
        const val PREFERENCES_NAME = "app_settings"
        const val KEY_LAST_SEEN_TIMESTAMP = "last_seen_timestamp"
        const val KEY_DISMISSED_INCIDENT_IDS = "dismissed_incident_ids"
        const val ALERT_DISTANCE_METERS = 500_000f
    }
}

data class IncidentAlertState(
    val incident: Incident,
    val position: Int,
    val total: Int,
)

data class IncidentMapTarget(
    val latitude: Double,
    val longitude: Double,
)
