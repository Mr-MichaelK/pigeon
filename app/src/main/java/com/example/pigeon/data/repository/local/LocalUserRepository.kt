package com.example.pigeon.data.repository.local

import com.example.pigeon.data.identity.IdentityKeyManager
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.toDomain
import com.example.pigeon.data.local.entities.toEntity
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Implementation of UserRepository using Room local storage.
 */
class LocalUserRepository @Inject constructor(
    private val userDao: UserDao,
    private val identityKeyManager: IdentityKeyManager
) : UserRepository {

    override fun getUser(): Flow<User?> {
        return userDao.getUser().map { it?.toDomain() }
    }

    override suspend fun saveUser(user: User) {
        val existingUser = userDao.getUserSync()

        // nodeName is the human-friendly display tag — immutable once generated
        // so a re-saved profile doesn't visually flip identities mid-session.
        val finalNodeName = if (existingUser?.nodeName.isNullOrEmpty()) {
            generateNodeName()
        } else {
            existingUser!!.nodeName
        }

        // nodeId is now the cryptographic signerId — SHA-256 of the device's
        // Ed25519 public key. Always (re)write it from IdentityKeyManager so a
        // legacy UUID-based nodeId from a pre-trust install is migrated to the
        // canonical key fingerprint on the next saveUser call. The keypair is
        // pinned in EncryptedSharedPreferences, so this value is stable for
        // the lifetime of the install (uninstall regenerates).
        val finalNodeId = identityKeyManager.signerId

        val userWithMetadata = user.copy(
            nodeName = finalNodeName,
            nodeId = finalNodeId,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        userDao.upsertUser(userWithMetadata.toEntity())
    }

    override suspend fun isProfileLocked(): Boolean {
        val lastUpdated = userDao.getUserSync()?.lastUpdatedTimestamp ?: return false
        val currentTime = System.currentTimeMillis()
        val difference = currentTime - lastUpdated
        val seventyTwoHoursInMillis = TimeUnit.HOURS.toMillis(72)
        
        return difference < seventyTwoHoursInMillis
    }

    override suspend fun incrementSyncCount() {
        userDao.incrementSyncCount()
    }

    override suspend fun updateTrustScore(score: Float) {
        userDao.updateTrustScore(score)
    }

    override suspend fun getOrGenerateNodeName(): String {
        val existing = userDao.getUserSync()
        return if (existing?.nodeName.isNullOrEmpty()) {
            generateNodeName()
        } else {
            existing!!.nodeName
        }
    }

    private fun generateNodeName(): String {
        // Derive the human-readable callsign from the cryptographic signerId.
        // First 8 hex chars give a 32-bit collision space, plenty for visual
        // disambiguation in a peer list. Replaces the prior ANDROID_ID-based
        // derivation, which was both a privacy leak and forgeable.
        val tag = identityKeyManager.signerId.take(8).uppercase()
        return "NODE-$tag"
    }
}
