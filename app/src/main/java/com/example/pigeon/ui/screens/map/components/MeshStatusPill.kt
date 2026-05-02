package com.example.pigeon.ui.screens.map.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pigeon.ui.theme.MeshColor

@Composable
fun MeshStatusPill(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MeshColor.Surface.copy(alpha = 0.9f))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(24.dp))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulse Icon Circle
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
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MeshColor.Primary.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.WifiTethering,
                    contentDescription = "Mesh Active",
                    tint = MeshColor.MeshBlue,
                    modifier = Modifier.padding(6.dp)
                )
            }
            
            // Status dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color(0xFF4ADE80).copy(alpha = pulseAlpha))
                    .border(1.dp, MeshColor.Surface, CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.padding(end = 12.dp)) {
            Text(
                "MESH ACTIVE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeshColor.TextPrimary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                "SYNCED",
                style = MaterialTheme.typography.bodySmall,
                color = MeshColor.TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}
