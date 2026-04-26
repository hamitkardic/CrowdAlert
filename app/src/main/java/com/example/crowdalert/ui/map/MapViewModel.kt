package com.example.crowdalert.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.repository.IncidentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Map screen state: live incident list from Firestore and optional location.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    incidentRepository: IncidentRepository,
) : ViewModel() {

    val incidents: StateFlow<List<Incident>> =
        incidentRepository
            .observeIncidents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())
}
