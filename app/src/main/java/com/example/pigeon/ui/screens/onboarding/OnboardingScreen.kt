package com.example.pigeon.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pigeon.ui.theme.DarkBackground
import com.example.pigeon.ui.theme.DarkSurface
import com.example.pigeon.ui.theme.EmergencyRed
import com.example.pigeon.ui.theme.TacticalBlack
import com.example.pigeon.ui.theme.TacticalGreen

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onJoinComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isProfileCreated) {
        if (uiState.isProfileCreated) {
            onJoinComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = "JOINING THE MESH",
            style = MaterialTheme.typography.headlineLarge,
            color = EmergencyRed,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "ESTABLISHING SECURE IDENTITY",
            style = MaterialTheme.typography.labelLarge,
            color = TacticalGreen,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Display Name Input
        OutlinedTextField(
            value = uiState.displayName,
            onValueChange = viewModel::onDisplayNameChange,
            label = { Text("DISPLAY NAME (CALLSIGN)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalGreen,
                unfocusedBorderColor = TacticalGreen.copy(alpha = 0.5f),
                focusedLabelColor = TacticalGreen,
                unfocusedLabelColor = TacticalGreen.copy(alpha = 0.5f),
                cursorColor = TacticalGreen,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Gender Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GENDER:",
                color = TacticalGreen,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.width(80.dp)
            )
            
            GenderButton(
                text = "MALE",
                isSelected = uiState.gender == "Male",
                onClick = { viewModel.onGenderChange("Male") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GenderButton(
                text = "FEMALE",
                isSelected = uiState.gender == "Female",
                onClick = { viewModel.onGenderChange("Female") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Anonymous Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANONYMOUS MODE",
                color = TacticalGreen,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = uiState.isAnonymous,
                onCheckedChange = viewModel::onAnonymousToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TacticalGreen,
                    checkedTrackColor = TacticalGreen.copy(alpha = 0.3f),
                    uncheckedThumbColor = DarkSurface,
                    uncheckedTrackColor = DarkSurface.copy(alpha = 0.5f)
                )
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "TACTICAL ROLE SELECTION",
            color = TacticalGreen,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Role Selection Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(TacticalRoles) { role ->
                RoleCard(
                    role = role,
                    isSelected = uiState.role == role.id,
                    onClick = { viewModel.onRoleChange(role.id) }
                )
            }
        }
        
        Button(
            onClick = viewModel::joinMesh,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmergencyRed,
                contentColor = TacticalBlack,
                disabledContainerColor = DarkSurface,
                disabledContentColor = Color.Gray
            ),
            enabled = uiState.displayName.isNotBlank() && !uiState.isSaving
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    color = TacticalBlack,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "JOIN THE MESH",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GenderButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp, 
            if (isSelected) TacticalGreen else TacticalGreen.copy(alpha = 0.3f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) TacticalGreen.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (isSelected) TacticalGreen else Color.Gray
        )
    ) {
        Text(text = text)
    }
}

@Composable
fun RoleCard(
    role: TacticalRole,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) TacticalGreen.copy(alpha = 0.1f) else DarkSurface,
        border = BorderStroke(
            1.dp,
            if (isSelected) TacticalGreen else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = role.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isSelected) TacticalGreen else Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = role.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                lineHeight = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
