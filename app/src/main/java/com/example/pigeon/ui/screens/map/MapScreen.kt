package com.example.pigeon.ui.screens.map

import android.annotation.SuppressLint
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.R
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.ui.screens.map.components.LatLongPill
import com.example.pigeon.ui.screens.map.components.ReportingWizardSheet
import com.example.pigeon.ui.screens.map.components.EventDetailSheet
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
import org.maplibre.android.style.layers.Property
import android.content.Context
import android.os.Looper
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.FillLayer

import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.engine.LocationEngineRequest
import com.google.gson.JsonPrimitive

import kotlinx.coroutines.awaitCancellation


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    reportViewModel: ReportViewModel = hiltViewModel(),
    detailViewModel: EventDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detailTrustScore by detailViewModel.trustScore.collectAsStateWithLifecycle()
    
    // Sync Selected Event ID to Detail ViewModel
    LaunchedEffect(uiState.selectedEvent?.eventId) {
        uiState.selectedEvent?.eventId?.let { id ->
            detailViewModel.onEventSelected(id)
        }
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    
    // Map State
    val symbolManagerState = remember { mutableStateOf<SymbolManager?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val verificationRadiusMeters = 500.0
    
    var showReportingWizard by remember { mutableStateOf(false) }
    var hasInitialZoomed by remember { mutableStateOf(false) }
    
    // Threshold for showing titles
    val zoomThreshold = 14.0
    var currentZoom by remember { mutableDoubleStateOf(uiState.metadata.zoom) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            mapLibreMap?.getStyle { style: Style ->
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
                
                val absolutePath = getOfflineMapPath(context)
                val mbtilesUri = "mbtiles://$absolutePath"
                
                // Permanent Offline Style
                try {
                    val styleJson = context.assets.open("style-offline.json").bufferedReader().use { it.readText() }
                    val updatedStyleJson = styleJson.replace("mbtiles://lebanon_base.mbtiles", mbtilesUri)
                    Log.d("MAP_DEBUG", "🔄 Initializing with Permanent Offline Style: $mbtilesUri")
                    map.setStyle(Style.Builder().fromJson(updatedStyleJson)) { style: Style ->
                        setupMapStyle(this@apply, map, style, context, uiState.events, uiState.trustScores, currentZoom, zoomThreshold, symbolManagerState, onEventClick = { id -> viewModel.onEventSelectedById(id) })
                    }
                } catch (e: Exception) {
                    Log.e("MAP_DEBUG", "❌ Offline style loading failed: ${e.message}")
                    // Fallback to direct asset (might fail if MBTiles uri isn't replaced, but best we can do)
                    map.setStyle("asset://style-offline.json") { style: Style ->
                        setupMapStyle(this@apply, map, style, context, uiState.events, uiState.trustScores, currentZoom, zoomThreshold, symbolManagerState, onEventClick = { id -> viewModel.onEventSelectedById(id) })
                    }
                }
                
                // Style Error & Map Failure Listeners
                this@apply.addOnDidFailLoadingMapListener { errorMessage ->
                    Log.e("MAP_DEBUG", "❌ Map Loading Failed: $errorMessage")
                }
                
                this@apply.addOnStyleImageMissingListener { imageName ->
                    Log.w("MAP_DEBUG", "⚠️ Style Image Missing: $imageName")
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

                // Initial location capture
                try {
                    map.locationComponent.lastKnownLocation?.let { 
                        userLocation = LatLng(it.latitude, it.longitude)
                        viewModel.updateLocation(userLocation!!)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    // Monitor User Location
    LaunchedEffect(mapLibreMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val locationComponent = map.locationComponent
        
        // Wait for activation
        while (!locationComponent.isLocationComponentActivated) {
            delay(500)
        }
        
        val engine = locationComponent.locationEngine ?: return@LaunchedEffect
        
        val listener = object : LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                result?.lastLocation?.let { location ->
                    val latLng = LatLng(location.latitude, location.longitude)
                    if (hasInitialZoomed) {
                        viewModel.updateLocation(latLng)
                    }
                }
            }
            override fun onFailure(exception: Exception) {}
        }
        
        val request = LocationEngineRequest.Builder(1000L).build()
        if (org.maplibre.android.location.permissions.PermissionsManager.areLocationPermissionsGranted(context)) {
            try {
                engine.requestLocationUpdates(request, listener, Looper.getMainLooper())
            } catch (e: SecurityException) {
                Log.e("MAP_DEBUG", "SecurityException during location updates: ${e.message}")
            }
        }
        
        try {
            awaitCancellation()
        } finally {
            engine.removeLocationUpdates(listener)
        }
    }

    // Update Proximity Circle Visuals
    LaunchedEffect(userLocation) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val loc = userLocation ?: return@LaunchedEffect
        
        map.getStyle { style ->
            val source = style.getSourceAs<GeoJsonSource>("proximity-source")
            if (source != null) {
                val geoJson = createRadiusPolygon(loc, verificationRadiusMeters, 64)
                source.setGeoJson(geoJson)
            }
        }
    }

    // Connection state removed - Map stays offline

    // React to event changes, trust score updates, OR zoom threshold cross to update symbols
    val showTitles = currentZoom >= zoomThreshold
    LaunchedEffect(uiState.events, uiState.trustScores, showTitles) {
        symbolManagerState.value?.let { manager ->
            mapLibreMap?.getStyle { style: Style ->
                updateSymbols(context, manager, style, uiState.events, uiState.trustScores, showTitles)
            }
        }
    }

    // Auto-zoom to user location on launch
    LaunchedEffect(mapLibreMap) {
        if (mapLibreMap != null && !hasInitialZoomed) {
             // Initial setup: camera to Lebanon center if offline
            val style = mapLibreMap?.style
            if (style?.url?.contains("style-offline.json") == true || style?.json != null) {
                val lebanonCenter = LatLng(33.8938, 35.5018)
                mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(lebanonCenter, 10.0))
                Log.d("MAP_DEBUG", "📍 Initial Camera to Lebanon Center")
            }

            // Then follow location
            while (!hasInitialZoomed) {
                val lastLocation = try {
                    mapLibreMap?.locationComponent?.lastKnownLocation
                } catch (e: Exception) {
                    null
                }
                if (lastLocation != null) {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastLocation.latitude, lastLocation.longitude),
                            14.0
                        )
                    )
                    hasInitialZoomed = true
                }
                delay(1000)
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Sticky Header: Mesh Status (Full Width)
        MeshHeader()

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize()
            )

            // Map Crosshair Overlay
            MapCrosshair(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
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

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { showReportingWizard = true },
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

            // Event Details Sheet
            uiState.selectedEvent?.let { event ->
                EventDetailSheet(
                    event = event,
                    trustScore = detailTrustScore,
                    isWithinRadius = uiState.isWithinRadius,
                    distanceMeters = uiState.distanceMeters,
                    onDismiss = { viewModel.onEventSelected(null) },
                    onResolve = { viewModel.onResolveEvent(it) },
                    onVerify = { detailViewModel.onVerify(it) }
                )
            }

            if (showReportingWizard) {
                ReportingWizardSheet(
                    onDismiss = { showReportingWizard = false },
                    onReport = { type, title, desc, ttlHours ->
                        reportViewModel.reportEvent(
                            eventType = type,
                            title = title,
                            description = desc,
                            ttlMillis = ttlHours * 60 * 60 * 1000,
                            latitude = uiState.metadata.latitude,
                            longitude = uiState.metadata.longitude
                        )
                    },
                    currentLatitude = uiState.metadata.latitude,
                    currentLongitude = uiState.metadata.longitude)
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
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pulse Icon
                    Box(contentAlignment = Alignment.Center) {
                        val infiniteTransition = rememberInfiniteTransition(label = "MeshPulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 0.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "PulseAlpha"
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
                                .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha))
                                .border(1.5.dp, MeshColor.Surface, CircleShape)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            "MESH ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MeshColor.TextPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "CONNECTED",
                            style = MaterialTheme.typography.bodySmall,
                            color = MeshColor.TextSecondary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "2m ago",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black,
                        color = MeshColor.TextPrimary
                    )
                    Text(
                        "SYNCED",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MeshColor.TextSecondary,
                        letterSpacing = 1.sp
                    )
                }
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

@SuppressLint("MissingPermission")
private fun setupMapStyle(
    mapView: MapView,
    map: MapLibreMap,
    style: Style,
    context: android.content.Context,
    events: List<Event>,
    trustScores: Map<String, com.example.pigeon.domain.model.TrustScore>,
    zoom: Double,
    zoomThreshold: Double,
    symbolManagerState: MutableState<SymbolManager?>,
    onEventClick: (String) -> Unit
) {
    // 1. Register icons (must be re-added on style change)
    val firePin = createTacticalPinFromDrawable(context, R.drawable.local_fire_department_24dp, MeshColor.EmergencyRed)
    val medicalPin = createTacticalPinFromDrawable(context, R.drawable.medical_services_24dp, MeshColor.EmergencyRed)
    val suppliesPin = createTacticalPinFromDrawable(context, R.drawable.package_2_24dp, MeshColor.MeshBlue)
    val conflictPin = createTacticalPinFromDrawable(context, R.drawable.warning_24dp, MeshColor.AlertOrange)
    val customPin = createTacticalPinFromDrawable(context, R.drawable.location_on_24dp, MeshColor.AssistYellow)
    val defaultPin = drawableToBitmap(context, R.drawable.ic_default_pin)

    style.addImage("pin-fire", firePin)
    style.addImage("pin-medical", medicalPin)
    style.addImage("pin-supplies", suppliesPin)
    style.addImage("pin-conflict", conflictPin)
    style.addImage("pin-custom", customPin)

    // Register faded variants (40% alpha)
    style.addImage("pin-fire-faded", createFadedBitmap(firePin, 102))
    style.addImage("pin-medical-faded", createFadedBitmap(medicalPin, 102))
    style.addImage("pin-supplies-faded", createFadedBitmap(suppliesPin, 102))
    style.addImage("pin-conflict-faded", createFadedBitmap(conflictPin, 102))
    style.addImage("pin-custom-faded", createFadedBitmap(customPin, 102))

    defaultPin?.let { style.addImage("default-pin", it) }

    // 2. Clear old manager if exists
    symbolManagerState.value?.let { oldManager ->
        try { oldManager.onDestroy() } catch (e: Exception) {}
    }
    
    // 3. Create new manager for the new style
    val manager = SymbolManager(mapView, map, style).apply {
        iconAllowOverlap = true
        textAllowOverlap = true
    }
    symbolManagerState.value = manager
    
    // 4. Setup interaction
    manager.addClickListener { symbol ->
        android.util.Log.d("MapScreenClick", "Symbol clicked! Data: ${symbol.data}")
        val eventId = symbol.data?.asString
        if (eventId != null) {
            onEventClick(eventId)
            return@addClickListener true
        } else {
            android.util.Log.d("MapScreenClick", "EventId was null from symbol.data")
        }
        false
    }

    // 5. Proximity Visuals Layer (Added BEFORE symbols so it sits underneath)
    if (style.getSource("proximity-source") == null) {
        style.addSource(GeoJsonSource("proximity-source"))
        val proximityLayer = FillLayer("proximity-layer", "proximity-source")
            .withProperties(
                fillColor("rgba(223, 156, 32, 0.15)"), // Operational Gold with 0.15 alpha
                fillOutlineColor("rgba(223, 156, 32, 0.4)")
            )
        // Add at top for testing visibility
        style.addLayer(proximityLayer)
    }

    // 6. Initial content
    enableLocationComponent(map, style, context)
    updateSymbols(context, manager, style, events, trustScores, zoom >= zoomThreshold)
    
    // MAP_DEBUG: Force camera to Lebanon if offline style is active
    if (style.url?.contains("style-offline.json") == true) {
        val lebanonCenter = LatLng(33.8938, 35.5018)
        map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(lebanonCenter, 10.0))
        Log.d("MAP_DEBUG", "📍 Offline Style detected: Forcing Camera to Lebanon Center (33.8938, 35.5018)")
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

private fun updateSymbols(
    context: android.content.Context, 
    manager: SymbolManager?, 
    style: Style, 
    events: List<Event>, 
    trustScores: Map<String, com.example.pigeon.domain.model.TrustScore>,
    showTitles: Boolean
) {
    manager?.deleteAll()
    events.forEach { event ->
        val trustScore = trustScores[event.eventId] ?: com.example.pigeon.domain.model.TrustScore.EMPTY
        val isVerified = trustScore.isVerified
        
        val iconRes = when (event.eventType) {
            EventType.FIRE -> R.drawable.local_fire_department_24dp
            EventType.MEDICAL -> R.drawable.medical_services_24dp
            EventType.SUPPLIES -> R.drawable.package_2_24dp
            EventType.CONFLICT -> R.drawable.warning_24dp
            EventType.CUSTOM, EventType.SOS -> R.drawable.location_on_24dp
        }
        
        val color = when (event.eventType) {
            EventType.FIRE -> MeshColor.EmergencyRed
            EventType.MEDICAL -> MeshColor.EmergencyRed
            EventType.SUPPLIES -> MeshColor.MeshBlue
            EventType.CONFLICT -> MeshColor.AlertOrange
            EventType.CUSTOM, EventType.SOS -> MeshColor.AssistYellow
        }

        if (showTitles) {
            // COMBINED VIEW: Icon + Label welded together
            val combinedId = "combined-${event.eventId}"
            // Note: Combined currently doesn't support easy fading without more bitmap logic. 
            // For now, only the Icon Only view will reflect trust for "Quick Glance" as requested.
            // But we can add a simple suffix to the cache key if we wanted to support it.
            if (style.getImage(combinedId) == null) {
                createCombinedPinFromDrawable(context, iconRes, color, event.title.uppercase())?.let {
                    style.addImage(combinedId, it)
                }
            }

            manager?.create(
                SymbolOptions()
                    .withLatLng(LatLng(event.latitude, event.longitude))
                    .withIconImage(combinedId)
                    .withIconAnchor(Property.ICON_ANCHOR_TOP)
                    .withIconOffset(arrayOf(0f, -0.5f)) // Anchor to top but offset so circle is on coord
                    .withIconSize(1.0f)
                    .withData(JsonPrimitive(event.eventId))
            )
        } else {
            // ICON ONLY VIEW
            val iconBase = when (event.eventType) {
                EventType.FIRE -> "pin-fire"
                EventType.MEDICAL -> "pin-medical"
                EventType.SUPPLIES -> "pin-supplies"
                EventType.CONFLICT -> "pin-conflict"
                EventType.CUSTOM, EventType.SOS -> "pin-custom"
            }
            
            val iconImage = if (isVerified) iconBase else "$iconBase-faded"

            manager?.create(
                SymbolOptions()
                    .withLatLng(LatLng(event.latitude, event.longitude))
                    .withIconImage(iconImage)
                    .withIconSize(1.0f)
                    .withData(JsonPrimitive(event.eventId))
            )
        }
    }
}

private fun createFadedBitmap(src: Bitmap, alpha: Int): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.alpha = alpha
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}

private fun createCombinedPinFromDrawable(
    context: android.content.Context,
    drawableId: Int,
    backgroundColor: Color,
    text: String
): Bitmap? {
    // 1. Measure Text Pill
    val textPaint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = "#171511".toColorInt()
        textSize = 32f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }
    
    val hPadding = 32f
    val vPadding = 20f
    val textBounds = android.graphics.Rect()
    textPaint.getTextBounds(text, 0, text.length, textBounds)
    val pillW = textBounds.width() + (hPadding * 2)
    val pillH = textBounds.height() + (vPadding * 2)
    
    // 2. Icon Circle Dimens
    val circleSize = 120f
    val overlap = 8f
    
    // 3. Total Dimens
    val totalW = Math.max(circleSize, pillW)
    val totalH = circleSize + pillH - overlap
    
    val bitmap = Bitmap.createBitmap(totalW.toInt(), totalH.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    // Position Circle at horizontal center
    val circleX = totalW / 2f
    val circleY = circleSize / 2f
    
    // Draw Circle
    paint.color = backgroundColor.toArgb()
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(circleX, circleY, circleSize / 2f, paint)
    
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(circleX, circleY, (circleSize / 2f) - 3f, paint)
    
    // Draw Icon from drawable
    val drawable = ContextCompat.getDrawable(context, drawableId)
    drawable?.let {
        it.setTint(android.graphics.Color.WHITE)
        val iconSize = (circleSize * 0.55f).toInt()
        val iconLeft = (circleX - (iconSize / 2f)).toInt()
        val iconTop = (circleY - (iconSize / 2f)).toInt()
        it.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        it.draw(canvas)
    }
    
    // Draw Pill below Circle
    val pillX = (totalW - pillW) / 2f
    val pillY = circleSize - overlap
    val rect = android.graphics.RectF(pillX, pillY, pillX + pillW, pillY + pillH)
    
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    canvas.drawRoundRect(rect, 24f, 24f, paint)
    
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 2f
    paint.color = "#E5E0D6".toColorInt()
    canvas.drawRoundRect(rect, 24f, 24f, paint)
    
    val textY = pillY + (pillH / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(text, totalW / 2f, textY, textPaint)
    
    return bitmap
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

private fun createTacticalPinFromDrawable(
    context: android.content.Context,
    drawableId: Int,
    backgroundColor: Color
): Bitmap {
    val sizePx = 120
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

    // 1. Background Circle
    paint.color = backgroundColor.toArgb()
    paint.style = android.graphics.Paint.Style.FILL
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

    // 2. White Border
    paint.style = android.graphics.Paint.Style.STROKE
    paint.strokeWidth = 6f
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, (sizePx / 2f) - 3f, paint)

    // 3. Render Drawable
    val drawable = ContextCompat.getDrawable(context, drawableId) ?: return bitmap
    drawable.setTint(android.graphics.Color.WHITE)
    val iconSize = (sizePx * 0.55f).toInt()
    val margin = (sizePx - iconSize) / 2
    drawable.setBounds(margin, margin, margin + iconSize, margin + iconSize)
    drawable.draw(canvas)

    return bitmap
}

@Composable
fun MapCrosshair(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = 1.5.dp.toPx()
        val center = Offset(size.width / 2, size.height / 2)
        val circleRadius = 8.dp.toPx()
        val gap = 4.dp.toPx()
        
        // Colors
        val primaryColor = MeshColor.Crosshair
        val extensionColor = MeshColor.TextSecondary.copy(alpha = 0.4f)
        val shadowColor = Color.Black.copy(alpha = 0.2f)
        val shadowOffset = 0.5.dp.toPx()

        // 1. Center Circle
        drawCircle(
            color = shadowColor,
            radius = circleRadius,
            center = center.copy(x = center.x + shadowOffset, y = center.y + shadowOffset),
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = primaryColor,
            radius = circleRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        // 2. Full-Screen Lines
        // Horizontal (Left)
        drawLine(
            color = extensionColor,
            start = Offset(0f, center.y),
            end = Offset(center.x - circleRadius - gap, center.y),
            strokeWidth = strokeWidth
        )
        // Horizontal (Right)
        drawLine(
            color = extensionColor,
            start = Offset(center.x + circleRadius + gap, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = strokeWidth
        )
        // Vertical (Top)
        drawLine(
            color = extensionColor,
            start = Offset(center.x, 0f),
            end = Offset(center.x, center.y - circleRadius - gap),
            strokeWidth = strokeWidth
        )
        // Vertical (Bottom)
        drawLine(
            color = extensionColor,
            start = Offset(center.x, center.y + circleRadius + gap),
            end = Offset(center.x, size.height),
            strokeWidth = strokeWidth
        )

        // 3. Inner Cross (Small solid segments near circle for focus)
        val focusLength = 12.dp.toPx()
        drawLine(
            color = primaryColor,
            start = Offset(center.x - circleRadius - gap - focusLength, center.y),
            end = Offset(center.x - circleRadius - gap, center.y),
            strokeWidth = strokeWidth * 1.5f
        )
        drawLine(
            color = primaryColor,
            start = Offset(center.x + circleRadius + gap, center.y),
            end = Offset(center.x + circleRadius + gap + focusLength, center.y),
            strokeWidth = strokeWidth * 1.5f
        )
        drawLine(
            color = primaryColor,
            start = Offset(center.x, center.y - circleRadius - gap - focusLength),
            end = Offset(center.x, center.y - circleRadius - gap),
            strokeWidth = strokeWidth * 1.5f
        )
        drawLine(
            color = primaryColor,
            start = Offset(center.x, center.y + circleRadius + gap),
            end = Offset(center.x, center.y + circleRadius + gap + focusLength),
            strokeWidth = strokeWidth * 1.5f
        )
    }
}

// Use standard Compose toArgb() instead of custom extension

/**
 * Retrieves the absolute path to the offline map file, copying it from assets if needed.
 */
private fun getOfflineMapPath(context: Context): String {
    val fileName = "lebanon_base.mbtiles"
    val file = File(context.filesDir, fileName)
    if (!file.exists()) {
        try {
            context.assets.open(fileName).use { inputStream ->
                java.io.FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            android.util.Log.d("MAP_DEBUG", "✅ Asset copied to: \${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("MAP_DEBUG", "❌ Failed to copy asset: \${e.message}")
        }
    }
    return file.absolutePath
}

/**
 * Creates a GeoJSON Feature string representing a Polygon (a circle approximation with 64 points)
 * around the given [center] with the specified [radiusInMeters].
 */
private fun createRadiusPolygon(center: LatLng, radiusInMeters: Double, points: Int = 64): String {
    val distanceX = radiusInMeters / (111320.0 * kotlin.math.cos(center.latitude * Math.PI / 180.0))
    val distanceY = radiusInMeters / 110574.0

    val coordinates = StringBuilder()
    coordinates.append("[")
    for (i in 0 until points) {
        val theta = (i.toDouble() / points) * (2 * Math.PI)
        val x = distanceX * kotlin.math.cos(theta)
        val y = distanceY * kotlin.math.sin(theta)
        val lng = center.longitude + x
        val lat = center.latitude + y
        coordinates.append("[$lng, $lat]")
        if (i < points - 1) {
            coordinates.append(", ")
        }
    }
    // Close the polygon
    val firstX = distanceX * kotlin.math.cos(0.0)
    val firstY = distanceY * kotlin.math.sin(0.0)
    coordinates.append(", [${center.longitude + firstX}, ${center.latitude + firstY}]")
    coordinates.append("]")

    return "{ \"type\": \"Feature\", \"geometry\": { \"type\": \"Polygon\", \"coordinates\": [$coordinates] } }"
}
