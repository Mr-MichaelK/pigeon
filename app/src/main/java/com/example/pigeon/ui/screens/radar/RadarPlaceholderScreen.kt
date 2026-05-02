package com.example.pigeon.ui.screens.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pigeon.ui.theme.MeshColor

@Composable
fun RadarPlaceholderScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshColor.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MeshColor.Surface)
                    .border(2.dp, MeshColor.Primary.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(1.dp, MeshColor.Primary.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, MeshColor.Primary.copy(alpha = 0.3f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = MeshColor.Primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Scanning for Mesh Peers...",
                style = MaterialTheme.typography.titleMedium,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Activate nearby discovery to connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = MeshColor.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
