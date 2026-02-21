package com.example.pigeon.ui.screens.map

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header: Mesh Status
        MeshHeader()

        Box(modifier = Modifier.weight(1f)) {
            // MapLibre View Container
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Top Overlays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Coordinate Pill (Left)
                LatLongPill(
                    latitude = uiState.metadata.latitude,
                    longitude = uiState.metadata.longitude
                )

                // Tool Stack (Right)
                ToolStack(
                    onMyLocationClick = {
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
                    onZoomIn = {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.zoomIn())
                    },
                    onZoomOut = {
                        mapLibreMap?.animateCamera(CameraUpdateFactory.zoomOut())
                    }
                )
            }

            // Bottom Actions (Large Buttons)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { /* TODO: Reporting Wizard */ },
                    modifier = Modifier
                        .height(64.dp)
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MeshColor.EmergencyRed),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Emergency,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "REPORT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

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
fun MeshHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        color = MeshColor.Surface,
        border = BorderStroke(1.dp, MeshColor.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse Icon
                Box(contentAlignment = Alignment.Center) {
                    val infiniteTransition = rememberInfiniteTransition()
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                    
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MeshColor.Primary.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WifiTethering,
                            contentDescription = "Mesh Active",
                            tint = MeshColor.MeshBlue,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha)) // Green 400
                            .border(1.5.dp, MeshColor.Surface, CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        "MESH ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MeshColor.TextPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Connected • Low Latency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MeshColor.TextSecondary
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "2m ago",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MeshColor.TextPrimary
                )
                Text(
                    "SYNCED",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MeshColor.TextSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ToolStack(
    onMyLocationClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Near Me Button
        Surface(
            onClick = onMyLocationClick,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MeshColor.Surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.NearMe,
                    contentDescription = "My Location",
                    tint = MeshColor.TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Zoom Pill
        Surface(
            modifier = Modifier.width(48.dp),
            shape = RoundedCornerShape(24.dp),
            color = MeshColor.Surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onZoomIn, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = "Zoom In", tint = MeshColor.TextPrimary)
                }
                Divider(modifier = Modifier.width(20.dp), color = MeshColor.TextPrimary.copy(alpha = 0.1f))
                IconButton(onClick = onZoomOut, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Zoom Out", tint = MeshColor.TextPrimary)
                }
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
                    Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(24.dp))
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
