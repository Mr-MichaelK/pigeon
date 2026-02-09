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
    TacticalRole("Scout", "Scout", "Mobile node. Focused on active field reporting."),
    TacticalRole("Medic", "First Responder", "Medical support. Notified of injury reports."),
    TacticalRole("Coordinator", "Coordinator", "Strategic node. Managing regional data.")
)
