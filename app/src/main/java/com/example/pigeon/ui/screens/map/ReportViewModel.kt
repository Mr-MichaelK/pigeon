package com.example.pigeon.ui.screens.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Event
import com.example.pigeon.domain.model.EventType
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.proto.PigeonEvent
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.data.service.MeshServiceController
import com.example.pigeon.data.identity.IdentityKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val nearbySyncManager: NearbySyncManager,
    private val meshController: MeshServiceController,
    private val identityKeyManager: IdentityKeyManager
) : ViewModel() {
    
    companion object {
        private const val MAX_TTL_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 Days
    }

    private val _canReport = MutableStateFlow(true)
    val canReport = _canReport.asStateFlow()

    private val _cooldownTimeRemaining = MutableStateFlow<String?>(null)
    val cooldownTimeRemaining = _cooldownTimeRemaining.asStateFlow()

    private val _reportError = MutableSharedFlow<String>()
    val reportError = _reportError.asSharedFlow()

    init {
        viewModelScope.launch {
            while(true) {
                checkCooldownStatus()
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    private suspend fun checkCooldownStatus() {
        val user = userRepository.getUser().first()
        val nodeId = user?.nodeId ?: return

        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)

        val count = eventRepository.getRecentEventCount(nodeId, oneHourAgo)
        if (count >= 3) {
            _canReport.value = false
            val baseTimestamp = eventRepository.getCooldownBaseTimestamp(nodeId, oneHourAgo)
            if (baseTimestamp != null) {
                val availableAt = baseTimestamp + (60 * 60 * 1000)
                val diff = availableAt - now
                if (diff > 0) {
                    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff)
                    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(diff) % 60
                    _cooldownTimeRemaining.value = String.format("%02d:%02d", minutes, seconds)
                } else {
                    _canReport.value = true
                    _cooldownTimeRemaining.value = null
                }
            } else {
                _cooldownTimeRemaining.value = null
            }
        } else {
            _canReport.value = true
            _cooldownTimeRemaining.value = null
        }
    }

    fun reportEvent(
        eventType: EventType,
        title: String,
        description: String,
        ttlMillis: Long,
        latitude: Double,
        longitude: Double,
        onLimitReached: () -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!_canReport.value) {
                onLimitReached()
                _reportError.emit("Report limit reached. Please wait before broadcasting more data to the mesh.")
                return@launch
            }
            val user = userRepository.getUser().first()
            val eventId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val sanitizedTtl = minOf(ttlMillis, MAX_TTL_MILLIS)
            
            val isAnonymous = user?.isAnonymous ?: false
            val resolvedCreatorName = if (isAnonymous) {
                "Anonymous Civilian"
            } else {
                user?.displayName.takeIf { !it.isNullOrBlank() }
                    ?: user?.nodeName.takeIf { !it.isNullOrBlank() }
                    ?: "Unknown"
            }

            // Build the proto unsigned, then sign once — the manager stamps
            // creatorDeviceId / creator_public_key / signature atomically. We
            // keep the signed proto as the canonical artifact and derive both
            // the persisted Event and the broadcast payload from it, so the
            // bytes on the wire are identical to the bytes in the database.
            val unsignedBuilder = PigeonEvent.newBuilder()
                .setEventId(eventId)
                .setEventType(domainEventTypeToProto(eventType))
                .setDescription(description)
                .setLatitude(latitude)
                .setLongitude(longitude)
                .setTimestamp(now)
                .setIsResolved(false)
                .setCreatorName(resolvedCreatorName)
                .setTitle(title)
                .setExpiryTimestamp(now + sanitizedTtl)
            val signed = identityKeyManager.signEvent(unsignedBuilder)

            val event = Event(
                eventId = signed.eventId,
                creatorDeviceId = signed.creatorDeviceId, // == identityKeyManager.signerId
                eventType = eventType,
                title = signed.title,
                description = signed.description,
                latitude = signed.latitude,
                longitude = signed.longitude,
                timestamp = signed.timestamp,
                isResolved = signed.isResolved,
                creatorName = signed.creatorName,
                ttl = sanitizedTtl,
                expiryTimestamp = signed.expiryTimestamp,
                creatorPublicKey = signed.creatorPublicKey.toByteArray(),
                signature = signed.signature.toByteArray()
            )

            // 1. Save to local ledger
            eventRepository.createEvent(event)

            // 2. Upgrade to Sticky ACTIVE (Rule 2) via the foreground service so
            //    the radio survives backgrounding while the report propagates.
            meshController.setPowerState(MeshPowerState.ACTIVE, isSticky = true)

            // 3. Initiate Wave for others (radio-internal — no service hop needed)
            nearbySyncManager.startProximityWave()

            // 4. Broadcast to nearby peers immediately
            nearbySyncManager.broadcastIncident(signed)
        }
    }

    fun onReportErrorShown() {
        viewModelScope.launch {
            _reportError.emit("")
        }
    }

    fun resetCooldown() {
        viewModelScope.launch {
            val user = userRepository.getUser().first()
            val userId = user?.nodeId ?: return@launch
            val hourAgo = System.currentTimeMillis() - (1 * 60 * 60 * 1000L)
            
            eventRepository.resetUserCooldown(userId, hourAgo)
            
            // Immediate manual check to refresh UI state
            val count = eventRepository.getRecentEventCount(userId, hourAgo)
            _canReport.value = count < 3
            if (_canReport.value) {
                _cooldownTimeRemaining.value = ""
            }
            
            Log.d("DEBUG_PIGEON", "Cooldown reset command executed for $userId")
        }
    }

    private fun domainEventTypeToProto(type: EventType): com.example.pigeon.proto.EventType =
        when (type) {
            EventType.FIRE -> com.example.pigeon.proto.EventType.FIRE
            EventType.MEDICAL -> com.example.pigeon.proto.EventType.MEDICAL
            EventType.SUPPLIES -> com.example.pigeon.proto.EventType.RESOURCE
            EventType.CONFLICT -> com.example.pigeon.proto.EventType.INFRASTRUCTURE
            EventType.CUSTOM -> com.example.pigeon.proto.EventType.CUSTOM
            EventType.SOS -> com.example.pigeon.proto.EventType.SOS
        }
}
