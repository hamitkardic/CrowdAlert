package com.example.crowdalert.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.ui.report.ReportViewModel.IncidentType
import com.example.crowdalert.ui.report.ReportViewModel.SubmitState

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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    val isSubmitting = submitState is SubmitState.Submitting
    val errorMessage =
        uiState.validationMessage
            ?: (submitState as? SubmitState.Error)?.message

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
        OutlinedTextField(
            value = uiState.latitude,
            onValueChange = viewModel::onLatitudeChange,
            label = { Text(stringResource(R.string.report_field_latitude)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.longitude,
            onValueChange = viewModel::onLongitudeChange,
            label = { Text(stringResource(R.string.report_field_longitude)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.report_field_description)) },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.report_location_note),
            style = MaterialTheme.typography.bodySmall,
        )
        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
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
}

@Composable
private fun IncidentType.label(): String =
    when (this) {
        IncidentType.RoadHazard -> stringResource(R.string.report_type_road_hazard)
        IncidentType.Outage -> stringResource(R.string.report_type_outage)
        IncidentType.Flood -> stringResource(R.string.report_type_flood)
        IncidentType.Other -> stringResource(R.string.report_type_other)
    }
