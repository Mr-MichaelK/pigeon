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

@Composable
fun MapPlaceholderScreen(onOpenProfile: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MAP VIEW [OFFLINE]",
                style = MaterialTheme.typography.headlineMedium,
                color = TacticalGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "GRID SYSTEM READY",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(
                onClick = onOpenProfile,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text("IDENTITY", color = Color.White, style = MaterialTheme.typography.labelSmall)
        }
    }
}
