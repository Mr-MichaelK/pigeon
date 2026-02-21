package com.example.pigeon.ui.screens.map.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
    val latStr = String.format("%.4f° N", latitude)
    val lonStr = String.format("%.4f° E", longitude)

    Text(
        text = "$latStr, $lonStr",
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MeshColor.Surface.copy(alpha = 0.9f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MeshColor.TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}
