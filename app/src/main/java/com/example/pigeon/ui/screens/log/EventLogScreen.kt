package com.example.pigeon.ui.screens.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.usecase.EventFilter
import com.example.pigeon.ui.theme.MeshColor
import com.example.pigeon.ui.util.LocationUtils
import org.maplibre.android.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.*

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EventLogScreen(
    viewModel: EventLogViewModel,
    onLongPressEvent: (eventId: String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshColor.Background)
    ) {
        // Top Bar Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeshColor.Background)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventLogSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            EventLogFilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = viewModel::onFilterSelected
            )
        }

        HorizontalDivider(color = MeshColor.Border)

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MeshColor.Primary)
            }
        } else if (uiState.events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO EVENTS FOUND",
                    style = MaterialTheme.typography.titleMedium,
                    color = MeshColor.TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(uiState.events) { event ->
                    EventLogItem(
                        event = event,
                        userLocation = uiState.userLocation,
                        onResolve = { viewModel.onResolveEvent(event.eventId) },
                        onLongPress = { onLongPressEvent(event.eventId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EventLogSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MeshColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MeshColor.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MeshColor.TextPrimary
                ),
                cursorBrush = SolidColor(MeshColor.Primary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search logs...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MeshColor.TextSecondary
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventLogFilterRow(
    selectedFilter: EventFilter,
    onFilterSelected: (EventFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EventFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { 
                    Text(
                        text = filter.name, 
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MeshColor.Primary,
                    selectedLabelColor = Color.White,
                    containerColor = MeshColor.Surface,
                    labelColor = MeshColor.TextSecondary
                ),
                // FIXED: Passing mandatory enabled/selected parameters
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MeshColor.Border,
                    selectedBorderColor = MeshColor.Primary,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EventLogItem(
    event: Event,
    userLocation: LatLng?,
    onResolve: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "LongPressScale"
    )
    val distance = remember(event, userLocation) {
        if (userLocation != null) {
            LocationUtils.calculateDistance(
                userLocation.latitude, userLocation.longitude,
                event.latitude, event.longitude
            )
        } else null
    }
    
    val isWithinRadius = distance != null && distance <= 500.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) 
    ) {
        Column(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = if (event.isResolved) MeshColor.SuccessGreen else MeshColor.Primary,
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f) // Use weight to fill vertical space properly
                    .background(MeshColor.Border)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier
            .padding(bottom = 16.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isPressed = true
                            onLongPress()
                        }
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeshColor.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MeshColor.Border)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EventTypeBadge(type = event.eventType)
                        Text(
                            text = formatTimestamp(event.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshColor.TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MeshColor.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MeshColor.TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = event.creatorName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshColor.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .background(MeshColor.Background, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (!event.isResolved) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                distance?.let { d ->
                                    Text(
                                        text = if (d >= 1000) String.format("%.1fkm", d/1000) else String.format("%.0fm", d),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isWithinRadius) MeshColor.Primary.copy(alpha = 0.7f) else MeshColor.EmergencyRed.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                                
                                TextButton(
                                    onClick = onResolve,
                                    enabled = isWithinRadius,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        disabledContentColor = MeshColor.TextSecondary.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = if (isWithinRadius) "MARK RESOLVED" else "TOO FAR",
                                        color = if (isWithinRadius) MeshColor.Primary else MeshColor.TextPrimary.copy(alpha = 0.38f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MeshColor.SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "RESOLVED",
                                    color = MeshColor.SuccessGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventTypeBadge(type: EventType) {
    val (color, label) = when (type) {
        EventType.FIRE -> MeshColor.EmergencyRed to "FIRE"
        EventType.MEDICAL -> MeshColor.EmergencyRed to "MEDICAL"
        EventType.SUPPLIES -> MeshColor.MeshBlue to "SUPPLIES"
        EventType.CONFLICT -> MeshColor.AlertOrange to "CONFLICT"
        EventType.SOS -> MeshColor.EmergencyRed to "SOS"
        EventType.CUSTOM -> MeshColor.AssistYellow to "OTHER"
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}