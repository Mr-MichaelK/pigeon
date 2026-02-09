package com.example.pigeon.ui.screens.radar

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.ConnectionType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.Peer
import com.example.pigeon.ui.theme.StichColor
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarScreen(
    viewModel: RadarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Animation for the radar sweep
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepAngle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StichColor.Background)
    ) {
        // Top Control Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PowerStateToggle(
                currentState = uiState.powerState,
                onStateSelected = viewModel::setPowerState
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Radar Canvas
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0EFE9)) // Slightly darker than background
                    .border(1.dp, StichColor.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                RadarCanvas(
                    activePeers = uiState.activePeers,
                    powerState = uiState.powerState,
                    sweepAngle = sweepAngle
                )
            }
        }
        
        Divider(color = StichColor.Border)
        
        // Peer List
        PeerListSection(
            activePeers = uiState.activePeers,
            historicalPeers = uiState.historicalPeers
        )
    }
}

@Composable
fun PowerStateToggle(
    currentState: MeshPowerState,
    onStateSelected: (MeshPowerState) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(StichColor.Surface, RoundedCornerShape(24.dp))
            .border(1.dp, StichColor.Border, RoundedCornerShape(24.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        MeshPowerState.values().forEach { state ->
            val isSelected = currentState == state
            val backgroundColor = if (isSelected) {
                when (state) {
                    MeshPowerState.ACTIVE -> StichColor.Primary
                    MeshPowerState.PASSIVE -> StichColor.TextSecondary
                    MeshPowerState.OFF -> Color.Gray
                }
            } else Color.Transparent
            
            val contentColor = if (isSelected) Color.White else StichColor.TextSecondary
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(backgroundColor, RoundedCornerShape(20.dp))
                    .clickable { onStateSelected(state) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun RadarCanvas(
    activePeers: List<Peer>,
    powerState: MeshPowerState,
    sweepAngle: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2
        
        // Draw Rings
        for (i in 1..3) {
            drawCircle(
                color = StichColor.Border,
                radius = maxRadius * (i / 3f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        
        if (powerState != MeshPowerState.OFF) {
            // Draw Sweep
            val sweepColor = if (powerState == MeshPowerState.ACTIVE) StichColor.Primary else StichColor.TextSecondary
            
            // Not truly implementing a sweep gradient for simplicity, using a rotating line
            val sweepRad = Math.toRadians(sweepAngle.toDouble())
            val endX = center.x + maxRadius * cos(sweepRad).toFloat()
            val endY = center.y + maxRadius * sin(sweepRad).toFloat()
            
            drawLine(
                color = sweepColor.copy(alpha = 0.5f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2.dp.toPx()
            )
            
            // Draw Peers
            activePeers.forEach { peer ->
                // Map RSSI (-90 to -40) to distance (1.0 to 0.0)
                // Stronger signal (closer to -40) = closer to center
                val normalizedDist = ((peer.rssi + 40).coerceAtMost(0) / -50f).coerceIn(0.1f, 0.9f)
                
                // Random angle for visualization (in real app, would need bearing)
                // Using hash of ID to keep position stable
                val angle = (peer.deviceId.hashCode() % 360).toDouble()
                val rad = Math.toRadians(angle)
                
                val peerX = center.x + (maxRadius * normalizedDist) * cos(rad).toFloat()
                val peerY = center.y + (maxRadius * normalizedDist) * sin(rad).toFloat()
                
                drawCircle(
                    color = StichColor.Primary,
                    radius = 6.dp.toPx(),
                    center = Offset(peerX, peerY)
                )
            }
        }
    }
}

@Composable
fun PeerListSection(
    activePeers: List<Peer>,
    historicalPeers: List<Peer>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (activePeers.isNotEmpty()) {
            item {
                Text(
                    text = "LIVE PEERS (${activePeers.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = StichColor.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(activePeers) { peer ->
                PeerListItem(peer = peer, isActive = true)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        if (historicalPeers.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PEER HISTORY (LAST 24H)",
                    style = MaterialTheme.typography.labelSmall,
                    color = StichColor.TextSecondary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(historicalPeers) { peer ->
                PeerListItem(peer = peer, isActive = false)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun PeerListItem(peer: Peer, isActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = StichColor.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StichColor.Border)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isActive) StichColor.Primary.copy(alpha = 0.1f) else Color(0xFFF0EFE9),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (peer.connectionType == ConnectionType.BLE) Icons.Default.Bluetooth else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (isActive) StichColor.Primary else StichColor.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = peer.callsign,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) StichColor.TextPrimary else StichColor.TextSecondary
                )
                Text(
                    text = "Signal: ${peer.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = StichColor.TextSecondary
                )
            }
            
            if (isActive && peer.syncProgress < 1.0f) {
                CircularProgressIndicator(
                    progress = peer.syncProgress,
                    modifier = Modifier.size(24.dp),
                    color = StichColor.SuccessGreen,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
