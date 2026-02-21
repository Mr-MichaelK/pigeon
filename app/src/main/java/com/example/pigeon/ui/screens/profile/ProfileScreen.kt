package com.example.pigeon.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.User
import com.example.pigeon.ui.theme.MeshColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeshColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "IDENTITY PROFILE",
            style = MaterialTheme.typography.headlineMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState.isLoading) {
            CircularProgressIndicator(color = MeshColor.Primary)
        } else {
            uiState.user?.let { user ->
                ProfileHeader(user)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CountdownCard(
                    countdownText = uiState.countdownText,
                    isLocked = uiState.isLocked
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (uiState.isLocked) {
                    IdentityDetails(user)
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MeshColor.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "RETURN TO MAP",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    TextButton(
                        onClick = viewModel::debugResetTimer,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = "DEBUG: RESET 72H TIMER",
                            color = MeshColor.Primary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    EditProfileView(
                        uiState = uiState,
                        onRoleChange = viewModel::onRoleChange,
                        onAnonymousToggle = viewModel::onAnonymousToggle,
                        onSave = viewModel::saveAndLockIdentity
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User) {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = MeshColor.Background,
            border = androidx.compose.foundation.BorderStroke(1.dp, MeshColor.Border)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Avatar",
                tint = Color.White,
                modifier = Modifier.padding(24.dp)
            )
        }
        
        // Verified Badge
        Surface(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            color = MeshColor.Surface,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified",
                tint = MeshColor.SuccessGreen,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = user.displayName,
        style = MaterialTheme.typography.headlineSmall,
        color = MeshColor.TextPrimary,
        fontWeight = FontWeight.Bold
    )
    
    Text(
        text = user.nodeName,
        style = MaterialTheme.typography.bodySmall,
        color = MeshColor.Primary,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    )
}

@Composable
fun CountdownCard(countdownText: String, isLocked: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MeshColor.Surface,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MeshColor.Border)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Verified,
                    contentDescription = null,
                    tint = if (isLocked) MeshColor.Primary else MeshColor.SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLocked) "IDENTITY LOCKED" else "IDENTITY STABLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLocked) MeshColor.Primary else MeshColor.SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = countdownText,
                style = MaterialTheme.typography.headlineLarge,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "REMAINING UNTIL RE-BROADCAST PERMITTED",
                style = MaterialTheme.typography.bodySmall,
                color = MeshColor.TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun IdentityDetails(user: User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeshColor.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        DetailRow(label = "TACTICAL ROLE", value = user.role.uppercase(), isLast = false)
        DetailRow(label = "ANONYMOUS MODE", value = if (user.isAnonymous) "ENABLED" else "DISABLED", isLast = true)
    }
}

@Composable
fun DetailRow(label: String, value: String, isLast: Boolean) {
    val modifier = if (!isLast) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .then(Modifier.drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = MeshColor.Border,
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            })
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EditProfileView(
    uiState: ProfileUiState,
    onRoleChange: (String) -> Unit,
    onAnonymousToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        MeshProfileDropdown(
            label = "OPERATIONAL ROLE",
            currentRole = uiState.editedRole,
            onRoleSelected = onRoleChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        MeshProfileAnonymousToggle(
            isAnonymous = uiState.editedIsAnonymous,
            onToggle = onAnonymousToggle
        )

        Spacer(modifier = Modifier.height(32.dp))

        MeshProfileSaveGroup(
            isSaving = uiState.isSaving,
            onSave = onSave
        )
    }
}

// --- Mesh Components (Duplicated/Adapted for Profile) ---

@Composable
fun MeshProfileDropdown(
    label: String,
    currentRole: String,
    onRoleSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Hardcoded roles for now, same as Onboarding
    val roles = listOf("Civilian", "Medic", "Logistics", "Scout", "Operator")

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    text = currentRole.uppercase(),
                    color = MeshColor.TextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MeshColor.TextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MeshColor.Surface)
            ) {
                listOf("Civilian", "Medic", "Rescue", "Utility").forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role.uppercase(), color = MeshColor.TextPrimary) },
                        onClick = {
                            onRoleSelected(role)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


@Composable
fun MeshProfileAnonymousToggle(
    isAnonymous: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MeshColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
            .clickable { onToggle(!isAnonymous) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ANONYMOUS MODE",
            color = MeshColor.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
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
fun MeshProfileSaveGroup(
    isSaving: Boolean,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeshColor.Surface, RoundedCornerShape(16.dp))
            .bottomBorder(2.dp, MeshColor.Primary.copy(alpha = 0.2f))
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
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MeshColor.Primary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "CONFIRM IDENTITY",
            style = MaterialTheme.typography.titleLarge,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your identity will be locked for 72 hours after saving. Ensure all data is correct.",
            style = MaterialTheme.typography.bodySmall,
            color = MeshColor.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MeshColor.Primary,
                disabledContainerColor = MeshColor.Primary.copy(alpha = 0.5f)
            ),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
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

private fun Modifier.bottomBorder(bottom: androidx.compose.ui.unit.Dp, color: Color) = this.drawBehind {
    val strokeWidth = bottom.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, y),
        end = androidx.compose.ui.geometry.Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}
