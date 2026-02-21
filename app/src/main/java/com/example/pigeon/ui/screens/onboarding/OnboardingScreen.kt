package com.example.pigeon.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.ui.theme.StichColor

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onJoinComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.isProfileCreated) {
        if (uiState.isProfileCreated) {
            onJoinComplete()
        }
    }

    Scaffold(
        topBar = { StichTopBar() },
        containerColor = StichColor.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // Profile Header
            StichProfileHeader(
                displayName = uiState.displayName.ifBlank { "Alpha-One" },
                role = uiState.role
            )

            // Warning Banner
            StichWarningBanner()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Edit Identity",
                style = MaterialTheme.typography.titleLarge,
                color = StichColor.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Callsign Input
            StichTextField(
                label = "Callsign",
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                placeholder = "Enter new callsign"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selector
            StichDropdown(
                label = "Operational Role",
                currentRole = uiState.role,
                onRoleSelected = viewModel::onRoleChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Anonymous Mode Toggle (To maintain functionality)
             StichAnonymousToggle(
                isAnonymous = uiState.isAnonymous,
                onToggle = viewModel::onAnonymousToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Device Node Name (Visual only for now, as repo generates it)
            StichTextField(
                label = "Device Node Name",
                value = "NODE-ALPHA-X", // Placeholder/Mock
                onValueChange = {},
                placeholder = "e.g. NODE-MESH-01",
                readOnly = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            StichSaveButton(
                onClick = viewModel::joinMesh,
                isLoading = uiState.isSaving,
                isEnabled = uiState.displayName.isNotBlank()
            )
        }
    }
}

@Composable
fun StichTopBar() {
    // Spacer/Box to maintain a smaller height as requested
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(StichColor.Background)
            .height(16.dp) 
    )
}

@Composable
fun StichProfileHeader(displayName: String, role: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = StichColor.Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StichColor.Border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0C09E)) // Close to the image placeholder color
                    .border(2.dp, StichColor.Primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = StichColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role: $role",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StichColor.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Verified",
                        tint = StichColor.SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified Node",
                        style = MaterialTheme.typography.labelMedium,
                        color = StichColor.SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun StichWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = StichColor.Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, StichColor.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.LockClock,
            contentDescription = null,
            tint = StichColor.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "PROFILE LOCK",
                style = MaterialTheme.typography.labelLarge,
                color = StichColor.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Identity changes are limited to once every 72 hours to maintain network trust during emergency operations.",
                style = MaterialTheme.typography.bodySmall,
                color = StichColor.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun StichTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    readOnly: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = StichColor.TextSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Height from Tailwind h-14 (approx 56dp)
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StichColor.Primary,
                unfocusedBorderColor = StichColor.Border,
                focusedTextColor = StichColor.TextPrimary,
                unfocusedTextColor = StichColor.TextPrimary,
                cursorColor = StichColor.Primary,
                focusedContainerColor = StichColor.Surface,
                unfocusedContainerColor = StichColor.Surface
            ),
            readOnly = readOnly,
            singleLine = true
        )
    }
}

@Composable
fun StichDropdown(
    label: String,
    currentRole: String,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(StichColor.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, StichColor.Border, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentRole,
                    color = StichColor.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = StichColor.TextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(StichColor.Surface)
            ) {
                TacticalRoles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.title, color = StichColor.TextPrimary) },
                        onClick = {
                            onRoleSelected(role.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun StichAnonymousToggle(
    isAnonymous: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(StichColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, StichColor.Border, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Anonymous Mode",
            color = StichColor.TextPrimary,
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = isAnonymous,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = StichColor.Primary,
                checkedTrackColor = StichColor.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = StichColor.TextSecondary,
                uncheckedTrackColor = StichColor.Border
            )
        )
    }
}

@Composable
fun StichSaveButton(
    onClick: () -> Unit,
    isLoading: Boolean,
    isEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(StichColor.Surface, RoundedCornerShape(16.dp))
            .border(2.dp, StichColor.Primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(StichColor.Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle, // Using generic icon as placeholder for 'emergency_home'
                contentDescription = null,
                tint = StichColor.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Are you sure?",
            style = MaterialTheme.typography.titleLarge,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This identity will be locked for the next 72 hours across the emergency mesh network.",
            style = MaterialTheme.typography.bodySmall,
            color = StichColor.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .background(StichColor.Background, RoundedCornerShape(50))
                .border(1.dp, StichColor.Border, RoundedCornerShape(50))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "71:59:58",
                style = MaterialTheme.typography.titleMedium,
                color = StichColor.Primary,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StichColor.Primary,
                disabledContainerColor = StichColor.Primary.copy(alpha = 0.5f)
            ),
            enabled = isEnabled && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text(
                    text = "SAVE & LOCK IDENTITY",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
