package com.example.crowdalert.ui.map

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.data.model.Incident
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Map screen backed by Firestore's realtime incident listener.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRoute(
    viewModel: MapViewModel,
    onOpenReport: () -> Unit,
    onOpenIncidents: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val focusedIncident by viewModel.focusedIncident.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var isLocationEnabled by remember { mutableStateOf(context.isLocationEnabled()) }
    var hasShownLocationPrompt by remember { mutableStateOf(false) }
    val isLocationAvailable = hasLocationPermission && isLocationEnabled
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            hasLocationPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                context.hasLocationPermission()
            isLocationEnabled = context.isLocationEnabled()
        }

    DisposableEffect(context, lifecycle) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasLocationPermission = context.hasLocationPermission()
                    isLocationEnabled = context.isLocationEnabled()
                }
            }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    fun enableLocation() {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        } else {
            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    LaunchedEffect(isLocationAvailable) {
        if (!isLocationAvailable && !hasShownLocationPrompt) {
            hasShownLocationPrompt = true
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.map_location_prompt),
                    actionLabel = context.getString(R.string.map_location_prompt_enable),
                    duration = SnackbarDuration.Short,
                )
            if (result == SnackbarResult.ActionPerformed) {
                enableLocation()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                actions = {
                    TextButton(onClick = onSignOut) {
                        Text(stringResource(R.string.auth_sign_out))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenReport) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.report_fab_cd))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            var centerOnUserLocationRequest by remember { mutableStateOf(0) }

            IncidentMap(
                incidents = incidents,
                focusedIncident = focusedIncident,
                centerOnUserLocationRequest = centerOnUserLocationRequest,
                hasLocationPermission = hasLocationPermission,
                isLocationEnabled = isLocationEnabled,
                onFocusedIncidentHandled = viewModel::onFocusedIncidentHandled,
                modifier = Modifier.fillMaxSize(),
            )
            IncidentCountBadge(
                incidentCount = incidents.size,
                onClick = onOpenIncidents,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(start = 16.dp, top = 10.dp)
                    .offset(y = (-30).dp),
            )
            MyLocationButton(
                onClick = {
                    if (isLocationAvailable) {
                        centerOnUserLocationRequest += 1
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = context.getString(R.string.map_location_disabled_snackbar),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 16.dp, bottom = 35.dp),
            )
        }
    }
}

@Composable
private fun IncidentMap(
    incidents: List<Incident>,
    focusedIncident: IncidentMapTarget?,
    centerOnUserLocationRequest: Int,
    hasLocationPermission: Boolean,
    isLocationEnabled: Boolean,
    onFocusedIncidentHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val geoJson = remember(incidents) { incidents.toGeoJsonFeatureCollection() }
    val latestGeoJson by rememberUpdatedState(geoJson)
    val latestIncidents by rememberUpdatedState(incidents)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var locationComponentReady by remember { mutableStateOf(false) }
    var hasCenteredOnUserLocation by remember { mutableStateOf(false) }
    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    val mapView =
        remember {
            MapLibre.getInstance(context)
            MapView(context).apply { onCreate(Bundle()) }
        }

    DisposableEffect(mapView, lifecycle) {
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
        }

        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(geoJson, map) {
        map?.getStyle { style ->
            style.upsertIncidentLayer(geoJson)
        }
    }

    LaunchedEffect(incidents) {
        selectedIncident = selectedIncident?.takeIf { selected ->
            incidents.any { it.id == selected.id }
        }
    }

    DisposableEffect(map) {
        val mapLibreMap = map ?: return@DisposableEffect onDispose { }
        val clickListener =
            MapLibreMap.OnMapClickListener { point ->
                val selected =
                    mapLibreMap
                        .queryRenderedFeatures(
                            mapLibreMap.projection.toScreenLocation(point),
                            INCIDENT_LAYER_ID,
                        )
                        .firstOrNull()
                        ?.getStringProperty("id")
                        ?.let { id -> latestIncidents.firstOrNull { it.id == id } }

                if (selected == null) {
                    false
                } else {
                    selectedIncident = selected
                    mapLibreMap.disableLocationTracking()
                    mapLibreMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(selected.latitude, selected.longitude),
                            INCIDENT_FOCUS_ZOOM,
                        ),
                    )
                    true
                }
            }
        mapLibreMap.addOnMapClickListener(clickListener)

        onDispose {
            mapLibreMap.removeOnMapClickListener(clickListener)
        }
    }

    LaunchedEffect(hasLocationPermission, isLocationEnabled, map) {
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!hasLocationPermission || !isLocationEnabled) {
            locationComponentReady = false
            return@LaunchedEffect
        }

        mapLibreMap.getStyle { style ->
            locationComponentReady =
                mapLibreMap.enableBlueLocationComponent(
                    context = context,
                    style = style,
                )
        }
    }

    LaunchedEffect(map, hasLocationPermission, isLocationEnabled, locationComponentReady, focusedIncident) {
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!hasLocationPermission || hasCenteredOnUserLocation || focusedIncident != null) {
            return@LaunchedEffect
        }
        if (isLocationEnabled && !locationComponentReady) {
            return@LaunchedEffect
        }

        hasCenteredOnUserLocation =
            centerOnUserLocation(
                mapLibreMap = mapLibreMap,
                zoom = USER_LOCATION_OPEN_ZOOM,
            )
    }

    LaunchedEffect(centerOnUserLocationRequest, map, hasLocationPermission, isLocationEnabled) {
        if (centerOnUserLocationRequest == 0) return@LaunchedEffect
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!hasLocationPermission || !isLocationEnabled) return@LaunchedEffect

        centerOnUserLocation(
            mapLibreMap = mapLibreMap,
            zoom = USER_LOCATION_OPEN_ZOOM,
        )
    }

    LaunchedEffect(focusedIncident, map) {
        val target = focusedIncident ?: return@LaunchedEffect
        val mapLibreMap = map ?: return@LaunchedEffect
        hasCenteredOnUserLocation = true
        mapLibreMap.disableLocationTracking()
        mapLibreMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.latitude, target.longitude),
                INCIDENT_FOCUS_ZOOM,
            ),
        )
        onFocusedIncidentHandled()
    }

    AndroidView(
        modifier = modifier,
        factory = {
            mapView.apply {
                getMapAsync { mapLibreMap ->
                    map = mapLibreMap
                    mapLibreMap.cameraPosition =
                        CameraPosition.Builder()
                            .target(DEFAULT_CAMERA_TARGET)
                            .zoom(DEFAULT_CAMERA_ZOOM)
                            .build()
                    mapLibreMap.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                        style.upsertIncidentLayer(latestGeoJson)
                        locationComponentReady =
                            mapLibreMap.enableBlueLocationComponent(
                                context = context,
                                style = style,
                                hasLocationPermission = hasLocationPermission && isLocationEnabled,
                            )
                    }
                }
            }
        },
        update = {
            map?.getStyle { style ->
                style.upsertIncidentLayer(latestGeoJson)
            }
        },
    )

    selectedIncident?.let { incident ->
        IncidentDetailsBottomSheet(
            incident = incident,
            onDismiss = { selectedIncident = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncidentDetailsBottomSheet(
    incident: Incident,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var address by remember(incident.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(incident.id) {
        address =
            context.reverseGeocodeAddress(
                latitude = incident.latitude,
                longitude = incident.longitude,
            ) ?: context.getString(R.string.incidents_location_unknown)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IncidentTypeChip(type = incident.type)
                SeverityChip(severity = incident.severity)
            }
            Text(
                text = incident.title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            incident.description
                ?.takeIf { it.isNotBlank() }
                ?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            DetailSection(
                label = stringResource(R.string.map_incident_location),
                value = address ?: stringResource(R.string.incidents_location_resolving),
            )
            DetailSection(
                label = stringResource(R.string.map_incident_reported),
                value = incident.createdAtMillis.toReportedTimeLabel(),
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(R.string.map_incident_dismiss))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DetailSection(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun IncidentTypeChip(type: String) {
    val chipColor = type.incidentTypeColor()

    Surface(
        shape = RoundedCornerShape(50),
        color = chipColor.copy(alpha = 0.14f),
    ) {
        Text(
            text = type.incidentTypeLabel(),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp),
            color = chipColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SeverityChip(severity: String?) {
    val label = severity.severityLabel()
    val color = severity.severityColor()

    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SmallFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = Color(LOCATION_BLUE),
        contentColor = Color.White,
    ) {
        Icon(
            imageVector = MyLocationIcon,
            contentDescription = stringResource(R.string.map_my_location_cd),
        )
    }
}

@Composable
private fun IncidentCountBadge(
    incidentCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            ),
    ) {
        Text(
            text = stringResource(R.string.map_live_incident_count, incidentCount),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun Style.upsertIncidentLayer(geoJson: String) {
    val source = getSourceAs<GeoJsonSource>(INCIDENT_SOURCE_ID)
    if (source == null) {
        addSource(GeoJsonSource(INCIDENT_SOURCE_ID, geoJson))
    } else {
        source.setGeoJson(geoJson)
    }

    if (getLayer(INCIDENT_LAYER_ID) == null) {
        addLayer(
            CircleLayer(INCIDENT_LAYER_ID, INCIDENT_SOURCE_ID)
                .withProperties(
                    circleColor("#E53935"),
                    circleRadius(8f),
                    circleStrokeColor("#FFFFFF"),
                    circleStrokeWidth(2f),
                ),
        )
    }
}

@SuppressLint("MissingPermission")
private fun MapLibreMap.enableBlueLocationComponent(
    context: Context,
    style: Style,
    hasLocationPermission: Boolean = context.hasLocationPermission(),
): Boolean {
    if (!hasLocationPermission) return false

    val locationOptions =
        LocationComponentOptions.builder(context)
            .foregroundTintColor(LOCATION_BLUE)
            .backgroundTintColor(AndroidColor.WHITE)
            .accuracyColor(AndroidColor.argb(48, 33, 150, 243))
            .pulseEnabled(true)
            .pulseColor(LOCATION_BLUE)
            .pulseAlpha(0.35f)
            .pulseMaxRadius(36f)
            .build()
    val activationOptions =
        LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(locationOptions)
            .useDefaultLocationEngine(true)
            .locationEngineRequest(
                LocationEngineRequest.Builder(USER_LOCATION_UPDATE_INTERVAL_MS)
                    .setFastestInterval(USER_LOCATION_FASTEST_INTERVAL_MS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build(),
            )
            .build()

    return runCatching {
        locationComponent.activateLocationComponent(activationOptions)
        locationComponent.setLocationComponentEnabled(true)
        locationComponent.setCameraMode(CameraMode.NONE)
        locationComponent.setRenderMode(RenderMode.NORMAL)
        true
    }.getOrDefault(false)
}

private fun centerOnUserLocation(
    mapLibreMap: MapLibreMap,
    zoom: Double,
): Boolean {
    val location = mapLibreMap.currentLocation() ?: return false
    mapLibreMap.disableLocationTracking()
    mapLibreMap.animateCamera(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude),
            zoom,
        ),
    )
    return true
}

private fun MapLibreMap.currentLocation(): Location? =
    try {
        locationComponent.lastKnownLocation
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: Exception) {
        null
    }

private fun MapLibreMap.disableLocationTracking() {
    runCatching {
        locationComponent.setCameraMode(CameraMode.NONE)
    }
}

private fun Context.isLocationEnabled(): Boolean {
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        manager.isLocationEnabled
    } else {
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

private fun List<Incident>.toGeoJsonFeatureCollection(): String {
    val features = JSONArray()
    forEach { incident ->
        val geometry =
            JSONObject()
                .put("type", "Point")
                .put(
                    "coordinates",
                    JSONArray()
                        .put(incident.longitude)
                        .put(incident.latitude),
                )
        val properties =
            JSONObject()
                .put("id", incident.id)
                .put("title", incident.title)
                .put("type", incident.type)
        incident.severity?.let { properties.put("severity", it) }
        incident.description?.let { properties.put("description", it) }
        incident.createdAtMillis?.let { properties.put("createdAtMillis", it) }

        features.put(
            JSONObject()
                .put("type", "Feature")
                .put("id", incident.id)
                .put("geometry", geometry)
                .put("properties", properties),
        )
    }

    return JSONObject()
        .put("type", "FeatureCollection")
        .put("features", features)
        .toString()
}

@Suppress("DEPRECATION")
private suspend fun Context.reverseGeocodeAddress(
    latitude: Double,
    longitude: Double,
): String? =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            Geocoder(this@reverseGeocodeAddress, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.toReadableAddress()
        }.getOrNull()
    }

private fun Address.toReadableAddress(): String? =
    getAddressLine(0)
        ?: listOfNotNull(thoroughfare, subLocality, locality, adminArea, countryName)
            .distinct()
            .joinToString(", ")
            .takeIf { it.isNotBlank() }

private fun Long?.toReportedTimeLabel(): String =
    this?.let { timestamp ->
        if (DateUtils.isToday(timestamp)) {
            "Today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        } else {
            DateUtils
                .getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                )
                .toString()
        }
    } ?: "Unknown"

private fun String.incidentTypeLabel(): String =
    when (uppercase(Locale.ROOT)) {
        "ROAD_HAZARD" -> "Road hazard"
        "FLOOD" -> "Flood"
        "OUTAGE" -> "Outage"
        "FIGHT" -> "Fight"
        "MEDICAL" -> "Medical"
        "SUSPICIOUS" -> "Suspicious"
        "CROWD_SURGE" -> "Crowd surge"
        "THEFT" -> "Theft"
        "FIRE" -> "Fire"
        "HARASSMENT" -> "Harassment"
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
        "MEDICAL" -> Color(0xFF00897B)
        "FIRE" -> Color(0xFFE53935)
        else -> Color(0xFF5E35B1)
    }

private fun String?.severityLabel(): String =
    when (this?.uppercase(Locale.ROOT)) {
        "LOW" -> "Low"
        "MEDIUM" -> "Medium"
        "HIGH" -> "High"
        "CRITICAL" -> "Critical"
        null -> "Unspecified"
        else -> this.incidentTypeLabel()
    }

private fun String?.severityColor(): Color =
    when (this?.uppercase(Locale.ROOT)) {
        "LOW" -> Color(0xFF43A047)
        "MEDIUM" -> Color(0xFFFDD835)
        "HIGH" -> Color(0xFFFF9800)
        "CRITICAL" -> Color(0xFFE53935)
        else -> Color(0xFF9E9E9E)
    }

private val MyLocationIcon: ImageVector =
    ImageVector.Builder(
        name = "MyLocation",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(fill = SolidColor(Color.White)) {
            moveTo(12f, 8f)
            curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
            curveTo(8f, 14.21f, 9.79f, 16f, 12f, 16f)
            curveTo(14.21f, 16f, 16f, 14.21f, 16f, 12f)
            curveTo(16f, 9.79f, 14.21f, 8f, 12f, 8f)
            close()
            moveTo(20.94f, 11f)
            curveTo(20.48f, 6.83f, 17.17f, 3.52f, 13f, 3.06f)
            lineTo(13f, 1f)
            lineTo(11f, 1f)
            lineTo(11f, 3.06f)
            curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
            lineTo(1f, 11f)
            lineTo(1f, 13f)
            lineTo(3.06f, 13f)
            curveTo(3.52f, 17.17f, 6.83f, 20.48f, 11f, 20.94f)
            lineTo(11f, 23f)
            lineTo(13f, 23f)
            lineTo(13f, 20.94f)
            curveTo(17.17f, 20.48f, 20.48f, 17.17f, 20.94f, 13f)
            lineTo(23f, 13f)
            lineTo(23f, 11f)
            close()
            moveTo(12f, 19f)
            curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
            curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
            curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
            curveTo(19f, 15.87f, 15.87f, 19f, 12f, 19f)
            close()
        }
    }.build()

private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private val DEFAULT_CAMERA_TARGET = LatLng(0.0, 0.0)
private const val DEFAULT_CAMERA_ZOOM = 3.0
private const val USER_LOCATION_OPEN_ZOOM = 14.0
private const val INCIDENT_FOCUS_ZOOM = 15.0
private const val INCIDENT_SOURCE_ID = "incident-source"
private const val INCIDENT_LAYER_ID = "incident-markers"
private const val LOCATION_BLUE = 0xFF2196F3.toInt()
private const val USER_LOCATION_UPDATE_INTERVAL_MS = 750L
private const val USER_LOCATION_FASTEST_INTERVAL_MS = 750L
