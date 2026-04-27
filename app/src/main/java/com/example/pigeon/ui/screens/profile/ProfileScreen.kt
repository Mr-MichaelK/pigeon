package com.example.pigeon.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.Gender
import com.example.pigeon.domain.model.User
import com.example.pigeon.ui.components.*
import com.example.pigeon.ui.theme.MeshColor

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
    ) {
        // Top bar — sits on the sand background
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IDENTITY PROFILE",
                style = MaterialTheme.typography.titleMedium,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(64.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MeshColor.Primary)
            }
        } else {
            uiState.user?.let { user ->
                ProfileHeader(user)

                Spacer(modifier = Modifier.height(16.dp))

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                    CountdownCard(
                        countdownText = uiState.countdownText,
                        isLocked = uiState.isLocked
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState.isLocked) {
                        SectionLabel("CURRENT ROLE")
                        Spacer(modifier = Modifier.height(8.dp))
                        IdentityDetails(user)

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionLabel("MESH STATISTICS")
                        Spacer(modifier = Modifier.height(8.dp))
                        MeshStatisticsSection(user)

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onBack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MeshColor.TextPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RETURN TO MAP",
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        EditProfileView(
                            uiState = uiState,
                            onRoleChange = viewModel::onRoleChange,
                            onDisplayNameChange = viewModel::onDisplayNameChange,
                            onAnonymousToggle = viewModel::onAnonymousToggle,
                            onGenderChange = viewModel::onGenderChange,
                            onSave = viewModel::onSaveClick
                        )
                    }

                    Spacer(modifier = Modifier.height(36.dp))
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
private fun SectionLabel(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = MeshColor.Border,
            thickness = 1.dp
        )
    }
}

@Composable
fun ProfileHeader(user: User) {
    // Full-bleed warm card that bleeds from the top bar into content
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MeshColor.Surface)
            .drawBehind {
                drawLine(
                    color = MeshColor.Border,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                // Subtle bottom border
                drawLine(
                    color = MeshColor.Border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(top = 28.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar — IdentityAvatar already has a gold border ring
        Box(contentAlignment = Alignment.BottomEnd) {
            IdentityAvatar(gender = user.gender, size = 96.dp)
            if (user.isVerified) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(MeshColor.Surface, CircleShape)
                        .border(1.dp, MeshColor.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified",
                        tint = MeshColor.SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = user.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Black
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Role + anonymous chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Chip(
                label = user.role.uppercase(),
                background = MeshColor.Background,
                border = MeshColor.Border,
                textColor = MeshColor.TextSecondary
            )
            if (user.isAnonymous) {
                Chip(
                    label = "ANONYMOUS",
                    background = MeshColor.TextSecondary.copy(alpha = 0.08f),
                    border = MeshColor.TextSecondary.copy(alpha = 0.20f),
                    textColor = MeshColor.TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Node ID in monospace — feels like a hardware serial
        Text(
            text = user.nodeName,
            style = MaterialTheme.typography.labelSmall,
            color = MeshColor.TextSecondary.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun Chip(
    label: String,
    background: Color,
    border: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(4.dp))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
fun CountdownCard(countdownText: String, isLocked: Boolean) {
    val accentColor = if (isLocked) MeshColor.TextSecondary else MeshColor.SuccessGreen
    val cardBg = MeshColor.Surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Left accent stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(accentColor)
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLocked) "IDENTITY LOCKED" else "IDENTITY UNLOCKED",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = countdownText,
                style = MaterialTheme.typography.displaySmall,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )

            if (isLocked) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "REMAINING UNTIL RE-BROADCAST PERMITTED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MeshColor.TextSecondary,
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun IdentityDetails(user: User) {
    val currentRole = TacticalRoles.find { it.id == user.role } ?: TacticalRoles.first()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MeshRoleCard(
            role = currentRole,
            isSelected = true,
            onClick = null,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MeshColor.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, MeshColor.Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANONYMOUS MODE",
                style = MaterialTheme.typography.labelLarge,
                color = MeshColor.TextSecondary,
                fontWeight = FontWeight.Bold
            )
            val isOn = user.isAnonymous
            Box(
                modifier = Modifier
                    .background(MeshColor.Border.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isOn) "ENABLED" else "DISABLED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOn) MeshColor.TextPrimary else MeshColor.TextSecondary,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun MeshStatisticsSection(user: User) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatTile(
            modifier = Modifier.weight(1f),
            label = "SYNCS",
            value = "${user.totalSyncs}",
            valueColor = MeshColor.TextPrimary
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = "TRUST",
            value = "${user.trustScore.toInt()}%",
            valueColor = if (user.trustScore >= 80) MeshColor.SuccessGreen else MeshColor.TextPrimary
        )
        StatTile(
            modifier = Modifier.weight(1f),
            label = "GENDER",
            value = when (user.gender) {
                Gender.MALE -> "M"
                Gender.FEMALE -> "F"
                Gender.UNDISCLOSED -> "N/A"
            },
            valueColor = MeshColor.TextPrimary
        )
    }
}

@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        modifier = modifier
            .background(MeshColor.Surface, RoundedCornerShape(10.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(10.dp))
            .padding(vertical = 16.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
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

        Spacer(modifier = Modifier.height(4.dp))

        MeshProfileSaveGroup(
            isSaving = uiState.isSaving,
            onSave = onSave
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
            .background(MeshColor.Surface, RoundedCornerShape(14.dp))
            .border(1.dp, MeshColor.Border, RoundedCornerShape(14.dp))
            .drawBehind {
                // Green top accent
                drawLine(
                    color = MeshColor.SuccessGreen.copy(alpha = 0.7f),
                    start = Offset(14.dp.toPx(), 0f),
                    end = Offset(size.width - 14.dp.toPx(), 0f),
                    strokeWidth = 3.dp.toPx()
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(MeshColor.SuccessGreen.copy(alpha = 0.08f), CircleShape)
                .border(1.dp, MeshColor.SuccessGreen.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = MeshColor.SuccessGreen,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CONFIRM IDENTITY",
            style = MaterialTheme.typography.titleMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Your identity will be locked for 72 hours after saving.",
            style = MaterialTheme.typography.bodySmall,
            color = MeshColor.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MeshColor.SuccessGreen,
                disabledContainerColor = MeshColor.SuccessGreen.copy(alpha = 0.4f)
            ),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SAVE & LOCK IDENTITY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun TacticalRoleBadge(role: String) {
    Surface(
        color = MeshColor.Background,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MeshColor.Border)
    ) {
        Text(
            text = role.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextSecondary,
            fontWeight = FontWeight.Black
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
                colors = ButtonDefaults.buttonColors(containerColor = MeshColor.SuccessGreen),
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
