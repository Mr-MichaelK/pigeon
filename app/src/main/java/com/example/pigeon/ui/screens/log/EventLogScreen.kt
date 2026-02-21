package com.example.pigeon.ui.screens.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.usecase.EventFilter
import com.example.pigeon.ui.theme.MeshColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventLogScreen(
    viewModel: EventLogViewModel
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
            EventLogSearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged
            )
            
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
                        onResolve = { viewModel.onResolveEvent(event.eventId) }
                    )
                }
            }
        }
    }
}

@Composable
fun EventLogSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
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

@Composable
fun EventLogItem(
    event: Event,
    onResolve: () -> Unit
) {
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

        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                            text = event.creatorDeviceId,
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshColor.TextSecondary,
                            modifier = Modifier
                                .background(MeshColor.Background, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                        
                        if (!event.isResolved) {
                            TextButton(
                                onClick = onResolve,
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = "MARK RESOLVED",
                                    color = MeshColor.Primary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
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
        EventType.WATER -> Color(0xFF29B6F6) to "WATER"
        EventType.CONFLICT -> Color(0xFFEF5350) to "CONFLICT"
        EventType.MEDICAL -> Color(0xFFEC407A) to "MEDICAL"
        EventType.SOS -> Color(0xFFAB47BC) to "SOS"
        EventType.FIRE_HAZARD -> Color(0xFFFFA726) to "FIRE"
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