package com.example.pigeon.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.ui.theme.MeshColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportingWizardSheet(
    onDismiss: () -> Unit,
    onReport: (EventType, String, String, Long) -> Unit,
    currentLatitude: Double,
    currentLongitude: Double
) {
    var stage by remember { mutableIntStateOf(1) }
    var selectedType by remember { mutableStateOf<EventType?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTtlHours by remember { mutableLongStateOf(24) }

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
                .padding(bottom = 32.dp)
        ) {
            if (stage == 1) {
                StageOne(
                    onTypeSelected = { selectedType = it },
                    selectedType = selectedType,
                    latitude = currentLatitude,
                    longitude = currentLongitude,
                    onNext = { stage = 2 },
                    onCancel = onDismiss
                )
            } else {
                StageTwo(
                    selectedType = selectedType!!,
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    selectedTtl = selectedTtlHours,
                    onTtlChange = { selectedTtlHours = it },
                    onConfirm = {
                        onReport(selectedType!!, title, description, selectedTtlHours)
                        onDismiss()
                    },
                    onBack = { stage = 1 }
                )
            }
        }
    }
}

@Composable
private fun StageOne(
    onTypeSelected: (EventType) -> Unit,
    selectedType: EventType?,
    latitude: Double,
    longitude: Double,
    onNext: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        "Report Incident",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = MeshColor.TextPrimary
    )
    Text(
        "QUICK SELECT • OFFLINE READY",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MeshColor.TextSecondary,
        letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(24.dp))

    val types = listOf(
        Triple(EventType.FIRE, R.drawable.local_fire_department_24dp, "FIRE"),
        Triple(EventType.MEDICAL, R.drawable.medical_services_24dp, "MEDICAL"),
        Triple(EventType.SUPPLIES, R.drawable.package_2_24dp, "SUPPLIES"),
        Triple(EventType.CONFLICT, R.drawable.warning_24dp, "CONFLICT"),
        Triple(EventType.CUSTOM, R.drawable.location_on_24dp, "OTHER")
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.heightIn(max = 300.dp)
    ) {
        items(types) { (type, iconRes, label) ->
            TypeCard(
                label = label,
                iconRes = iconRes,
                isSelected = selectedType == type,
                onClick = { onTypeSelected(type) }
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Location Pill
    Surface(
        color = MeshColor.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MeshColor.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MeshColor.TextPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.my_location),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MeshColor.Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "AUTO-FILLED",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MeshColor.Primary,
                            fontSize = 8.sp
                        )
                    }
                }
                Text(
                    String.format("%.4f° N, %.4f° W", latitude, longitude),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MeshColor.TextPrimary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MeshColor.Border)
        ) {
            Text("CANCEL", fontWeight = FontWeight.Bold, color = MeshColor.TextSecondary)
        }
        Button(
            onClick = onNext,
            enabled = selectedType != null,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MeshColor.TextPrimary)
        ) {
            Text("NEXT", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageTwo(
    selectedType: EventType,
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    selectedTtl: Long,
    onTtlChange: (Long) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Text(
        "Incident Details",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Black,
        color = MeshColor.TextPrimary
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    // Type Display (Non-editable)
    Surface(
        color = MeshColor.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MeshColor.Border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
             val (iconRes, label) = when (selectedType) {
                EventType.FIRE -> R.drawable.local_fire_department_24dp to "FIRE"
                EventType.MEDICAL -> R.drawable.medical_services_24dp to "MEDICAL"
                EventType.SUPPLIES -> R.drawable.package_2_24dp to "SUPPLIES"
                EventType.CONFLICT -> R.drawable.warning_24dp to "CONFLICT"
                EventType.CUSTOM -> R.drawable.location_on_24dp to "OTHER"
            }
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MeshColor.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold, color = MeshColor.TextPrimary)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        label = { Text("Event Title") },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MeshColor.Primary,
            unfocusedBorderColor = MeshColor.Border
        )
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Event Description") },
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MeshColor.Primary,
            unfocusedBorderColor = MeshColor.Border
        )
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        "TIME TO LIVE (TTL)",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MeshColor.TextSecondary
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(1L, 6L, 24L).forEach { hours ->
            FilterChip(
                selected = selectedTtl == hours,
                onClick = { onTtlChange(hours) },
                label = { Text("${hours}h") },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("BACK", fontWeight = FontWeight.Bold, color = MeshColor.TextSecondary)
        }
        Button(
            onClick = onConfirm,
            enabled = title.isNotBlank(),
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MeshColor.SuccessGreen)
        ) {
            Text("CONFIRM REPORT", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun TypeCard(
    label: String,
    iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        color = if (isSelected) MeshColor.Primary.copy(alpha = 0.1f) else MeshColor.Surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MeshColor.Primary else MeshColor.Border
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MeshColor.Primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isSelected) MeshColor.Primary else MeshColor.TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) MeshColor.Primary else MeshColor.TextPrimary
                )
            }
        }
    }
}
