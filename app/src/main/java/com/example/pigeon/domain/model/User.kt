package com.example.pigeon.domain.model

/**
 * Domain model representing a user in the Pigeon mesh network.
 */
data class User(
    val id: Int = 1,
    val displayName: String,
    val gender: String, // "Male" or "Female"
    val role: String,
    val nodeName: String,
    val isAnonymous: Boolean,
    val lastUpdatedTimestamp: Long
)
