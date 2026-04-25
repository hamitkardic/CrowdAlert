package com.example.crowdalert.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crowdalert.R

/**
 * Map shell: MapLibre composable and GeoJSON pins are added in the map + Firestore step.
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
                .padding(inner)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.map_placeholder, incidents.size),
            )
        }
    }
}
