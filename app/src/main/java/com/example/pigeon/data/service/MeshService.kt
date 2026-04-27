package com.example.pigeon.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.pigeon.MainActivity
import com.example.pigeon.R
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.network.ConnectionStatus
import com.example.pigeon.domain.network.NearbySyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Long-running foreground service that owns the mesh radio's lifetime.
 *
 * Without this, the OS will Doze/throttle the process the moment the user
 * leaves the app — and Nearby Connections falls silent. The service is a
 * permanent foreground host: it pins the process's priority high enough to
 * keep the duty cycle, advertising, and discovery callbacks firing while
 * the screen is off and the app is backgrounded.
 *
 * The service is the only allowed entry point for *user-driven* power-state
 * mutations (OFF / PASSIVE / ACTIVE / sticky-ACTIVE). Internal radio
 * reactions inside [NearbySyncManager] (proximity-wave self-toggling,
 * post-wave PASSIVE revert) still call the manager directly — those are
 * automatic and don't represent a user intent.
 */
@AndroidEntryPoint
class MeshService : LifecycleService() {

    @Inject lateinit var nearbySyncManager: NearbySyncManager

    private var wakeLock: PowerManager.WakeLock? = null
    private val prefs by lazy { getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        startInForeground(buildNotification("Pigeon Mesh Active", "Searching for nearby peers..."))
        observeMeshState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action stickyRestart=${intent == null}")

        if (intent == null) {
            // Process restart after START_STICKY — restore the persisted state so the
            // user's OFF/PASSIVE/ACTIVE choice survives an OS-initiated kill.
            val savedState = readPersistedState()
            val savedSticky = readPersistedSticky()
            Log.d(TAG, "Sticky restart: restoring state=$savedState sticky=$savedSticky")
            if (savedState == MeshPowerState.OFF) {
                stopMeshAndService()
            } else {
                nearbySyncManager.togglePowerState(savedState, savedSticky)
            }
            return START_STICKY
        }

        when (action) {
            ACTION_START_PASSIVE -> applyPowerState(MeshPowerState.PASSIVE, isSticky = false)
            ACTION_START_PASSIVE_STICKY -> applyPowerState(MeshPowerState.PASSIVE, isSticky = true)
            ACTION_START_ACTIVE -> applyPowerState(MeshPowerState.ACTIVE, isSticky = false)
            ACTION_START_ACTIVE_STICKY -> applyPowerState(MeshPowerState.ACTIVE, isSticky = true)
            ACTION_STOP -> {
                stopMeshAndService()
                return START_NOT_STICKY
            }
            else -> Log.w(TAG, "Unknown action '$action' — keeping current radio state")
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy — releasing wake lock")
        releaseWakeLock()
        super.onDestroy()
    }

    private fun applyPowerState(state: MeshPowerState, isSticky: Boolean) {
        persistState(state, isSticky)
        nearbySyncManager.togglePowerState(state, isSticky)
    }

    private fun stopMeshAndService() {
        Log.d(TAG, "Stopping mesh and tearing down service")
        nearbySyncManager.stop()
        persistState(MeshPowerState.OFF, false)
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun observeMeshState() {
        // Drive the persistent notification text from the live radio state. The
        // distinctUntilChanged() prevents a notification post on every peer-list
        // diff (RSSI bumps, lastSeen updates, etc.) — only structural changes
        // (peer count crossing a threshold, status flip) re-render the text.
        lifecycleScope.launch {
            combine(
                nearbySyncManager.nearbyPeers,
                nearbySyncManager.status
            ) { peers, status ->
                val connected = peers.count { it.isConnected }
                val seen = peers.size
                buildNotificationText(connected, seen, status)
            }
                .distinctUntilChanged()
                .collect { (title, body) -> updateNotification(title, body) }
        }

        // Hold a partial wake lock while a Set-Union session is in progress so
        // the CPU can't sleep mid-payload. Released as soon as all sessions
        // close — partial lock is cheap but we don't want to hold it idle.
        lifecycleScope.launch {
            nearbySyncManager.isSyncing.collect { syncing ->
                if (syncing) acquireWakeLock() else releaseWakeLock()
            }
        }
    }

    private fun buildNotificationText(
        connected: Int,
        seen: Int,
        status: ConnectionStatus
    ): Pair<String, String> {
        val title = when (status) {
            is ConnectionStatus.OFF -> "Pigeon Mesh Stopped"
            is ConnectionStatus.PASSIVE -> "Pigeon Mesh Active"
            is ConnectionStatus.ACTIVE -> "Pigeon Mesh Active"
        }
        val body = when {
            connected == 1 -> "1 peer in range"
            connected > 1 -> "$connected peers in range"
            seen > 0 -> "Searching nearby ($seen seen recently)"
            else -> "Searching for nearby peers..."
        }
        return title to body
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pigeon Mesh",
            NotificationManager.IMPORTANCE_LOW // LOW = no sound, no peek; visible but quiet.
        ).apply {
            description = "Keeps the peer-to-peer mesh radio alive in the background."
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, body: String): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val contentPi = PendingIntent.getActivity(this, 0, tapIntent, pendingFlags)

        val stopIntent = Intent(this, MeshService::class.java).setAction(ACTION_STOP)
        val stopPi = PendingIntent.getService(this, 1, stopIntent, pendingFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(contentPi)
            .addAction(0, "Stop mesh", stopPi)
            .build()
    }

    private fun startInForeground(notification: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun updateNotification(title: String, body: String) {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.notify(NOTIFICATION_ID, buildNotification(title, body))
    }

    // ── Wake lock ─────────────────────────────────────────────────────────────

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            // 10 min ceiling — a sync session normally takes seconds; the timeout
            // is a backstop so a stuck flow can't drain the battery overnight.
            acquire(10 * 60 * 1000L)
        }
        Log.d(TAG, "WakeLock acquired (sync in progress)")
    }

    private fun releaseWakeLock() {
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                try { lock.release() } catch (_: Throwable) {}
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistState(state: MeshPowerState, isSticky: Boolean) {
        prefs.edit()
            .putString(KEY_STATE, state.name)
            .putBoolean(KEY_STICKY, isSticky)
            .apply()
    }

    private fun readPersistedState(): MeshPowerState {
        val name = prefs.getString(KEY_STATE, MeshPowerState.PASSIVE.name)
        return runCatching { MeshPowerState.valueOf(name!!) }.getOrDefault(MeshPowerState.PASSIVE)
    }

    private fun readPersistedSticky(): Boolean = prefs.getBoolean(KEY_STICKY, false)

    companion object {
        private const val TAG = "[MESH_SERVICE]"
        private const val CHANNEL_ID = "pigeon_mesh_channel"
        private const val NOTIFICATION_ID = 0xB1ED
        private const val WAKE_LOCK_TAG = "Pigeon::MeshSync"
        private const val PREFS_NAME = "mesh_service_state"
        private const val KEY_STATE = "last_power_state"
        private const val KEY_STICKY = "last_sticky"

        const val ACTION_START_PASSIVE = "com.example.pigeon.action.START_PASSIVE"
        const val ACTION_START_PASSIVE_STICKY = "com.example.pigeon.action.START_PASSIVE_STICKY"
        const val ACTION_START_ACTIVE = "com.example.pigeon.action.START_ACTIVE"
        const val ACTION_START_ACTIVE_STICKY = "com.example.pigeon.action.START_ACTIVE_STICKY"
        const val ACTION_STOP = "com.example.pigeon.action.STOP"
    }
}
