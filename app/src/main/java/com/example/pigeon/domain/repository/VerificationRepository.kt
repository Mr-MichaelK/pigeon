package com.example.pigeon.domain.repository

import com.example.pigeon.data.local.entities.VerificationEntity
import com.example.pigeon.domain.model.TrustScore
import kotlinx.coroutines.flow.Flow

interface VerificationRepository {
    fun getTrustScore(eventId: String): Flow<TrustScore>
    suspend fun insertVerification(verification: VerificationEntity)
    suspend fun verifyEvent(eventId: String, isConfirm: Boolean)
}
