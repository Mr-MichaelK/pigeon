package com.example.pigeon.ui.components

import androidx.compose.foundation.Canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pigeon.domain.model.Gender
import com.example.pigeon.ui.theme.MeshColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor

@Immutable
data class TacticalRole(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int? = null 
)

val TacticalRoles = listOf(
    TacticalRole("Civilian", "Civilian", "Basic node. Best for receiving alerts."),
    TacticalRole("Responder", "First Responder", "Medical and support. Notified of emergency reports.")
)

@Composable
fun IdentityAvatar(
    gender: Gender,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier
) {
    val avatarColor = when(gender) {
        Gender.MALE -> Color(0xFFE0C09E)
        Gender.FEMALE -> Color(0xFFF3E5F5)
        Gender.UNDISCLOSED -> Color(0xFFE5E2DC)
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor)
            .border(2.dp, MeshColor.Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size / 2)
        )
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
            text = "OPERATIONAL ROLE",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TacticalRoles.forEach { role ->
                MeshRoleCard(
                    role = role,
                    isSelected = selectedRoleId == role.id,
                    onClick = { onRoleSelected(role.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MeshRoleCard(
    role: TacticalRole,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MeshColor.Primary.copy(alpha = 0.1f) else MeshColor.Surface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) MeshColor.TextPrimary else MeshColor.Border
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
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = role.description,
                style = MaterialTheme.typography.labelSmall,
                color = MeshColor.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp
            )
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
            text = "GENDER SELECTION",
            style = MaterialTheme.typography.labelMedium,
            color = MeshColor.TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
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
                text = "ANONYMOUS MODE",
                style = MaterialTheme.typography.bodyLarge,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isAnonymous) "PRIVACY ACTIVE: Identity hidden from mesh" else "Hide your display name in the network",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAnonymous) MeshColor.AssistYellow else MeshColor.TextSecondary,
                fontWeight = if (isAnonymous) FontWeight.Bold else FontWeight.Normal
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
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MeshColor.TextPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
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

