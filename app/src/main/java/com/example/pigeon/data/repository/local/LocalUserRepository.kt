package com.example.pigeon.data.repository.local

import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.toDomain
import com.example.pigeon.data.local.entities.toEntity
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Implementation of UserRepository using Room local storage.
 */
class LocalUserRepository @Inject constructor(
    private val userDao: UserDao
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

        val userWithMetadata = user.copy(
            nodeName = finalNodeName,
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

    private fun generateNodeName(): String {
        // Generate a rugged-style node name hash
        return "NODE-${UUID.randomUUID().toString().take(8).uppercase()}"
    }
}
