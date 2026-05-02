package com.example.crowdalert.ui.map

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.data.model.Incident
import kotlinx.coroutines.delay
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
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val focusedIncident by viewModel.focusedIncident.collectAsStateWithLifecycle()
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            hasLocationPermission =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                context.hasLocationPermission()
        }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
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
                onFocusedIncidentHandled = viewModel::onFocusedIncidentHandled,
                modifier = Modifier.fillMaxSize(),
            )
            IncidentCountBadge(
                incidentCount = incidents.size,
                onClick = onOpenIncidents,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(16.dp),
            )
            MyLocationButton(
                onClick = { centerOnUserLocationRequest += 1 },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .safeDrawingPadding()
                    .padding(end = 16.dp, bottom = 88.dp),
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
    onFocusedIncidentHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val geoJson = remember(incidents) { incidents.toGeoJsonFeatureCollection() }
    val latestGeoJson by rememberUpdatedState(geoJson)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var locationComponentReady by remember { mutableStateOf(false) }
    var hasCenteredOnUserLocation by remember { mutableStateOf(false) }
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

    LaunchedEffect(hasLocationPermission, map) {
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!hasLocationPermission) {
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

    LaunchedEffect(map, locationComponentReady, focusedIncident) {
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!locationComponentReady || hasCenteredOnUserLocation || focusedIncident != null) {
            return@LaunchedEffect
        }

        centerOnBestKnownUserLocation(
            context = context,
            mapLibreMap = mapLibreMap,
            zoom = USER_LOCATION_OPEN_ZOOM,
        )
        hasCenteredOnUserLocation = true
    }

    LaunchedEffect(centerOnUserLocationRequest, map, locationComponentReady) {
        if (centerOnUserLocationRequest == 0) return@LaunchedEffect
        val mapLibreMap = map ?: return@LaunchedEffect
        if (!locationComponentReady) return@LaunchedEffect

        centerOnBestKnownUserLocation(
            context = context,
            mapLibreMap = mapLibreMap,
            zoom = USER_LOCATION_OPEN_ZOOM,
        )
    }

    LaunchedEffect(focusedIncident, map) {
        val target = focusedIncident ?: return@LaunchedEffect
        val mapLibreMap = map ?: return@LaunchedEffect
        hasCenteredOnUserLocation = true
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
                            .target(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
                            .zoom(DEFAULT_ZOOM)
                            .build()
                    mapLibreMap.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                        style.upsertIncidentLayer(latestGeoJson)
                        locationComponentReady =
                            mapLibreMap.enableBlueLocationComponent(
                                context = context,
                                style = style,
                                hasLocationPermission = hasLocationPermission,
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
        context.lastKnownLocation()?.let { location ->
            locationComponent.forceLocationUpdate(location)
        }
        true
    }.getOrDefault(false)
}

private suspend fun centerOnBestKnownUserLocation(
    context: Context,
    mapLibreMap: MapLibreMap,
    zoom: Double,
) {
    val location =
        mapLibreMap.awaitLocationComponentLocation()
            ?: context.lastKnownLocation()

    val target =
        location?.let { LatLng(it.latitude, it.longitude) }
            ?: LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)

    mapLibreMap.animateCamera(
        CameraUpdateFactory.newLatLngZoom(target, zoom),
    )
}

private suspend fun MapLibreMap.awaitLocationComponentLocation(): Location? {
    repeat(LOCATION_WAIT_ATTEMPTS) {
        locationComponent.lastKnownLocation?.let { return it }
        delay(LOCATION_WAIT_INTERVAL_MS)
    }
    return locationComponent.lastKnownLocation
}

private fun Context.lastKnownLocation(): Location? {
    if (!hasLocationPermission()) return null

    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers =
        listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )

    return providers
        .mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull(Location::getTime)
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
private const val DEFAULT_LATITUDE = 41.0082
private const val DEFAULT_LONGITUDE = 28.9784
private const val DEFAULT_ZOOM = 10.0
private const val USER_LOCATION_OPEN_ZOOM = 14.0
private const val INCIDENT_FOCUS_ZOOM = 15.0
private const val INCIDENT_SOURCE_ID = "incident-source"
private const val INCIDENT_LAYER_ID = "incident-markers"
private const val LOCATION_BLUE = 0xFF2196F3.toInt()
private const val LOCATION_WAIT_ATTEMPTS = 15
private const val LOCATION_WAIT_INTERVAL_MS = 200L
private const val USER_LOCATION_UPDATE_INTERVAL_MS = 750L
private const val USER_LOCATION_FASTEST_INTERVAL_MS = 750L
