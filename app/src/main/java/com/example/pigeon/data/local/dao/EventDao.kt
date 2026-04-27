package com.example.pigeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pigeon.data.local.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("UPDATE events SET isResolved = :isResolved WHERE eventId = :eventId")
    suspend fun updateEventStatus(eventId: String, isResolved: Boolean)
    
    @Query("SELECT * FROM events WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    suspend fun searchEvents(query: String): List<EventEntity>

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("DELETE FROM events WHERE expiryTimestamp < :currentTime")
    suspend fun deleteExpiredEvents(currentTime: Long): Int

    @Query("SELECT COUNT(*) FROM events WHERE creatorDeviceId = :creatorId AND timestamp >= :sinceTimestamp")
    suspend fun getRecentEventCountByCreator(creatorId: String, sinceTimestamp: Long): Int

    @Query("SELECT timestamp FROM events WHERE creatorDeviceId = :creatorId AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC LIMIT 1 OFFSET 2")
    suspend fun getCooldownBaseTimestamp(creatorId: String, sinceTimestamp: Long): Long?

    @Query("DELETE FROM events WHERE creatorDeviceId = :creatorId AND timestamp >= :sinceTimestamp")
    suspend fun deleteRecentEventsByCreator(creatorId: String, sinceTimestamp: Long)
}
