package com.example.crowdalert.ui.incidents

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.data.model.Incident
import com.example.crowdalert.data.repository.IncidentUpdate
import com.example.crowdalert.ui.map.MapViewModel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentsListRoute(
    viewModel: MapViewModel,
    onBack: () -> Unit,
    onIncidentSelected: (Incident) -> Unit,
) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var isManageMode by remember { mutableStateOf(false) }
    var selectedIncidentIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var editingIncident by remember { mutableStateOf<Incident?>(null) }
    val manageableIncidentIds =
        remember(incidents, currentUserId) {
            incidents
                .filter { it.isOwnedBy(currentUserId) }
                .map { it.id }
                .toSet()
        }
    val canManage = manageableIncidentIds.isNotEmpty()

    LaunchedEffect(manageableIncidentIds) {
        selectedIncidentIds = selectedIncidentIds.intersect(manageableIncidentIds)
        if (isManageMode && manageableIncidentIds.isEmpty()) {
            isManageMode = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.incidents_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.incidents_back_to_map),
                        )
                    }
                },
                actions = {
                    if (canManage) {
                        TextButton(
                            onClick = {
                                isManageMode = !isManageMode
                                if (!isManageMode) {
                                    selectedIncidentIds = emptySet()
                                }
                            },
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        if (isManageMode) {
                                            R.string.incidents_manage_done
                                        } else {
                                            R.string.incidents_manage
                                        },
                                    ),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (isManageMode) {
                ManageActionsBar(
                    hasSelection = selectedIncidentIds.isNotEmpty(),
                    onDelete = { showDeleteConfirmation = true },
                    onEdit = {
                        when (selectedIncidentIds.size) {
                            1 ->
                                editingIncident =
                                    incidents.firstOrNull { it.id == selectedIncidentIds.first() }

                            else ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Please select only one incident to edit",
                                    )
                                }
                        }
                    },
                )
            }
        },
    ) { inner ->
        IncidentsListScreen(
            incidents = incidents,
            currentUserId = currentUserId,
            isManageMode = isManageMode,
            selectedIncidentIds = selectedIncidentIds,
            onIncidentSelected = { incident ->
                if (!isManageMode) {
                    onIncidentSelected(incident)
                    return@IncidentsListScreen
                }
                if (!incident.isOwnedBy(currentUserId)) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "You can only manage incidents you reported",
                        )
                    }
                    return@IncidentsListScreen
                }
                selectedIncidentIds =
                    if (incident.id in selectedIncidentIds) {
                        selectedIncidentIds - incident.id
                    } else {
                        selectedIncidentIds + incident.id
                    }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.incidents_delete_title)) },
            text = { Text(stringResource(R.string.incidents_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val idsToDelete = selectedIncidentIds
                        showDeleteConfirmation = false
                        viewModel.deleteIncidents(idsToDelete) { result ->
                            scope.launch {
                                if (result.isSuccess) {
                                    selectedIncidentIds = emptySet()
                                    isManageMode = false
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message =
                                            result.exceptionOrNull()?.message
                                                ?: "Unable to delete selected incidents",
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(
                        text = stringResource(R.string.incidents_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.incidents_cancel))
                }
            },
        )
    }

    editingIncident?.let { incident ->
        EditIncidentDialog(
            incident = incident,
            onDismiss = { editingIncident = null },
            onSave = { update ->
                viewModel.updateIncident(incident.id, update) { result ->
                    scope.launch {
                        if (result.isSuccess) {
                            editingIncident = null
                            selectedIncidentIds = emptySet()
                            isManageMode = false
                        } else {
                            snackbarHostState.showSnackbar(
                                message =
                                    result.exceptionOrNull()?.message
                                        ?: "Unable to update incident",
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun IncidentsListScreen(
    incidents: List<Incident>,
    currentUserId: String?,
    isManageMode: Boolean,
    selectedIncidentIds: Set<String>,
    onIncidentSelected: (Incident) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var locationLabels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(incidents) {
        val activeIncidentIds = incidents.map { it.id }.toSet()
        val nextLabels =
            locationLabels
                .filterKeys { it in activeIncidentIds }
                .toMutableMap()

        incidents
            .filterNot { it.id in nextLabels }
            .forEach { incident ->
                nextLabels[incident.id] =
                    context.reverseGeocodeCityDistrict(
                        latitude = incident.latitude,
                        longitude = incident.longitude,
                    ) ?: context.getString(R.string.incidents_location_unknown)
            }

        locationLabels = nextLabels
    }

    if (incidents.isEmpty()) {
        Box(
            modifier = modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.incidents_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = incidents,
            key = { it.id },
        ) { incident ->
            IncidentCard(
                incident = incident,
                locationLabel = locationLabels[incident.id]
                    ?: stringResource(R.string.incidents_location_resolving),
                isManageMode = isManageMode,
                isSelected = incident.id in selectedIncidentIds,
                isManageable = incident.isOwnedBy(currentUserId),
                onClick = { onIncidentSelected(incident) },
            )
        }
    }
}

@Composable
private fun IncidentCard(
    incident: Incident,
    locationLabel: String,
    isManageMode: Boolean,
    isSelected: Boolean,
    isManageable: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (isManageMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    enabled = isManageable,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = locationLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IncidentTypeChip(type = incident.type)
                    Spacer(modifier = Modifier.weight(1f))
                    SeverityIndicator(severity = incident.severity)
                }
                Text(
                    text = incident.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ManageActionsBar(
    hasSelection: Boolean,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onDelete,
                enabled = hasSelection,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.incidents_delete))
            }
            Button(
                onClick = onEdit,
                enabled = hasSelection,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800),
                    ),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.incidents_edit))
            }
        }
    }
}

@Composable
private fun EditIncidentDialog(
    incident: Incident,
    onDismiss: () -> Unit,
    onSave: (IncidentUpdate) -> Unit,
) {
    var title by remember(incident.id) { mutableStateOf(incident.title) }
    var type by remember(incident.id) { mutableStateOf(incident.type) }
    var severity by remember(incident.id) { mutableStateOf(incident.severity ?: SEVERITY_UNSPECIFIED) }
    var description by remember(incident.id) { mutableStateOf(incident.description.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.incidents_edit_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.report_field_title)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.report_field_type),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChipSelector(
                        options = IncidentEditType.entries.map { it.firestoreValue },
                        selected = type,
                        label = { it.incidentTypeLabel() },
                        onSelected = { type = it },
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.incidents_severity),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChipSelector(
                        options = severityOptions,
                        selected = severity,
                        label = { it.severityLabel() },
                        onSelected = { severity = it },
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.report_field_description)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        IncidentUpdate(
                            title = title.trim(),
                            type = type,
                            severity = severity.takeUnless { it == SEVERITY_UNSPECIFIED },
                            description = description.trim().takeIf { it.isNotBlank() },
                        ),
                    )
                },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.incidents_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.incidents_cancel))
            }
        },
    )
}

@Composable
private fun ChipSelector(
    options: List<String>,
    selected: String,
    label: (String) -> String,
    onSelected: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selected == option,
                        onClick = { onSelected(option) },
                        label = { Text(label(option)) },
                    )
                }
            }
        }
    }
}

private fun Incident.isOwnedBy(currentUserId: String?): Boolean =
    reporterId != null && reporterId == currentUserId

@Composable
private fun IncidentTypeChip(type: String) {
    val chipColor = type.incidentTypeColor()

    Surface(
        shape = RoundedCornerShape(50),
        color = chipColor.copy(alpha = 0.14f),
    ) {
        Text(
            text = type.incidentTypeLabel(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = chipColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SeverityIndicator(severity: String?) {
    val label = severity.severityLabel()
    val color = severity.severityColor()

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Suppress("DEPRECATION")
private suspend fun Context.reverseGeocodeCityDistrict(
    latitude: Double,
    longitude: Double,
): String? =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            Geocoder(this@reverseGeocodeCityDistrict, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.toCityDistrictLabel()
        }.getOrNull()
    }

private fun Address.toCityDistrictLabel(): String? {
    val city = locality ?: subAdminArea ?: adminArea
    val district = subLocality ?: featureName

    return listOfNotNull(city, district)
        .distinct()
        .joinToString(" / ")
        .takeIf { it.isNotBlank() }
}

private fun String.incidentTypeLabel(): String =
    when (uppercase(Locale.ROOT)) {
        "ROAD_HAZARD" -> "Road hazard"
        "FLOOD" -> "Flood"
        "OUTAGE" -> "Outage"
        "FIGHT" -> "Fight"
        "OTHER" -> "Other"
        else ->
            lowercase(Locale.ROOT)
                .replace('_', ' ')
                .split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { char ->
                        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
                    }
                }
                .ifBlank { "Incident" }
    }

private fun String.incidentTypeColor(): Color =
    when (uppercase(Locale.ROOT)) {
        "ROAD_HAZARD" -> Color(0xFFFF9800)
        "FLOOD" -> Color(0xFF1E88E5)
        "FIGHT" -> Color(0xFFD32F2F)
        "OUTAGE" -> Color(0xFF6D4C41)
        else -> Color(0xFF5E35B1)
    }

private fun String?.severityLabel(): String =
    when (this?.uppercase(Locale.ROOT)) {
        "LOW" -> "Low"
        "MEDIUM" -> "Medium"
        "HIGH" -> "High"
        "CRITICAL" -> "Critical"
        null -> "Unspecified"
        else -> this?.incidentTypeLabel() ?: "Unspecified"
    }

private fun String?.severityColor(): Color =
    when (this?.uppercase(Locale.ROOT)) {
        "LOW" -> Color(0xFF43A047)
        "MEDIUM" -> Color(0xFFFFA000)
        "HIGH" -> Color(0xFFE53935)
        "CRITICAL" -> Color(0xFF8E24AA)
        else -> Color(0xFF9E9E9E)
    }

private enum class IncidentEditType(val firestoreValue: String) {
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

private val severityOptions =
    listOf(
        "LOW",
        "MEDIUM",
        "HIGH",
        "CRITICAL",
        SEVERITY_UNSPECIFIED,
    )

private const val SEVERITY_UNSPECIFIED = "UNSPECIFIED"
