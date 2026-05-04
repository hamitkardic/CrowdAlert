package com.example.crowdalert.ui.incidents

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.repository.AuthRepository
import com.example.crowdalert.data.repository.IncidentRepository
import com.example.crowdalert.data.repository.IncidentUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyIncidentsViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
    authRepository: AuthRepository,
) : ViewModel() {
    val currentUserId: StateFlow<String?> =
        authRepository
            .currentUser()
            .map { it?.uid }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    val incidents: StateFlow<List<Incident>> =
        currentUserId
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    Log.d("MyIncidentsViewModel", "observeMyIncidents userId=$userId")
                    incidentRepository.observeMyIncidents(userId)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

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
