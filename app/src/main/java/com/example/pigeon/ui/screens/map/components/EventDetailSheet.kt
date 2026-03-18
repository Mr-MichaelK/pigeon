package com.example.pigeon.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pigeon.R
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.ui.theme.MeshColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: Event,
    onDismiss: () -> Unit,
    onResolve: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MeshColor.Background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MeshColor.Border) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header: Title and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MeshColor.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    StatusBadge(isResolved = event.isResolved)
                }
                
                // Type Icon Surface
                val (iconRes, tint) = when (event.eventType) {
                    EventType.FIRE -> R.drawable.local_fire_department_24dp to MeshColor.EmergencyRed
                    EventType.MEDICAL -> R.drawable.medical_services_24dp to MeshColor.EmergencyRed
                    EventType.SUPPLIES -> R.drawable.package_2_24dp to MeshColor.MeshBlue
                    EventType.CONFLICT -> R.drawable.warning_24dp to MeshColor.AlertOrange
                    EventType.CUSTOM -> R.drawable.location_on_24dp to MeshColor.AssistYellow
                }
                Surface(
                    color = tint.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.3f))
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Section
            Text(
                "DESCRIPTION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeshColor.TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MeshColor.Surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MeshColor.Border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = event.description,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MeshColor.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metadata Grid
            Text(
                "SITUATIONAL DATA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MeshColor.TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Grid of Data Pills
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Location Row
                MetadataPill(
                    icon = R.drawable.my_location,
                    label = "COORDINATES",
                    value = String.format("%.4f° N, %.4f° E", event.latitude, event.longitude)
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetadataPill(
                        icon = Icons.Outlined.Dns,
                        label = "ORIGIN",
                        value = event.creatorDeviceId.take(8).uppercase(),
                        modifier = Modifier.weight(1f)
                    )
                    MetadataPill(
                        icon = Icons.Outlined.Schedule,
                        label = "CREATED",
                        value = SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(Date(event.timestamp)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action: Resolve Button
            if (!event.isResolved) {
                Button(
                    onClick = { 
                        onResolve(event.eventId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MeshColor.SuccessGreen)
                ) {
                    Text("RESOLVE INCIDENT", fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MeshColor.Border)
                ) {
                    Text("CLOSE DETAILS", fontWeight = FontWeight.Bold, color = MeshColor.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isResolved: Boolean) {
    val (text, color) = if (isResolved) "RESOLVED" to Color.Gray else "LIVE" to MeshColor.SuccessGreen
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun MetadataPill(
    icon: Any, // Int resource or ImageVector
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MeshColor.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MeshColor.Border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (icon) {
                is Int -> Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = MeshColor.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                is ImageVector -> Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MeshColor.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MeshColor.TextSecondary,
                    fontSize = 9.sp
                )
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeshColor.TextPrimary
                )
            }
        }
    }
}
