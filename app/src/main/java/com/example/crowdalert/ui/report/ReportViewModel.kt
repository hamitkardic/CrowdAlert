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
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun onTitleChange(value: String) {
        clearMessages()
        _uiState.update { it.copy(title = value) }
    }

    fun onTypeChange(value: IncidentType) {
        clearMessages()
        _uiState.update { it.copy(type = value) }
    }

    fun onDescriptionChange(value: String) {
        clearMessages()
        _uiState.update { it.copy(description = value) }
    }

    fun onLatitudeChange(value: String) {
        clearMessages()
        _uiState.update { it.copy(latitude = value) }
    }

    fun onLongitudeChange(value: String) {
        clearMessages()
        _uiState.update { it.copy(longitude = value) }
    }

    fun submit(onDone: () -> Unit) {
        val form = _uiState.value
        val title = form.title.trim()
        val latitude = form.latitude.toDoubleOrNull()
        val longitude = form.longitude.toDoubleOrNull()
        val validationMessage =
            when {
                title.isBlank() -> "Enter a title for the incident."
                latitude == null || latitude !in LATITUDE_RANGE -> "Enter a latitude between -90 and 90."
                longitude == null || longitude !in LONGITUDE_RANGE -> "Enter a longitude between -180 and 180."
                else -> null
            }

        if (validationMessage != null) {
            _uiState.update { it.copy(validationMessage = validationMessage) }
            return
        }

        val validLatitude = latitude ?: return
        val validLongitude = longitude ?: return

        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting
            val result =
                incidentRepository.reportIncident(
                    NewIncident(
                        title = title,
                        type = form.type.firestoreValue,
                        description = form.description.trim().ifBlank { null },
                        latitude = validLatitude,
                        longitude = validLongitude,
                    ),
                )
            result.fold(
                onSuccess = {
                    _uiState.value = ReportUiState()
                    _submitState.value = SubmitState.Idle
                    onDone()
                },
                onFailure = { e ->
                    _submitState.value = SubmitState.Error(e.message ?: "Error")
                },
            )
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(validationMessage = null) }
        if (_submitState.value is SubmitState.Error) {
            _submitState.value = SubmitState.Idle
        }
    }

    sealed interface SubmitState {
        data object Idle : SubmitState

        data object Submitting : SubmitState

        data class Error(val message: String) : SubmitState
    }

    data class ReportUiState(
        val title: String = "",
        val type: IncidentType = IncidentType.Other,
        val description: String = "",
        val latitude: String = DEFAULT_LATITUDE,
        val longitude: String = DEFAULT_LONGITUDE,
        val validationMessage: String? = null,
    )

    enum class IncidentType(val firestoreValue: String) {
        RoadHazard("ROAD_HAZARD"),
        Outage("OUTAGE"),
        Flood("FLOOD"),
        Other("OTHER"),
    }

    private companion object {
        const val DEFAULT_LATITUDE = "41.0082"
        const val DEFAULT_LONGITUDE = "28.9784"
        val LATITUDE_RANGE = -90.0..90.0
        val LONGITUDE_RANGE = -180.0..180.0
    }
}
