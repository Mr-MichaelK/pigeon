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
import com.example.pigeon.domain.model.Gender
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
                displayName = uiState.displayName.ifBlank { "John Doe" },
                role = uiState.role,
                gender = uiState.gender,
                isVerified = uiState.isVerified
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

            MeshTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onDisplayNameChange,
                label = "Display Name",
                placeholder = "e.g. John Doe"
            )

            if (uiState.isAnonymous) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFB300).copy(alpha = 0.1f) // Amber/Yellow Rugged
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Your name will be hidden; you will appear as 'Anonymous Civilian' to the mesh",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFB300),
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Role Selector
            MeshRoleSelector(
                selectedRoleId = uiState.role,
                onRoleSelected = viewModel::onRoleChange
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gender Selector
            MeshGenderSelector(
                selectedGender = uiState.gender,
                onGenderSelected = viewModel::onGenderChange
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
                value = uiState.nodeName,
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
fun MeshProfileHeader(displayName: String, role: String, gender: Gender, isVerified: Boolean) {
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
            // Avatar Switching Logic (Task 7.1/7.4)
            val avatarColor = when(gender) {
                Gender.MALE -> Color(0xFFE0C09E)
                Gender.FEMALE -> Color(0xFFF3E5F5)
                Gender.UNDISCLOSED -> Color(0xFFE5E2DC)
            }
            
            val avatarIcon = if (gender == Gender.FEMALE) Icons.Default.Person else Icons.Default.Person // Placeholder until actual assets

            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(avatarColor)
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
                    text = role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MeshColor.TextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Gender: ${gender.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeshColor.Primary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                // Task 7.5: Conditional visibility for Verified Node badge
                if (isVerified) {
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
    selectedRoleId: String,
    onRoleSelected: (String) -> Unit
) {
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
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TacticalRoles.forEach { role ->
                val isSelected = selectedRoleId == role.id
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(100.dp)
                        .clickable { onRoleSelected(role.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MeshColor.Primary.copy(alpha = 0.1f) else MeshColor.Surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MeshColor.Primary else MeshColor.Border
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = role.title.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) MeshColor.Primary else MeshColor.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = role.description,
                            style = MaterialTheme.typography.labelSmall,
                            color = MeshColor.TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeshGenderSelector(
    selectedGender: Gender,
    onGenderSelected: (Gender) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Gender Selection",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MeshColor.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Gender.values().forEachIndexed { index, gender ->
                val isSelected = selectedGender == gender
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isSelected) MeshColor.Primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onGenderSelected(gender) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender.name,
                        color = if (isSelected) MeshColor.Primary else MeshColor.TextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
                
                if (index < Gender.values().size - 1) {
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MeshColor.Border))
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
