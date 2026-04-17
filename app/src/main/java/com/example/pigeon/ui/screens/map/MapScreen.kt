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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer

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
    detailViewModel: EventDetailViewModel = hiltViewModel(),
    onHeaderClick: () -> Unit = {}
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
    val isWizardLoading by viewModel.isWizardLoading.collectAsStateWithLifecycle()
    var lastReportingClickTime by remember { mutableLongStateOf(0L) }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val canReport by reportViewModel.canReport.collectAsStateWithLifecycle()
    val cooldownTimeRemaining by reportViewModel.cooldownTimeRemaining.collectAsStateWithLifecycle()
    val meshStatus by viewModel.meshStatus.collectAsStateWithLifecycle()
    val peerCount by viewModel.peerCount.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        reportViewModel.reportError.collect { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

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
                map.uiSettings.isCompassEnabled = false
                
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
        // Sticky Header: Functional Mesh Status (Full Width)
        MeshHeader(
            latitude = uiState.metadata.latitude,
            longitude = uiState.metadata.longitude,
            meshStatus = meshStatus,
            peerCount = peerCount,
            onClick = onHeaderClick
        )

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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Tool Stack (Right)
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
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
                        },
                        onResetCooldown = {
                            reportViewModel.resetCooldown()
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 32.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { 
                        val now = System.currentTimeMillis()
                        if (canReport && !showReportingWizard && !isWizardLoading && (now - lastReportingClickTime > 500L)) {
                            lastReportingClickTime = now
                            viewModel.setWizardLoading(true)
                            showReportingWizard = true
                        }
                    },
                    enabled = canReport && !showReportingWizard && !isWizardLoading,
                    modifier = Modifier
                        .height(64.dp)
                        .weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canReport) MeshColor.EmergencyRed else Color.Gray,
                        disabledContainerColor = if (canReport) MeshColor.EmergencyRed.copy(alpha = 0.5f) else Color.Gray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(
                        imageVector = if (canReport) Icons.Outlined.Emergency else Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (canReport) "REPORT" else "COOLDOWN ${cooldownTimeRemaining?.let { "($it)" } ?: ""}",
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
                    onDismiss = { 
                        showReportingWizard = false 
                        viewModel.setWizardLoading(false)
                    },
                    onReport = { type, title, desc, ttlHours ->
                        reportViewModel.reportEvent(
                            eventType = type,
                            title = title,
                            description = desc,
                            ttlMillis = ttlHours * 60 * 60 * 1000L,
                            latitude = uiState.metadata.latitude,
                            longitude = uiState.metadata.longitude,
                            onLimitReached = { 
                                showReportingWizard = false
                                viewModel.setWizardLoading(false)
                            }
                        )
                        showReportingWizard = false
                        viewModel.setWizardLoading(false)
                    },
                    currentLatitude = uiState.metadata.latitude,
                    currentLongitude = uiState.metadata.longitude)
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
            )
        }
    }

}

@Composable
fun MeshHeader(
    latitude: Double,
    longitude: Double,
    meshStatus: com.example.pigeon.domain.network.ConnectionStatus,
    peerCount: Int,
    onClick: () -> Unit
) {
    val isTracking = meshStatus != com.example.pigeon.domain.network.ConnectionStatus.OFF
    val statusColor = when (meshStatus) {
        com.example.pigeon.domain.network.ConnectionStatus.ACTIVE -> Color(0xFF4ADE80) // Green
        com.example.pigeon.domain.network.ConnectionStatus.PASSIVE -> MeshColor.Primary // Amber/Gold
        com.example.pigeon.domain.network.ConnectionStatus.OFF -> Color.Gray
    }
    
    val statusText = when (meshStatus) {
        com.example.pigeon.domain.network.ConnectionStatus.ACTIVE -> "SYNCING"
        com.example.pigeon.domain.network.ConnectionStatus.PASSIVE -> "SEARCHING"
        com.example.pigeon.domain.network.ConnectionStatus.OFF -> "OFFLINE"
    }

    val iconVector = when (meshStatus) {
        com.example.pigeon.domain.network.ConnectionStatus.ACTIVE -> Icons.Outlined.Sync
        com.example.pigeon.domain.network.ConnectionStatus.PASSIVE -> Icons.Outlined.WifiTethering
        com.example.pigeon.domain.network.ConnectionStatus.OFF -> Icons.Outlined.WifiTetheringOff
    }

    Surface(
        onClick = onClick,
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
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Coordinates Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val latStr = String.format("%.5f", latitude)
                    val lonStr = String.format("%.5f", longitude)

                    Column {
                        Text(
                            "TACTICAL COORDINATES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MeshColor.TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$latStr, $lonStr",
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            fontWeight = FontWeight.Bold,
                            color = MeshColor.TextPrimary
                        )
                    }
                }
                
                // Right: Network Status Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 1.sp
                        )
                        if (isTracking && peerCount > 0) {
                            Text(
                                "$peerCount Peers Nearby",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MeshColor.TextPrimary
                            )
                        } else if (isTracking) {
                            Text(
                                "0 Peers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MeshColor.TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Dynamic Icon
                    Box(contentAlignment = Alignment.Center) {
                        val infiniteTransition = rememberInfiniteTransition(label = "MeshAnim")
                        
                        // Icon rotation for SYNCING
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "IconRotation"
                        )
                        
                        // Alpha pulse for SEARCHING
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
                            color = statusColor.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = "Mesh Status",
                                tint = statusColor.copy(alpha = if (meshStatus == com.example.pigeon.domain.network.ConnectionStatus.PASSIVE) pulseAlpha else 1f),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .run {
                                        if (meshStatus == com.example.pigeon.domain.network.ConnectionStatus.ACTIVE) {
                                            this.rotate(rotation)
                                        } else this
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolStack(
    onMyLocationClick: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetCooldown: () -> Unit
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
                HorizontalDivider(modifier = Modifier.width(20.dp), color = MeshColor.TextPrimary.copy(alpha = 0.1f))
                IconButton(onClick = onZoomOut, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Remove, contentDescription = "Zoom Out", tint = MeshColor.TextPrimary)
                }
            }
        }

        // Debug Reset Button
        Surface(
            onClick = onResetCooldown,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MeshColor.Surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.BugReport,
                    contentDescription = "Reset Cooldown",
                    tint = MeshColor.Primary,
                    modifier = Modifier.size(24.dp)
                )
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

    // 2. Proximity Visuals Layer (Added BEFORE symbols so it sits underneath)
    if (style.getSource("proximity-source") == null) {
        style.addSource(GeoJsonSource("proximity-source"))
        
        val proximityFillLayer = FillLayer("proximity-fill-layer", "proximity-source")
            .withProperties(
                fillColor("rgba(0, 255, 0, 0.10)") // #00FF00 with 10% alpha
            )
            
        val proximityLineLayer = LineLayer("proximity-line-layer", "proximity-source")
            .withProperties(
                lineColor("rgba(0, 255, 0, 0.50)"), // #00FF00 with 50% alpha
                lineWidth(2f)
            )
            
        style.addLayer(proximityFillLayer)
        style.addLayer(proximityLineLayer)
    }

    // 3. Clear old manager if exists
    symbolManagerState.value?.let { oldManager ->
        try { oldManager.onDestroy() } catch (e: Exception) {}
    }
    
    // 4. Create new manager for the new style
    val manager = SymbolManager(mapView, map, style).apply {
        iconAllowOverlap = true
        textAllowOverlap = true
        iconIgnorePlacement = true
        textIgnorePlacement = true
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

    // 5. Initial content
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
