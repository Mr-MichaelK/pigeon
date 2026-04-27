package com.example.pigeon.data.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.pigeon.domain.model.MeshPowerState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin write-side facade ViewModels use to ask [MeshService] to change the
 * radio's power state. Reads still come from [com.example.pigeon.domain.network.NearbySyncManager]'s
 * StateFlows directly — those are the source of truth.
 *
 * Centralising the Intent dispatch here keeps every caller consistent: they
 * all go through [ContextCompat.startForegroundService], which is what
 * Android 8+ requires for a backgrounded process to start the service legally.
 */
@Singleton
class MeshServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun setPowerState(state: MeshPowerState, isSticky: Boolean = false) {
        when (state) {
            MeshPowerState.OFF -> dispatch(MeshService.ACTION_STOP, asForegroundStart = false)
            MeshPowerState.PASSIVE ->
                if (isSticky) dispatch(MeshService.ACTION_START_PASSIVE_STICKY)
                else dispatch(MeshService.ACTION_START_PASSIVE)
            MeshPowerState.ACTIVE ->
                if (isSticky) dispatch(MeshService.ACTION_START_ACTIVE_STICKY)
                else dispatch(MeshService.ACTION_START_ACTIVE)
        }
    }

    private fun dispatch(action: String, asForegroundStart: Boolean = true) {
        val intent = Intent(context, MeshService::class.java).setAction(action)
        if (asForegroundStart) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            // Stop is delivered via plain startService — the service is already
            // running; we just want it to receive the ACTION_STOP and tear down.
            context.startService(intent)
        }
    }
}
