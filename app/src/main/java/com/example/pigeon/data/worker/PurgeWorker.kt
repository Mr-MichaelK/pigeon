package com.example.pigeon.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pigeon.data.local.dao.EventDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PurgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val eventDao: EventDao
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "PurgeWorker"
        private const val TTL_HOURS = 72L
    }

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - (TTL_HOURS * 60 * 60 * 1000L)
        val deletedCount = eventDao.deleteExpiredEvents(cutoff)
        Log.i(TAG, "\uD83E\uDDF9 Purge Controller: Cleared $deletedCount expired events from the ledger.")
        return Result.success()
    }
}
