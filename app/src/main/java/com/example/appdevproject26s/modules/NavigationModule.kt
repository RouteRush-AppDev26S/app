package com.example.appdevproject26s.modules

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.room.Room
import com.example.appdevproject26s.map.route.RouteDao
import com.example.appdevproject26s.map.route.RouteDatabase
import com.example.appdevproject26s.map.route.Zeitberechnung
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun provideRouteDatabase(@ApplicationContext context: Context): RouteDatabase {
        return Room.databaseBuilder(
            context,
            RouteDatabase::class.java,
            "route_database"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideRouteDao(db: RouteDatabase): RouteDao = db.routeDao()

    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Provides
    @Singleton
    fun provideZeitberechnung(): Zeitberechnung = Zeitberechnung()
}
