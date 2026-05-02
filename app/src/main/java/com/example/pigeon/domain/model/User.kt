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
    val totalSyncs: Int = 0,
    // Distributed Trust: peers start at zero and earn trust through valid
    // signed verifications from other peers. The prior 100.0 default
    // pre-granted unearned reputation, which is incompatible with a
    // cryptographic identity model where keys are cheap to mint.
    val trustScore: Float = 0.0f,
    val lastUpdatedTimestamp: Long
)
