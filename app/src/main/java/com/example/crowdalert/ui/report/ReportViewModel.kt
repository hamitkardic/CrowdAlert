package com.example.crowdalert.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.data.repository.IncidentRepository
import com.example.crowdalert.data.repository.NewIncident
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds report form fields and submits a new incident to Firestore.
 */
@HiltViewModel
class ReportViewModel @Inject constructor(
    private val incidentRepository: IncidentRepository,
) : ViewModel() {

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    fun submit(
        title: String,
        type: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting
            val result =
                incidentRepository.reportIncident(
                    NewIncident(
                        title = title,
                        type = type,
                        description = description,
                        latitude = latitude,
                        longitude = longitude,
                    ),
                )
            result.fold(
                onSuccess = {
                    _submitState.value = SubmitState.Idle
                    onDone()
                },
                onFailure = { e ->
                    _submitState.value = SubmitState.Error(e.message ?: "Error")
                },
            )
        }
    }

    sealed interface SubmitState {
        data object Idle : SubmitState

        data object Submitting : SubmitState

        data class Error(val message: String) : SubmitState
    }
}
