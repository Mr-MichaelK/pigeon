package com.example.pigeon.ui.screens.profile

import androidx.compose.foundation.BorderStroke
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
import com.example.pigeon.domain.model.Gender
import com.example.pigeon.domain.model.User
import com.example.pigeon.ui.components.*
import com.example.pigeon.ui.theme.MeshColor
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign

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
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    MeshStatisticsSection(user)
                    
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = viewModel::debugResetTimer
                        ) {
                            Text(
                                text = "DEBUG: UNLOCK",
                                color = MeshColor.Primary.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))

                        TextButton(
                            onClick = viewModel::debugLockProfile
                        ) {
                            Text(
                                text = "DEBUG: LOCK",
                                color = MeshColor.EmergencyRed.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    EditProfileView(
                        uiState = uiState,
                        onRoleChange = viewModel::onRoleChange,
                        onDisplayNameChange = viewModel::onDisplayNameChange,
                        onAnonymousToggle = viewModel::onAnonymousToggle,
                        onGenderChange = viewModel::onGenderChange,
                        onVerifiedToggle = viewModel::onVerifiedToggle,
                        onSave = viewModel::onSaveClick)
                }
            }
        }
    }

    if (uiState.showSaveConfirmation) {
        SaveIdentityConfirmationDialog(
            onConfirm = viewModel::saveAndLockIdentity,
            onDismiss = viewModel::dismissSaveConfirmation
        )
    }
}

@Composable
fun ProfileHeader(user: User) {
    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        IdentityAvatar(gender = user.gender, size = 120.dp)
        
        // Verified Badge (Task 7.5: Conditional visibility)
        if (user.isVerified) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = MeshColor.Surface,
                tonalElevation = 2.dp
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verified Member",
                    tint = MeshColor.SuccessGreen,
                    modifier = Modifier.padding(2.dp)
                )
            }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    // Display Name & Node ID (Task 8.1)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = user.displayName,
            style = MaterialTheme.typography.headlineSmall,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = user.nodeName,
            style = MaterialTheme.typography.labelLarge,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun CountdownCard(countdownText: String, isLocked: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isLocked) Modifier.border(2.dp, MeshColor.Primary.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                else Modifier
            ),
        color = if (isLocked) MeshColor.Surface else MeshColor.SuccessGreen.copy(alpha = 0.05f),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, if (isLocked) MeshColor.Primary else MeshColor.SuccessGreen)
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
    val currentRole = TacticalRoles.find { it.id == user.role } ?: TacticalRoles.first()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "CURRENT ROLE",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        MeshRoleCard(
            role = currentRole,
            isSelected = true,
            onClick = null,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeshColor.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            DetailRow(label = "ANONYMOUS MODE", value = if (user.isAnonymous) "ENABLED" else "DISABLED", isLast = true)
        }
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
    onDisplayNameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onAnonymousToggle: (Boolean) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onVerifiedToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MeshTextField(
            value = uiState.editedDisplayName,
            onValueChange = onDisplayNameChange,
            label = "DISPLAY NAME",
            placeholder = "Enter your mesh identity..."
        )

        MeshRoleSelector(
            selectedRoleId = uiState.editedRole,
            onRoleSelected = onRoleChange
        )

        MeshGenderSelector(
            selectedGender = uiState.editedGender,
            onGenderSelected = onGenderChange
        )
        
        MeshAnonymousToggle(
            isAnonymous = uiState.editedIsAnonymous,
            onToggle = onAnonymousToggle
        )

        MeshProfileVerifiedToggle(
            isVerified = uiState.editedIsVerified,
            onToggle = onVerifiedToggle
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MeshProfileSaveGroup(
            isSaving = uiState.isSaving,
            onSave = onSave
        )
    }
}

@Composable
fun MeshProfileVerifiedToggle(
    isVerified: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MeshColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(8.dp))
            .clickable { onToggle(!isVerified) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = if (isVerified) MeshColor.SuccessGreen else MeshColor.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "VERIFIED MESH MEMBER [DEBUG]",
                color = if (isVerified) MeshColor.SuccessGreen else MeshColor.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Switch(
            checked = isVerified,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MeshColor.SuccessGreen,
                checkedTrackColor = MeshColor.SuccessGreen.copy(alpha = 0.5f),
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
            textAlign = TextAlign.Center,
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

@Composable
fun MeshStatisticsSection(user: com.example.pigeon.domain.model.User) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeshColor.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(12.dp))
            .padding(24.dp)
    ) {
        Text(
            text = "MESH STATISTICS",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        MeshStatRow(
            label = "OPERATIONAL STATUS",
            value = { TacticalRoleBadge(user.role) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        MeshStatRow(
            label = "NETWORK UPTIME / SYNCS",
            value = { 
                Text(
                    text = "${user.totalSyncs} SUCCESSFUL EXCHANGES",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MeshColor.TextPrimary
                )
            }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        MeshStatRow(
            label = "TRUST RATING",
            value = {
                Text(
                    text = "${user.trustScore.toInt()}% OPERATIONAL INTEGRITY",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (user.trustScore >= 80) MeshColor.SuccessGreen else MeshColor.Primary
                )
            }
        )
    }
}

@Composable
fun MeshStatRow(label: String, value: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MeshColor.TextSecondary,
            fontSize = 10.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        value()
    }
}

@Composable
fun TacticalRoleBadge(role: String) {
    Surface(
        color = MeshColor.Primary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = role.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun SaveIdentityConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "CONFIRM IDENTITY BROADCAST",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MeshColor.TextPrimary
            )
        },
        text = {
            Text(
                text = "Your identity will be broadcast to the mesh and LOCKED for 72 hours. You will not be able to change your role or name during this period. Proceed?",
                style = MaterialTheme.typography.bodyMedium,
                color = MeshColor.TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MeshColor.Primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BROADCAST & LOCK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MeshColor.TextSecondary)
            }
        },
        containerColor = MeshColor.Surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 8.dp
    )
}
