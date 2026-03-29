package com.example.pigeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pigeon.data.local.entities.VerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: VerificationEntity)

    @Query("SELECT * FROM verifications WHERE eventId = :eventId ORDER BY timestamp ASC")
    fun getVerificationsForEvent(eventId: String): Flow<List<VerificationEntity>>

    @Query("SELECT COUNT(*) FROM verifications WHERE eventId = :eventId")
    fun getVerificationCount(eventId: String): Flow<Int>

    @Query("DELETE FROM verifications WHERE eventId = :eventId")
    suspend fun deleteVerificationsForEvent(eventId: String)
}
