package com.example.pigeon.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material3.Icon
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
fun LatLongPill(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MeshColor.Surface.copy(alpha = 0.9f))
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MeshColor.Background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Radar,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MeshColor.TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        val latStr = String.format("%.4f", latitude)
        val lonStr = String.format("%.4f", longitude)

        Text(
            text = "Lat: $latStr, Lon: $lonStr",
            modifier = Modifier.padding(end = 16.dp),
            color = MeshColor.TextPrimary,
            fontSize = 13.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
        )
    }
}
