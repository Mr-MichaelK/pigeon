package com.example.pigeon.ui.screens.map

import android.annotation.SuppressLint
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
                    // Register dynamic tactical pins
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
                    defaultPin?.let { style.addImage("default-pin", it) }

                    val manager = SymbolManager(this@apply, map, style).apply {
                        iconAllowOverlap = true
                        textAllowOverlap = true
                    }
                    symbolManagerState.value = manager
                    
                    enableLocationComponent(map, style, context)
                    updateSymbols(context, manager, style, uiState.events, currentZoom >= zoomThreshold)

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
            mapLibreMap?.getStyle { style ->
                updateSymbols(context, manager, style, uiState.events, showTitles)
            }
        }
    }

    // Auto-zoom to user location on launch
    LaunchedEffect(mapLibreMap) {
        if (mapLibreMap != null && !hasInitialZoomed) {
            // Wait for location to become available
            while (!hasInitialZoomed) {
                val lastLocation = mapLibreMap?.locationComponent?.lastKnownLocation
                if (lastLocation != null) {
                    mapLibreMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(lastLocation.latitude, lastLocation.longitude),
                            14.0
                        )
                    )
                    hasInitialZoomed = true
                }
                kotlinx.coroutines.delay(1000)
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
        // Sticky Header: Mesh Status
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

            // Event Detail Sheet
            selectedEvent?.let { event ->
                EventDetailSheet(
                    event = event,
                    onDismiss = { selectedEvent = null },
                    onResolve = viewModel::onResolveEvent
                )
            }

            if (showReportingWizard) {
                ReportingWizardSheet(
                    onDismiss = { showReportingWizard = false },
                    onReport = { type, title, desc, ttlHours ->
                        viewModel.reportEvent(type, title, desc, ttlHours * 60 * 60 * 1000)
                    },
                    currentLatitude = uiState.metadata.latitude,
                    currentLongitude = uiState.metadata.longitude
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

// Legacy EventDetailSheet removed in favor of standalone component

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

private fun updateSymbols(context: android.content.Context, manager: SymbolManager?, style: Style, events: List<Event>, showTitles: Boolean) {
    manager?.deleteAll()
    events.forEach { event ->
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
            )
        } else {
            // ICON ONLY VIEW
                val iconImage = when (event.eventType) {
                    EventType.FIRE -> "pin-fire"
                    EventType.MEDICAL -> "pin-medical"
                    EventType.SUPPLIES -> "pin-supplies"
                    EventType.CONFLICT -> "pin-conflict"
                    EventType.CUSTOM, EventType.SOS -> "pin-custom"
                }

            manager?.create(
                SymbolOptions()
                    .withLatLng(LatLng(event.latitude, event.longitude))
                    .withIconImage(iconImage)
                    .withIconSize(1.0f)
            )
        }
    }
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

private fun createLabelPillBitmap(text: String): Bitmap? {
    val paint = android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = "#171511".toColorInt() // Tactical Black
        textSize = 32f // High-res source size
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    val horizontalPadding = 32f
    val verticalPadding = 20f
    val cornerRadius = 24f

    val textBounds = android.graphics.Rect()
    paint.getTextBounds(text, 0, text.length, textBounds)

    val width = textBounds.width() + (horizontalPadding * 2)
    val height = textBounds.height() + (verticalPadding * 2)

    val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.FILL
    }

    val rect = android.graphics.RectF(0f, 0f, width, height)
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    // Add subtle tactical border
    bgPaint.style = android.graphics.Paint.Style.STROKE
    bgPaint.strokeWidth = 2f
    bgPaint.color = "#E5E0D6".toColorInt()
    canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint)

    val textY = (height / 2f) - ((paint.descent() + paint.ascent()) / 2f)
    canvas.drawText(text, width / 2f, textY, paint)

    return bitmap
}

private fun drawVectorPaths(
    group: androidx.compose.ui.graphics.vector.VectorGroup,
    canvas: Canvas,
    paint: android.graphics.Paint
) {
    canvas.save()
    canvas.translate(group.translationX, group.translationY)
    canvas.rotate(group.rotation, group.pivotX, group.pivotY)
    canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)

    group.forEach { node ->
        when (node) {
            is androidx.compose.ui.graphics.vector.VectorGroup -> {
                drawVectorPaths(node, canvas, paint)
            }
            is androidx.compose.ui.graphics.vector.VectorPath -> {
                val path = android.graphics.Path()
                var lastX = 0f
                var lastY = 0f
                node.pathData.forEach { p ->
                    when (p) {
                        is androidx.compose.ui.graphics.vector.PathNode.MoveTo -> {
                            path.moveTo(p.x, p.y)
                            lastX = p.x
                            lastY = p.y
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeMoveTo -> {
                            path.rMoveTo(p.dx, p.dy)
                            lastX += p.dx
                            lastY += p.dy
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.LineTo -> {
                            path.lineTo(p.x, p.y)
                            lastX = p.x
                            lastY = p.y
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeLineTo -> {
                            path.rLineTo(p.dx, p.dy)
                            lastX += p.dx
                            lastY += p.dy
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.HorizontalTo -> {
                            path.lineTo(p.x, lastY)
                            lastX = p.x
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeHorizontalTo -> {
                            path.rLineTo(p.dx, 0f)
                            lastX += p.dx
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.VerticalTo -> {
                            path.lineTo(lastX, p.y)
                            lastY = p.y
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeVerticalTo -> {
                            path.rLineTo(0f, p.dy)
                            lastY += p.dy
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.CurveTo -> {
                            path.cubicTo(p.x1, p.y1, p.x2, p.y2, p.x3, p.y3)
                            lastX = p.x3
                            lastY = p.y3
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeCurveTo -> {
                            path.rCubicTo(p.dx1, p.dy1, p.dx2, p.dy2, p.dx3, p.dy3)
                            lastX += p.dx3
                            lastY += p.dy3
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.QuadTo -> {
                            path.quadTo(p.x1, p.y1, p.x2, p.y2)
                            lastX = p.x2
                            lastY = p.y2
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.RelativeQuadTo -> {
                            path.rQuadTo(p.dx1, p.dy1, p.dx2, p.dy2)
                            lastX += p.dx2
                            lastY += p.dy2
                        }
                        is androidx.compose.ui.graphics.vector.PathNode.Close -> path.close()
                        else -> {}
                    }
                }
                canvas.drawPath(path, paint)
            }
        }
    }
    canvas.restore()
}

@Composable
fun MapCrosshair(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
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

private fun Color.toArgb(): Int = (value shr 32).toInt()
