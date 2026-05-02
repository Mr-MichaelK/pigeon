package com.example.pigeon.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pigeon.ui.theme.DarkBackground
import com.example.pigeon.ui.theme.TacticalGreen
import com.example.pigeon.ui.theme.MeshColor

@Composable
fun MapPlaceholderScreen(
    onOpenProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshColor.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Map View",
                style = MaterialTheme.typography.headlineMedium,
                color = MeshColor.TextPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "MapLibre Integration Pending",
                style = MaterialTheme.typography.bodyLarge,
                color = MeshColor.TextSecondary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onOpenProfile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MeshColor.Primary
                )
            ) {
                Text("Go to Profile", color = Color.White)
            }
        }
    }
}
