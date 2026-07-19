package com.example.appdevproject26s

import android.app.Application
import com.example.appdevproject26s.navigate.Navigate
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        //Navigate.init(this)
    }
}
