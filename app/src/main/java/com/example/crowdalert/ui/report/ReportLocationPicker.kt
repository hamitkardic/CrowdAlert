package com.example.crowdalert.ui.report

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.graphics.PointF
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun ReportLocationPicker(
    latitude: Double,
    longitude: Double,
    enabled: Boolean,
    onLocationSelected: (latitude: Double, longitude: Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestOnLocationSelected by rememberUpdatedState(onLocationSelected)
    val selectedLocation = remember(latitude, longitude) { LatLng(latitude, longitude) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var cameraVersion by remember { mutableIntStateOf(0) }
    var isDraggingMarker by remember { mutableStateOf(false) }
    var draggedPinOffset by remember { mutableStateOf<Offset?>(null) }
    val pinSize = 28.dp
    val pinSizePx = with(LocalDensity.current) { pinSize.toPx() }
    val mapView =
        remember {
            MapLibre.getInstance(context)
            MapView(context).apply { onCreate(Bundle()) }
        }

    val projectedPinOffset =
        remember(map, selectedLocation, mapSize, cameraVersion) {
            map
                ?.projection
                ?.toScreenLocation(selectedLocation)
                ?.let { Offset(it.x, it.y) }
                ?: Offset(mapSize.width / 2f, mapSize.height / 2f)
        }
    val pinOffset = draggedPinOffset ?: projectedPinOffset

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

    LaunchedEffect(selectedLocation, map, isDraggingMarker) {
        if (!isDraggingMarker) {
            map?.animateCamera(CameraUpdateFactory.newLatLng(selectedLocation))
        }
    }

    Box(modifier = modifier) {

        AndroidView(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { mapSize = it },
            factory = {
                mapView.apply {
                    getMapAsync { mapLibreMap ->
                        map = mapLibreMap
                        mapLibreMap.setStyle(Style.Builder().fromUri(OPEN_FREE_MAP_STYLE))
                        mapLibreMap.cameraPosition =
                            CameraPosition.Builder()
                                .target(selectedLocation)
                                .zoom(REPORT_PICKER_ZOOM)
                                .build()

                        mapLibreMap.addOnCameraIdleListener {
                            cameraVersion += 1
                        }
                        mapLibreMap.addOnMapClickListener { latLng ->
                            if (!enabled) return@addOnMapClickListener true
                            latestOnLocationSelected(latLng.latitude, latLng.longitude)
                            mapLibreMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
                            true
                        }
                    }
                }
            },
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(pinSize)
                .offsetToPin(pinOffset, pinSizePx)
                .background(Color(0xFFE53935), CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .pointerInput(enabled, map, mapSize) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = {
                            isDraggingMarker = true
                            draggedPinOffset = pinOffset
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nextOffset = (draggedPinOffset ?: pinOffset) + dragAmount
                            draggedPinOffset = nextOffset
                            map?.projection
                                ?.fromScreenLocation(PointF(nextOffset.x, nextOffset.y))
                                ?.let { latLng ->
                                    latestOnLocationSelected(latLng.latitude, latLng.longitude)
                                }
                        },
                        onDragEnd = {
                            isDraggingMarker = false
                            draggedPinOffset = null
                        },
                        onDragCancel = {
                            isDraggingMarker = false
                            draggedPinOffset = null
                        },
                    )
                },
        )
    }
}

private fun Modifier.offsetToPin(offset: Offset, pinSizePx: Float): Modifier =
    this.then(
        Modifier.offset {
            IntOffset(
                x = (offset.x - pinSizePx / 2f).toInt(),
                y = (offset.y - pinSizePx / 2f).toInt(),
            )
        },
    )

fun Context.lastKnownIncidentLocation(): Pair<Double, Double>? {
    val hasFineLocation =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation && !hasCoarseLocation) return null

    val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return runCatching {
        val providers =
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        providers
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull(Location::getTime)
            ?.let { it.latitude to it.longitude }
    }.getOrNull()
}

@Suppress("DEPRECATION")
suspend fun Context.reverseGeocodeIncidentLocation(latitude: Double, longitude: Double): String? =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null

        runCatching {
            Geocoder(this@reverseGeocodeIncidentLocation, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.toReadableAddress()
        }.getOrNull()
    }

private fun Address.toReadableAddress(): String? =
    getAddressLine(0)
        ?: listOfNotNull(
            featureName,
            thoroughfare,
            subLocality,
            locality,
            adminArea,
            countryName,
        ).distinct().joinToString(", ").takeIf { it.isNotBlank() }

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val REPORT_PICKER_ZOOM = 15.0
