package com.example.pigeon.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.domain.model.User
import com.example.pigeon.ui.theme.*
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle


@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StichColor.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "IDENTITY PROFILE",
            style = MaterialTheme.typography.headlineMedium,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (uiState.isLoading) {
            CircularProgressIndicator(color = StichColor.Primary)
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
                            containerColor = StichColor.Primary,
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
                            color = StichColor.Primary.copy(alpha = 0.5f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else {
                    EditProfileView(
                        uiState = uiState,
                        onRoleChange = viewModel::onRoleChange,
                        onGenderChange = viewModel::onGenderChange,
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
        // Avatar based on gender
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = if (user.gender == "Male") Color(0xFFE0C09E) else Color(0xFFD1B08E),
            border = androidx.compose.foundation.BorderStroke(2.dp, StichColor.Primary)
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
            color = StichColor.Surface,
            tonalElevation = 2.dp
        ) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified",
                tint = StichColor.SuccessGreen,
                modifier = Modifier.padding(2.dp)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    
    Text(
        text = user.displayName,
        style = MaterialTheme.typography.headlineSmall,
        color = StichColor.TextPrimary,
        fontWeight = FontWeight.Bold
    )
    
    Text(
        text = user.nodeName,
        style = MaterialTheme.typography.bodySmall,
        color = StichColor.Primary,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    )
}

@Composable
fun CountdownCard(countdownText: String, isLocked: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StichColor.Surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, StichColor.Primary.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Verified,
                    contentDescription = null,
                    tint = if (isLocked) StichColor.Primary else StichColor.SuccessGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLocked) "IDENTITY LOCKED" else "IDENTITY STABLE",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isLocked) StichColor.Primary else StichColor.SuccessGreen,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = countdownText,
                style = MaterialTheme.typography.headlineLarge,
                color = StichColor.TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "REMAINING UNTIL RE-BROADCAST PERMITTED",
                style = MaterialTheme.typography.bodySmall,
                color = StichColor.TextSecondary,
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
            .background(StichColor.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, StichColor.Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        DetailRow(label = "TACTICAL ROLE", value = user.role.uppercase(), isLast = false)
        DetailRow(label = "GENDER", value = user.gender.uppercase(), isLast = false)
        DetailRow(label = "ANONYMOUS MODE", value = if (user.isAnonymous) "ENABLED" else "DISABLED", isLast = true)
    }
}

@Composable
fun DetailRow(label: String, value: String, isLast: Boolean) {
    val modifier = if (!isLast) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(bottom = 1.dp, color = StichColor.Border)
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
            color = StichColor.TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EditProfileView(
    uiState: ProfileUiState,
    onRoleChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAnonymousToggle: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        StichProfileDropdown(
            label = "OPERATIONAL ROLE",
            currentRole = uiState.editedRole,
            onRoleSelected = onRoleChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        StichProfileGenderSelector(
            selectedGender = uiState.editedGender,
            onGenderSelected = onGenderChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        StichProfileAnonymousToggle(
            isAnonymous = uiState.editedIsAnonymous,
            onToggle = onAnonymousToggle
        )

        Spacer(modifier = Modifier.height(32.dp))

        StichProfileSaveGroup(
            onClick = onSave
        )
    }
}

// --- Stich Components (Duplicated/Adapted for Profile) ---

@Composable
fun StichProfileDropdown(
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
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = StichColor.TextSecondary
                )
            }
            
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(StichColor.Surface)
            ) {
                roles.forEach { role ->
                    DropdownMenuItem(
                        text = { Text(role, color = StichColor.TextPrimary) },
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
fun StichProfileGenderSelector(
    selectedGender: String,
    onGenderSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "GENDER",
            style = MaterialTheme.typography.labelMedium,
            color = StichColor.TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Male", "Female").forEach { gender ->
                val isSelected = selectedGender == gender
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            if (isSelected) StichColor.Primary.copy(alpha = 0.2f) else StichColor.Surface,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) StichColor.Primary else StichColor.Border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onGenderSelected(gender) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = gender.uppercase(),
                        color = if (isSelected) StichColor.Primary else StichColor.TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (gender == "Male") Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun StichProfileAnonymousToggle(
    isAnonymous: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StichColor.Surface, RoundedCornerShape(8.dp))
            .border(1.dp, StichColor.Border, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "ANONYMOUS MODE",
            color = StichColor.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
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
fun StichProfileSaveGroup(
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
                imageVector = Icons.Default.CheckCircle,
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
        
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StichColor.Primary
            )
        ) {
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

private fun Modifier.border(bottom: androidx.compose.ui.unit.Dp, color: Color) = this.drawBehind {
    val strokeWidth = bottom.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(
        color = color,
        start = androidx.compose.ui.geometry.Offset(0f, y),
        end = androidx.compose.ui.geometry.Offset(size.width, y),
        strokeWidth = strokeWidth
    )
}
