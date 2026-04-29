package com.example.pigeon.data.repository

import com.example.pigeon.data.identity.IdentityKeyManager
import com.example.pigeon.data.local.dao.VerificationDao
import com.example.pigeon.data.local.entities.VerificationEntity
import com.example.pigeon.domain.model.TrustScore
import com.example.pigeon.domain.repository.VerificationRepository
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.proto.VerificationMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class VerificationRepositoryImpl @Inject constructor(
    private val verificationDao: VerificationDao,
    private val nearbySyncManager: Provider<NearbySyncManager>,
    private val identityKeyManager: IdentityKeyManager
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
        // Sign the verification message under this device's Ed25519 identity.
        // The manager stamps signer_id (= SHA-256 hex of public key) and the
        // public key bytes onto the proto, so the persisted entity and the
        // broadcast payload are byte-identical — no second derivation step
        // needed for re-broadcasts via manifest sync.
        val unsignedBuilder = VerificationMessage.newBuilder()
            .setId(UUID.randomUUID().toString())
            .setEventId(eventId)
            .setIsConfirm(isConfirm)
            .setTimestamp(System.currentTimeMillis())
        val signed = identityKeyManager.signVerification(unsignedBuilder)

        val entity = VerificationEntity(
            id = signed.id,
            eventId = signed.eventId,
            signerId = signed.signerId,
            isConfirm = signed.isConfirm,
            timestamp = signed.timestamp,
            signerPublicKey = signed.signerPublicKey.toByteArray(),
            signature = signed.signature.toByteArray()
        )
        verificationDao.insertVerification(entity)

        nearbySyncManager.get().broadcastVerification(signed)
    }
}
