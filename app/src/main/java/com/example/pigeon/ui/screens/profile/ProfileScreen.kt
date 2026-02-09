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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
                
                IdentityDetails(user)
                
                Spacer(modifier = Modifier.weight(1f))
                
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
