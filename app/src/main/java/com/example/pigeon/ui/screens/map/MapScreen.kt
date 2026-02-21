package com.example.pigeon.ui.screens.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.R
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.ui.screens.map.components.LatLongPill
import com.example.pigeon.ui.theme.MeshColor
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    val symbolManagerState = remember { mutableStateOf<SymbolManager?>(null) }
    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    
    // Threshold for showing titles
    val zoomThreshold = 14.0
    var currentZoom by remember { mutableDoubleStateOf(uiState.metadata.zoom) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            mapLibreMap?.getStyle { style ->
                enableLocationComponent(mapLibreMap!!, style, context)
            }
        }
    }

    fun requestLocationPermissions() {
        permissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Lifecycle-aware MapView, created ONCE
    val mapView = remember { 
        MapView(context).apply {
            getMapAsync { map ->
                mapLibreMap = map
                map.setStyle("https://demotiles.maplibre.org/style.json") { style ->
                    // Load the red SVG pin into the style
                    val pinBitmap = drawableToBitmap(context, R.drawable.ic_default_pin)
                    pinBitmap?.let { style.addImage("default-pin", it) }

                    val manager = SymbolManager(this@apply, map, style).apply {
                        iconAllowOverlap = true
                        textAllowOverlap = true
                    }
                    symbolManagerState.value = manager
                    
                    enableLocationComponent(map, style, context)
                    updateSymbols(manager, uiState.events, currentZoom >= zoomThreshold)

                    manager.addClickListener { symbol ->
                        selectedEvent = uiState.events.find { 
                            it.latitude == symbol.latLng.latitude && 
                            it.longitude == symbol.latLng.longitude 
                        }
                        true
                    }
                }
                
                map.addOnCameraMoveListener {
                    val camera = map.cameraPosition
                    val target = camera.target
                    if (target != null) {
                        currentZoom = camera.zoom
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

    // React to event changes OR zoom threshold cross to update symbols
    val showTitles = currentZoom >= zoomThreshold
    LaunchedEffect(uiState.events, showTitles) {
        symbolManagerState.value?.let { manager ->
            updateSymbols(manager, uiState.events, showTitles)
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
        containerColor = MeshColor.Background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        if (org.maplibre.android.location.permissions.PermissionsManager.areLocationPermissionsGranted(context)) {
                            try {
                                mapLibreMap?.locationComponent?.lastKnownLocation?.let { loc ->
                                    val latLng = LatLng(loc.latitude, loc.longitude)
                                    mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0))
                                }
                            } catch (e: SecurityException) {
                                requestLocationPermissions()
                            }
                        } else {
                            requestLocationPermissions()
                        }
                    },
                    containerColor = MeshColor.Surface,
                    contentColor = MeshColor.TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }

                FloatingActionButton(
                    onClick = { /* TODO: Open Reporting Wizard */ },
                    containerColor = MeshColor.Primary,
                    contentColor = MeshColor.Background
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report Event")
                }
            }
        }
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
            ConnectivityBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 64.dp)
            )

            // Event Detail Sheet Placeholder
            if (selectedEvent != null) {
                EventDetailSheet(
                    event = selectedEvent!!,
                    onClose = { selectedEvent = null },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun EventDetailSheet(
    event: Event,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = MeshColor.Surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MeshColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Add, contentDescription = "Close", modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "TYPE: ${event.eventType}",
                style = MaterialTheme.typography.labelSmall,
                color = MeshColor.Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MeshColor.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO: Resolve */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MeshColor.Primary)
            ) {
                Text("RESOLVE INCIDENT", color = MeshColor.Background)
            }
        }
    }
}

@Composable
fun ConnectivityBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        color = MeshColor.Surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "MESH STATUS: PASSIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeshColor.TextSecondary
                )
                Text(
                    text = "Synced: Just Now",
                    style = MaterialTheme.typography.bodySmall,
                    color = MeshColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MeshColor.SuccessGreen, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@SuppressLint("MissingPermission")
private fun enableLocationComponent(map: MapLibreMap, style: Style, context: android.content.Context) {
    if (org.maplibre.android.location.permissions.PermissionsManager.areLocationPermissionsGranted(context)) {
        try {
            val locationComponent = map.locationComponent
            val options = LocationComponentActivationOptions.builder(context, style).build()
            locationComponent.activateLocationComponent(options)
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS
        } catch (e: SecurityException) {
            // Log or handle the case where permission was revoked at runtime
        }
    }
}

private fun updateSymbols(manager: SymbolManager?, events: List<Event>, showTitles: Boolean) {
    manager?.deleteAll()
    events.forEach { event ->
        manager?.create(
            SymbolOptions()
                .withLatLng(LatLng(event.latitude, event.longitude))
                .withIconImage("default-pin")
                .withIconSize(1.5f)
                .withTextField(if (showTitles) event.title else "")
                .withTextOffset(arrayOf(0f, 1.5f))
                .withTextColor("rgba(23, 21, 17, 1)") // Tactical Black
                .withTextSize(12f)
        )
    }
}

private fun drawableToBitmap(context: android.content.Context, drawableId: Int): Bitmap? {
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
    val bitmap = Bitmap.createBitmap(
        drawable.intrinsicWidth,
        drawable.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
