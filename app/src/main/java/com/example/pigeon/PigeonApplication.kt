package com.example.pigeon

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PigeonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        org.maplibre.android.MapLibre.getInstance(this)
    }
}
