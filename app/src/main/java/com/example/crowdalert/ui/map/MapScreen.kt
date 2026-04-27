package com.example.crowdalert.ui.map

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R
import com.example.crowdalert.data.model.Incident
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

/**
 * Map screen backed by Firestore's realtime incident listener.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRoute(
    viewModel: MapViewModel,
    onOpenReport: () -> Unit,
    onSignOut: () -> Unit,
) {
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()

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
            IncidentMap(
                incidents = incidents,
                modifier = Modifier.fillMaxSize(),
            )
            IncidentCountBadge(
                incidentCount = incidents.size,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
private fun IncidentMap(
    incidents: List<Incident>,
    modifier: Modifier = Modifier,
) {
    val cameraState =
        rememberCameraState(
            firstPosition =
                CameraPosition(
                    target = Position(DEFAULT_LONGITUDE, DEFAULT_LATITUDE),
                    zoom = DEFAULT_ZOOM,
                ),
        )
    val geoJson = remember(incidents) { incidents.toGeoJsonFeatureCollection() }

    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_STYLE),
        cameraState = cameraState,
        onMapLoadFailed = {
            // Keep the Firestore overlay visible even if a public tile endpoint is unavailable.
        },
    ) {
        val incidentSource =
            rememberGeoJsonSource(
                data = GeoJsonData.JsonString(geoJson),
            )
        CircleLayer(
            id = "incident-markers",
            source = incidentSource,
            color = const(Color(0xFFE53935)),
            radius = const(8.dp),
            strokeColor = const(Color.White),
            strokeWidth = const(2.dp),
        )
    }
}

@Composable
private fun IncidentCountBadge(
    incidentCount: Int,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
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

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DEFAULT_LATITUDE = 41.0082
private const val DEFAULT_LONGITUDE = 28.9784
private const val DEFAULT_ZOOM = 10.0
