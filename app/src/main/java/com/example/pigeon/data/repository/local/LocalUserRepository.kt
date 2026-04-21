package com.example.pigeon.data.repository.local

import android.content.Context
import android.provider.Settings
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.toDomain
import com.example.pigeon.data.local.entities.toEntity
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Implementation of UserRepository using Room local storage.
 */
class LocalUserRepository @Inject constructor(
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) : UserRepository {

    override fun getUser(): Flow<User?> {
        return userDao.getUser().map { it?.toDomain() }
    }

    override suspend fun saveUser(user: User) {
        val existingUser = userDao.getUserSync()
        
        // Ensure nodeName is immutable once generated
        val finalNodeName = if (existingUser?.nodeName.isNullOrEmpty()) {
            generateNodeName()
        } else {
            existingUser!!.nodeName
        }

        // Ensure nodeId is immutable once generated (Task 7.4)
        val finalNodeId = if (existingUser?.nodeId.isNullOrEmpty()) {
            UUID.randomUUID().toString()
        } else {
            existingUser!!.nodeId
        }

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
        // Generate a deterministic node name based on ANDROID_ID (Task 7.4)
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val hash = androidId?.take(8)?.uppercase() ?: UUID.randomUUID().toString().take(8).uppercase()
        return "NODE-$hash"
    }
}
