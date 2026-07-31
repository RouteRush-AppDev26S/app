package com.example.appdevproject26s

import android.app.Application
import com.example.appdevproject26s.network.SessionManager
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class MapApplication : Application() {
    @Inject lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        //Navigate.init(this)
    }
}
