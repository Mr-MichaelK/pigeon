package com.example.pigeon.domain.model

enum class Gender {
    MALE, FEMALE, UNDISCLOSED
}

/**
 * Domain model representing a user in the Pigeon mesh network.
 */
data class User(
    val id: Int = 1,
    val displayName: String,
    val role: String,
    val nodeName: String,
    val isAnonymous: Boolean,
    val gender: Gender = Gender.UNDISCLOSED,
    val nodeId: String = "",
    val isVerified: Boolean = false,
    val lastUpdatedTimestamp: Long
)
