package com.example.pigeon

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pigeon.data.worker.PurgeWorker
import com.example.pigeon.util.FileLogger
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PigeonApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Capture this process's logcat to a file before anything else logs, so we
        // don't lose the early-startup lines that surround Hilt/MapLibre init.
        FileLogger.init(this)
        org.maplibre.android.MapLibre.getInstance(this)
        schedulePurgeWorker()
    }

    private fun schedulePurgeWorker() {
        val purgeRequest = PeriodicWorkRequestBuilder<PurgeWorker>(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "purge_expired_events",
            ExistingPeriodicWorkPolicy.KEEP,
            purgeRequest
        )
    }
}
