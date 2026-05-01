package com.example.crowdalert.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.ui.report.ReportViewModel.IncidentType
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
    val errorMessage =
        uiState.validationMessage
            ?: (submitState as? SubmitState.Error)?.message

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

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Top),
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
                )
            }
            Text(text = stringResource(R.string.report_title))
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
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                IncidentType.entries.forEach { type ->
                    FilterChip(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(type.label()) },
                        enabled = !isSubmitting,
                    )
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
            errorMessage?.let { message ->
                Text(
                    text = message,
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
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.report_submit))
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
            )
            Text(
                text = address.ifBlank { stringResource(R.string.report_location_not_selected) },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onChooseLocation,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.report_choose_location))
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
                .padding(top = 25.dp, start = 16.dp)
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
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.report_location_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = address.ifBlank { stringResource(R.string.report_address_resolving) },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        FloatingActionButton(
            onClick = onConfirm,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 127.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(R.string.report_confirm_location),
            )
        }
    }
}

@Composable
private fun IncidentType.label(): String =
    when (this) {
        IncidentType.RoadHazard -> stringResource(R.string.report_type_road_hazard)
        IncidentType.Outage -> stringResource(R.string.report_type_outage)
        IncidentType.Flood -> stringResource(R.string.report_type_flood)
        IncidentType.Other -> stringResource(R.string.report_type_other)
    }
