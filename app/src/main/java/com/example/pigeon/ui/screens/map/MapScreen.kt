package com.example.pigeon.ui.screens.map

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.ui.screens.map.components.LatLongPill
import com.example.pigeon.ui.theme.MeshColor
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Tactical Situational Awareness Map.
 * Uses MapLibre Native SDK with full lifecycle management.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Lifecycle-aware MapView, created ONCE
    val mapView = remember { 
        MapView(context).apply {
            getMapAsync { map ->
                map.setStyle("https://demotiles.maplibre.org/style.json")
                map.addOnCameraMoveListener {
                    val camera = map.cameraPosition
                    val target = camera.target
                    if (target != null) {
                        viewModel.onMapMoved(
                            target.latitude,
                            target.longitude,
                            camera.zoom
                        )
                    }
                }
            }
        }
    }

    // Connect Lifecycle to MapView
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = MeshColor.Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // MapLibre View Container
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Top Pill Overlay (Lat/Long)
            LatLongPill(
                latitude = uiState.metadata.latitude,
                longitude = uiState.metadata.longitude,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )

            // Connectivity Bar / Mesh Status indicator (Placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(MeshColor.Surface.copy(alpha = 0.8f))
                    .padding(12.dp)
            ) {
                // Future status indicators
            }
        }
    }
}
