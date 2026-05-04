package com.example.crowdalert.ui.report

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.crowdalert.R
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

    fun onSeverityChange(value: Severity) {
        clearMessages()
        _uiState.update { it.copy(severity = value) }
    }

    fun onDescriptionChange(value: String) {
        clearMessages()
        _uiState.update { it.copy(description = value) }
    }

    fun onLocationSelected(latitude: Double, longitude: Double) {
        clearMessages()
        _uiState.update {
            it.copy(
                latitude = latitude.coerceIn(LATITUDE_RANGE),
                longitude = longitude.coerceIn(LONGITUDE_RANGE),
            )
        }
    }

    fun onAddressResolved(value: String) {
        _uiState.update { it.copy(addressLabel = value) }
    }

    fun submit(onDone: () -> Unit) {
        val form = _uiState.value
        val title = form.title.trim()
        val validationMessage =
            when {
                title.isBlank() -> R.string.report_error_title_required
                else -> null
            }

        if (validationMessage != null) {
            _uiState.update { it.copy(validationMessageRes = validationMessage) }
            return
        }

        viewModelScope.launch {
            _submitState.value = SubmitState.Submitting
            val result =
                incidentRepository.reportIncident(
                    NewIncident(
                        title = title,
                        type = form.type.firestoreValue,
                        severity = form.severity.firestoreValue,
                        description = form.description.trim().ifBlank { null },
                        latitude = form.latitude,
                        longitude = form.longitude,
                    ),
                )
            result.fold(
                onSuccess = {
                    _uiState.value = ReportUiState()
                    _submitState.value = SubmitState.Idle
                    onDone()
                },
                onFailure = {
                    _submitState.value = SubmitState.Error(R.string.report_submit_error)
                },
            )
        }
    }

    private fun clearMessages() {
        _uiState.update { it.copy(validationMessageRes = null) }
        if (_submitState.value is SubmitState.Error) {
            _submitState.value = SubmitState.Idle
        }
    }

    sealed interface SubmitState {
        data object Idle : SubmitState

        data object Submitting : SubmitState

        data class Error(@StringRes val messageRes: Int) : SubmitState
    }

    data class ReportUiState(
        val title: String = "",
        val type: IncidentType = IncidentType.Other,
        val severity: Severity = Severity.Unspecified,
        val description: String = "",
        val latitude: Double = DEFAULT_LATITUDE,
        val longitude: Double = DEFAULT_LONGITUDE,
        val addressLabel: String = "",
        @StringRes val validationMessageRes: Int? = null,
    )

    enum class IncidentType(val firestoreValue: String) {
        RoadHazard("ROAD_HAZARD"),
        Flood("FLOOD"),
        Fight("FIGHT"),
        Medical("MEDICAL"),
        Suspicious("SUSPICIOUS"),
        CrowdSurge("CROWD_SURGE"),
        Theft("THEFT"),
        Fire("FIRE"),
        Harassment("HARASSMENT"),
        Other("OTHER"),
    }

    enum class Severity(val firestoreValue: String?) {
        Low("LOW"),
        Medium("MEDIUM"),
        High("HIGH"),
        Critical("CRITICAL"),
        Unspecified(null),
    }

    private companion object {
        const val DEFAULT_LATITUDE = 41.0082
        const val DEFAULT_LONGITUDE = 28.9784
        val LATITUDE_RANGE = -90.0..90.0
        val LONGITUDE_RANGE = -180.0..180.0
    }
}
