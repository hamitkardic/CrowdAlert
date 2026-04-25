package com.example.crowdalert.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.crowdalert.R

/**
 * Placeholder report form: incident type and map-driven location are expanded later.
 */
@Composable
fun ReportRoute(
    viewModel: ReportViewModel,
    onSubmitted: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("OTHER") }
    var description by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Arrangement.Top),
    ) {
        Text(text = stringResource(R.string.report_title))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(stringResource(R.string.report_field_title)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = type,
            onValueChange = { type = it },
            label = { Text(stringResource(R.string.report_field_type)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.report_field_description)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = stringResource(R.string.report_location_stub))
        Button(
            onClick = {
                viewModel.submit(
                    title = title,
                    type = type,
                    description = description.ifBlank { null },
                    latitude = 0.0,
                    longitude = 0.0,
                    onDone = onSubmitted,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.report_submit))
        }
    }
}
