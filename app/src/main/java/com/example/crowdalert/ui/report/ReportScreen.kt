package com.example.crowdalert.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.ui.report.ReportViewModel.IncidentType
import com.example.crowdalert.ui.report.ReportViewModel.Severity
import com.example.crowdalert.ui.report.ReportViewModel.SubmitState
import kotlinx.coroutines.delay

/**
 * Report form for creating Firestore incident documents.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportRoute(
    viewModel: ReportViewModel,
    onSubmitted: () -> Unit,
    onBackToMap: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val isSubmitting = submitState is SubmitState.Submitting
    var isPickerOpen by remember { mutableStateOf(false) }
    var draftLatitude by remember { mutableDoubleStateOf(uiState.latitude) }
    var draftLongitude by remember { mutableDoubleStateOf(uiState.longitude) }
    var draftAddress by remember { mutableStateOf(uiState.addressLabel) }
    val errorMessageRes =
        uiState.validationMessageRes
            ?: (submitState as? SubmitState.Error)?.messageRes

    LaunchedEffect(Unit) {
        context.lastKnownIncidentLocation()?.let { (latitude, longitude) ->
            viewModel.onLocationSelected(latitude, longitude)
        }
    }

    LaunchedEffect(uiState.latitude, uiState.longitude) {
        viewModel.onAddressResolved(context.getString(R.string.report_address_resolving))
        delay(300)
        val address =
            context.reverseGeocodeIncidentLocation(
                latitude = uiState.latitude,
                longitude = uiState.longitude,
            )
        viewModel.onAddressResolved(address ?: context.getString(R.string.report_address_unavailable))
    }

    LaunchedEffect(isPickerOpen, draftLatitude, draftLongitude) {
        if (!isPickerOpen) return@LaunchedEffect

        draftAddress = context.getString(R.string.report_address_resolving)
        delay(300)
        draftAddress =
            context.reverseGeocodeIncidentLocation(
                latitude = draftLatitude,
                longitude = draftLongitude,
            ) ?: context.getString(R.string.report_address_unavailable)
    }

    BackHandler(enabled = isPickerOpen) {
        isPickerOpen = false
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.Top),
            ) {
                IconButton(
                    onClick = onBackToMap,
                    enabled = !isSubmitting,
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp),
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.report_back_to_map),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = stringResource(R.string.report_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange,
                    label = { Text(stringResource(R.string.report_field_title)) },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.report_field_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IncidentType.entries.chunked(CHIPS_PER_ROW).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowTypes.forEach { type ->
                                FilterChip(
                                    selected = uiState.type == type,
                                    onClick = { viewModel.onTypeChange(type) },
                                    label = { Text(type.label()) },
                                    enabled = !isSubmitting,
                                    colors = FilterChipDefaults.filterChipColors
                                        (
                                                selectedContainerColor =
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.incidents_severity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Severity.entries.chunked(CHIPS_PER_ROW).forEach { rowSeverities ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowSeverities.forEach { severity ->
                                FilterChip(
                                    selected = uiState.severity == severity,
                                    onClick = { viewModel.onSeverityChange(severity) },
                                    label = { Text(severity.label()) },
                                    enabled = !isSubmitting,
                                    colors = FilterChipDefaults.filterChipColors
                                        (
                                                selectedContainerColor =
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor =
                                                    MaterialTheme.colorScheme.onPrimaryContainer,
                                        ),
                                )
                            }
                        }
                    }
                }
                LocationPreviewCard(
                    address = uiState.addressLabel,
                    enabled = !isSubmitting,
                    onChooseLocation = {
                        draftLatitude = uiState.latitude
                        draftLongitude = uiState.longitude
                        draftAddress = uiState.addressLabel
                        isPickerOpen = true
                    },
                )
                errorMessageRes?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text(stringResource(R.string.report_field_description)) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.submit(onSubmitted) },
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.report_submit),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            if (isPickerOpen) {
                FullscreenLocationPicker(
                    latitude = draftLatitude,
                    longitude = draftLongitude,
                    address = draftAddress,
                    onLocationSelected = { latitude, longitude ->
                        draftLatitude = latitude
                        draftLongitude = longitude
                    },
                    onCancel = { isPickerOpen = false },
                    onConfirm = {
                        viewModel.onLocationSelected(draftLatitude, draftLongitude)
                        viewModel.onAddressResolved(draftAddress)
                        isPickerOpen = false
                    },
                )
            }
        }
    }
}

@Composable
private fun LocationPreviewCard(
    address: String,
    enabled: Boolean,
    onChooseLocation: () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.report_location_label),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = address.ifBlank { stringResource(R.string.report_location_not_selected) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onChooseLocation,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.report_choose_location),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun FullscreenLocationPicker(
    latitude: Double,
    longitude: Double,
    address: String,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            ReportLocationPicker(
                latitude = latitude,
                longitude = longitude,
                enabled = true,
                onLocationSelected = onLocationSelected,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 16.dp, start = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp),
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.report_cancel_location),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Card(
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.report_location_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = address.ifBlank { stringResource(R.string.report_address_resolving) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = onConfirm,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.report_confirm_location),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentType.label(): String =
    when (this) {
        IncidentType.RoadHazard -> stringResource(R.string.report_type_road_hazard)
        IncidentType.Flood -> stringResource(R.string.report_type_flood)
        IncidentType.Fight -> stringResource(R.string.report_type_fight)
        IncidentType.Medical -> stringResource(R.string.report_type_medical)
        IncidentType.Suspicious -> stringResource(R.string.report_type_suspicious)
        IncidentType.CrowdSurge -> stringResource(R.string.report_type_crowd_surge)
        IncidentType.Theft -> stringResource(R.string.report_type_theft)
        IncidentType.Fire -> stringResource(R.string.report_type_fire)
        IncidentType.Harassment -> stringResource(R.string.report_type_harassment)
        IncidentType.Other -> stringResource(R.string.report_type_other)
    }

@Composable
private fun Severity.label(): String =
    when (this) {
        Severity.Low -> stringResource(R.string.severity_low)
        Severity.Medium -> stringResource(R.string.severity_medium)
        Severity.High -> stringResource(R.string.severity_high)
        Severity.Critical -> stringResource(R.string.severity_critical)
        Severity.Unspecified -> stringResource(R.string.severity_unspecified)
    }

private const val CHIPS_PER_ROW = 3
