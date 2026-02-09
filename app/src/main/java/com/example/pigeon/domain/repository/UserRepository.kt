package com.example.pigeon.domain.repository

import com.example.pigeon.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user data.
 */
interface UserRepository {
    fun getUser(): Flow<User?>
    suspend fun saveUser(user: User)
    suspend fun isProfileLocked(): Boolean
    suspend fun debugResetTimer() // Added for development only
}
