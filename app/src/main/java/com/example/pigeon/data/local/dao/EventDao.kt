package com.example.pigeon.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pigeon.data.local.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getUnresolvedEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE eventId = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): EventEntity?

    // Distributed Trust: REPLACE was the prior policy, which let any incoming
    // event with a colliding eventId silently overwrite the original — so a
    // peer could clobber someone else's event by re-broadcasting it under
    // their own creatorDeviceId. With IGNORE, the first writer wins at the
    // SQL layer, and the repository layer enforces the secondary rule (same
    // original creator + strictly newer timestamp gates a controlled update).
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(event: EventEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvents(events: List<EventEntity>)

    /**
     * Full-row replace gated by the repository's signature/creator/timestamp
     * check. Use this only after `getEventById` confirms the existing row was
     * authored by the same creatorDeviceId and the incoming timestamp strictly
     * exceeds the existing one — otherwise data integrity rules are bypassed.
     */
    @Update
    suspend fun updateEvent(event: EventEntity)

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
