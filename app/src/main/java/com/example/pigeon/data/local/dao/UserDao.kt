package com.example.pigeon.data.local.dao

import androidx.room.*
import com.example.pigeon.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for UserEntity.
 */
@Dao
interface UserDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserSync(): UserEntity?

    @Upsert
    suspend fun upsertUser(user: UserEntity)

    @Query("UPDATE user_profile SET totalSyncs = totalSyncs + 1 WHERE id = 1")
    suspend fun incrementSyncCount()

    @Query("UPDATE user_profile SET trustScore = :score WHERE id = 1")
    suspend fun updateTrustScore(score: Float)

    @Query("DELETE FROM user_profile")
    suspend fun clearUser()
}
