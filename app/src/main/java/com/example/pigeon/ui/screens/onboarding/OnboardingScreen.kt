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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.ui.theme.MeshColor

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
        topBar = { MeshTopBar(title = "JOIN THE MESH") },
        containerColor = MeshColor.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp)
        ) {
            // Profile Header
            MeshProfileHeader(
                displayName = uiState.displayName.ifBlank { "Alpha-One" },
                role = uiState.role
            )

            // Warning Banner
            MeshWarningBanner()

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Edit Identity",
                style = MaterialTheme.typography.titleLarge,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Callsign Input
            MeshTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = "Display Name",
                placeholder = "e.g. Alpha-One"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selector
            MeshRoleSelector(
                selectedRole = uiState.role,
                onRoleSelected = viewModel::onRoleChange
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Mesh Anonymous Mode Toggle
            MeshAnonymousToggle(
                isAnonymous = uiState.isAnonymous,
                onToggle = viewModel::onAnonymousToggle
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Device Node Name (Visual only for now, as repo generates it)
            MeshTextField(
                label = "Device Node Name",
                value = "NODE-ALPHA-X", // Placeholder/Mock
                onValueChange = {},
                placeholder = "e.g. NODE-MESH-01",
                readOnly = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            MeshSaveButton(
                text = "JOIN MESH",
                onClick = viewModel::joinMesh,
                enabled = uiState.displayName.isNotBlank()
            )
        }
    }
}

@Composable
fun MeshTopBar(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeshColor.Background)
            .height(16.dp) 
    )
}

@Composable
fun MeshProfileHeader(displayName: String, role: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MeshColor.Surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MeshColor.Border)
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
                    .border(2.dp, MeshColor.Primary, CircleShape),
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
                    color = MeshColor.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role: $role",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeshColor.TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Verified,
                        contentDescription = "Verified",
                        tint = MeshColor.SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Verified Node",
                        style = MaterialTheme.typography.labelMedium,
                        color = MeshColor.SuccessGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun MeshWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(
                color = MeshColor.Primary.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, MeshColor.Primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.LockClock,
            contentDescription = null,
            tint = MeshColor.Primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "PROFILE LOCK",
                style = MaterialTheme.typography.labelLarge,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Identity changes are limited to once every 72 hours to maintain network trust during emergency operations.",
                style = MaterialTheme.typography.bodySmall,
                color = MeshColor.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun MeshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
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
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MeshColor.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MeshColor.TextPrimary
                ),
                cursorBrush = SolidColor(MeshColor.Primary),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MeshColor.TextSecondary
                        )
                    }
                    innerTextField()
                },
                readOnly = readOnly
            )
        }
    }
}

@Composable
fun MeshRoleSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Operational Role",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MeshColor.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
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
                    text = selectedRole,
                    color = MeshColor.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MeshColor.TextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MeshColor.Surface)
            ) {
                TacticalRoles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.title, color = MeshColor.TextPrimary) },
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
fun MeshAnonymousToggle(
    isAnonymous: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MeshColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
            .clickable { onToggle(!isAnonymous) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Anonymous Mode",
                style = MaterialTheme.typography.bodyLarge,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hide your display name in the mesh",
                style = MaterialTheme.typography.bodySmall,
                color = MeshColor.TextSecondary
            )
        }
        Switch(
            checked = isAnonymous,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MeshColor.Primary,
                checkedTrackColor = MeshColor.Primary.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MeshColor.Border
            )
        )
    }
}

@Composable
fun MeshSaveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MeshColor.Surface, RoundedCornerShape(16.dp))
            .border(2.dp, MeshColor.Primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MeshColor.Primary.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle, // Using generic icon as placeholder for 'emergency_home'
                contentDescription = null,
                tint = MeshColor.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Are you sure?",
            style = MaterialTheme.typography.titleLarge,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This identity will be locked for the next 72 hours across the emergency mesh network.",
            style = MaterialTheme.typography.bodySmall,
            color = MeshColor.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .background(MeshColor.Background, RoundedCornerShape(50))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(50))
                .padding(vertical = 8.dp, horizontal = 16.dp)
        ) {
            Text(
                text = "71:59:58",
                style = MaterialTheme.typography.titleMedium,
                color = MeshColor.Primary,
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
                containerColor = MeshColor.Primary,
                disabledContainerColor = MeshColor.Primary.copy(alpha = 0.5f)
            ),
            enabled = enabled && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
