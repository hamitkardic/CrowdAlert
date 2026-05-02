package com.example.crowdalert.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.repository.AuthRepository
import com.example.crowdalert.data.repository.IncidentRepository
import com.example.crowdalert.data.repository.IncidentUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
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

/**
 * Map screen state: live incident list from Firestore and optional location.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    private var incidentsJob: Job? = null
    private val _incidents = MutableStateFlow<List<Incident>>(emptyList())
    val incidents: StateFlow<List<Incident>> = _incidents.asStateFlow()

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
                    .collect { _incidents.value = it }
            }
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
}

data class IncidentMapTarget(
    val latitude: Double,
    val longitude: Double,
)
