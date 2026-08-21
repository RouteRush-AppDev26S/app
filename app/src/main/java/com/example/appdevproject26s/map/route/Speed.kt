package com.example.appdevproject26s.map.route

import android.Manifest
import android.content.Context
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Sinnvoll, damit nicht mehrere Clients parallel Tracker starten
class Speed @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Client wird jetzt sauber mit dem injizierten Context initialisiert
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Callback als Property auslagern, damit wir das Tracking auch stoppen können
    private var locationCallback: LocationCallback? = null

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000L
    ).build()

    var timerwert  : Long by mutableStateOf(locationRequest.durationMillis/1000)
    var speedKmH: Float? by mutableStateOf(null)
        private set
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    fun startSpeedTracking(onSpeedUpdated: (Float) -> Unit) {
        // Falls schon ein Tracking läuft, erst stoppen (Vermeidung von Duplikaten)
        stopSpeedTracking()



        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val currentSpeed = if (location.hasSpeed()) {
                        location.speed * 3.6f
                    } else {
                        0.0f
                    }

                    // Compose State aktualisieren
                    speedKmH = currentSpeed
                    // Lambda aufrufen für zusätzliche Logik im UI / ViewModel
                    onSpeedUpdated(currentSpeed)
                }
            }
        }

        // Looper.getMainLooper() direkt vom System holen
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    /**
     * Wichtig! Rufe diese Methode auf, wenn das ViewModel/die Activity zerstört wird.
     */
    fun stopSpeedTracking() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            locationCallback = null
        }
    }

}