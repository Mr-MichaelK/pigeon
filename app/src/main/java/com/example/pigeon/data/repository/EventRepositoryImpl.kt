package com.example.pigeon.data.repository

import android.util.Log
import com.example.pigeon.data.local.dao.EventDao
import com.example.pigeon.data.local.entities.toDomain
import com.example.pigeon.data.local.entities.toEntity
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.repository.EventRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val mockDataGenerator: MockDataGenerator
) : EventRepository {

    override fun getAllEvents(): Flow<List<Event>> =
        eventDao.getAllEvents().map { entities -> entities.map { it.toDomain() } }

    override fun getUnresolvedEvents(): Flow<List<Event>> =
        eventDao.getUnresolvedEvents().map { entities -> entities.map { it.toDomain() } }

    /**
     * Distributed Trust write rule. The DAO uses IGNORE on conflicts so a row
     * can never be silently clobbered. Updates are gated here:
     *   - new eventId → INSERT
     *   - same eventId, same creatorDeviceId, strictly newer timestamp → UPDATE
     *   - anything else (different creator OR same/older timestamp) → DROP
     *
     * Signature validity is the upstream caller's responsibility (the network
     * layer rejects unsigned/invalid messages before they reach this repo). The
     * binding `creatorDeviceId == SHA-256(public_key)` is enforced during that
     * verification, so trusting `creatorDeviceId` here transitively trusts the
     * cryptographic identity behind it.
     */
    override suspend fun createEvent(event: Event) {
        val existing = eventDao.getEventById(event.eventId)
        if (existing == null) {
            eventDao.insertEvent(event.toEntity())
            return
        }
        if (existing.creatorDeviceId != event.creatorDeviceId) {
            Log.w(
                TAG,
                "🚫 Dropping event ${event.eventId} — creator mismatch " +
                    "(existing=${existing.creatorDeviceId.take(12)}…, " +
                    "incoming=${event.creatorDeviceId.take(12)}…)."
            )
            return
        }
        if (event.timestamp <= existing.timestamp) {
            Log.d(
                TAG,
                "Dropping event ${event.eventId} — incoming timestamp " +
                    "${event.timestamp} is not newer than ${existing.timestamp}."
            )
            return
        }
        eventDao.updateEvent(event.toEntity())
        Log.d(TAG, "✓ Updated event ${event.eventId} from same creator (newer timestamp).")
    }

    override suspend fun resolveEvent(eventId: String) {
        eventDao.updateEventStatus(eventId, true)
    }
    
    override suspend fun searchEvents(query: String): List<Event> {
        return eventDao.searchEvents(query).map { it.toDomain() }
    }

    override suspend fun populateMockData() {
        eventDao.deleteAllEvents() // Ensure clean slate on launch
        val mockEntities = mockDataGenerator.generateMockEvents()
        eventDao.insertEvents(mockEntities)
    }

    override suspend fun clearAllEvents() {
        eventDao.deleteAllEvents()
    }

    override suspend fun getRecentEventCount(creatorId: String, sinceTimestamp: Long): Int {
        return eventDao.getRecentEventCountByCreator(creatorId, sinceTimestamp)
    }

    override suspend fun getCooldownBaseTimestamp(creatorId: String, sinceTimestamp: Long): Long? {
        return eventDao.getCooldownBaseTimestamp(creatorId, sinceTimestamp)
    }

    override suspend fun resetUserCooldown(creatorId: String, sinceTimestamp: Long) {
        eventDao.deleteRecentEventsByCreator(creatorId, sinceTimestamp)
    }

    companion object {
        private const val TAG = "[EVENT_REPO]"
    }
}
