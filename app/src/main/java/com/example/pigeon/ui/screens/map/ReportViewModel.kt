package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val nearbySyncManager: NearbySyncManager
) : ViewModel() {

    fun reportEvent(
        eventType: EventType,
        title: String,
        description: String,
        ttlMillis: Long,
        latitude: Double,
        longitude: Double
    ) {
        viewModelScope.launch {
            val user = userRepository.getUser().first()
            val eventId = UUID.randomUUID().toString()
            val event = Event(
                eventId = eventId,
                creatorDeviceId = user?.nodeId ?: "UNKNOWN-NODE",
                eventType = eventType,
                title = title,
                description = description,
                latitude = latitude,
                longitude = longitude,
                timestamp = System.currentTimeMillis(),
                isResolved = false,
                creatorName = user?.displayName ?: "Unknown",
                ttl = ttlMillis
            )
            
            // 1. Save to local ledger
            eventRepository.createEvent(event)
            
            // 2. Upgrade to Sticky ACTIVE (Rule 2)
            nearbySyncManager.togglePowerState(MeshPowerState.ACTIVE, isSticky = true)
            
            // 3. Initiate Wave for others
            nearbySyncManager.startProximityWave()

            // 4. Broadcast to nearby peers immediately
            val isAnonymous = user?.isAnonymous ?: false
            val protoEvent = domainToProto(event, isAnonymous)
            nearbySyncManager.broadcastIncident(protoEvent)
        }
    }

    private fun domainToProto(event: Event, isAnonymous: Boolean = false): PigeonEvent {
        val protoType = when (event.eventType) {
            EventType.FIRE -> com.example.pigeon.proto.EventType.FIRE
            EventType.MEDICAL -> com.example.pigeon.proto.EventType.MEDICAL
            EventType.SUPPLIES -> com.example.pigeon.proto.EventType.RESOURCE
            EventType.CONFLICT -> com.example.pigeon.proto.EventType.INFRASTRUCTURE
            EventType.CUSTOM -> com.example.pigeon.proto.EventType.CUSTOM
            EventType.SOS -> com.example.pigeon.proto.EventType.SOS
        }
        return PigeonEvent.newBuilder()
            .setEventId(event.eventId)
            .setEventType(protoType)
            .setDescription(event.description)
            .setLatitude(event.latitude)
            .setLongitude(event.longitude)
            .setTimestamp(event.timestamp)
            .setCreatorDeviceId(event.creatorDeviceId)
            .setIsResolved(event.isResolved)
            .setCreatorName(if (isAnonymous) "Anonymous Civilian" else event.creatorName)
            .build()
    }
}
