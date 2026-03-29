package com.example.pigeon.data.repository

import android.content.Context
import android.provider.Settings
import com.example.pigeon.data.local.dao.VerificationDao
import com.example.pigeon.data.local.entities.VerificationEntity
import com.example.pigeon.domain.model.TrustScore
import com.example.pigeon.domain.repository.VerificationRepository
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.proto.VerificationMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class VerificationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val verificationDao: VerificationDao,
    private val nearbySyncManager: Provider<NearbySyncManager>
) : VerificationRepository {

    override fun getTrustScore(eventId: String): Flow<TrustScore> {
        return verificationDao.getVerificationsForEvent(eventId).map { list ->
            val totalConfirms = list.count { it.isConfirm }
            val totalContradicts = list.size - totalConfirms
            val total = totalConfirms + totalContradicts
            val percentage = if (total == 0) 0 else (totalConfirms * 100) / total
            val isVerified = percentage >= 80 && totalConfirms >= 3

            TrustScore(
                percentage = percentage,
                totalConfirms = totalConfirms,
                totalContradicts = totalContradicts,
                isVerified = isVerified
            )
        }
    }

    override suspend fun insertVerification(verification: VerificationEntity) {
        verificationDao.insertVerification(verification)
    }

    override suspend fun verifyEvent(eventId: String, isConfirm: Boolean) {
        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        val verification = VerificationEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            signerId = deviceId,
            isConfirm = isConfirm,
            timestamp = System.currentTimeMillis()
        )
        verificationDao.insertVerification(verification)

        // Broadcast to mesh
        val protoMsg = VerificationMessage.newBuilder()
            .setId(verification.id)
            .setEventId(verification.eventId)
            .setSignerId(verification.signerId)
            .setIsConfirm(verification.isConfirm)
            .setTimestamp(verification.timestamp)
            .build()
        
        nearbySyncManager.get().broadcastVerification(protoMsg)
    }
}
