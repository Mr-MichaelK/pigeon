package com.example.pigeon.ui.screens.onboarding

import androidx.compose.runtime.Immutable

@Immutable
data class TacticalRole(
    val id: String,
    val title: String,
    val description: String,
    val iconRes: Int? = null // Placeholder for future icons
)

val TacticalRoles = listOf(
    TacticalRole("Civilian", "Civilian", "Basic node. Best for receiving alerts."),
    TacticalRole("Responder", "First Responder", "Medical and support. Notified of emergency reports.")
)
