package com.example.appdevproject26s

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.navigate.Location
import com.example.appdevproject26s.navigate.Navigate
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import javax.inject.Inject

const val PIXELS_PER_TILE = 256

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: MapSettingsRepository,
    private val navigate: Navigate,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {
    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()
    var cameraState = CameraState(firstPosition = defaultPos)

    val routePoints get() = navigate.routePoints
    val currentTrip get() = navigate.currentTrip
    val totalLengthKM get() = navigate.totalLengthKM
    val durationSeconds get() = navigate.durationSeconds
    val speedLimit get() = navigate.speedLimit
    val manoevertext get() = navigate.manoevertext
    val startAddress get() = navigate.startAddress
    val destinationAddress get() = navigate.destinationAddress
    val isCalculating get() = navigate.isCalculating
    val errorMessage get() = navigate.errorMessage

    private val _userLocation = MutableStateFlow<Position?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _userLocation.value = Position(loc.longitude, loc.latitude)
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.savedPosition.firstOrNull()?.let { restoredPos ->
                cameraState.position = restoredPos
            }

            snapshotFlow { cameraState.position }
                .collectLatest { position ->
                    delay(1000)
                    repository.saveCameraPosition(position)
                }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun stopLocationUpdates() {
        locationClient.removeLocationUpdates(locationCallback)
    }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
    }

    fun onMapTap(point: Position, screePoint: DpOffset) {
        _fullscreen.value = !_fullscreen.value
    }

    private var lastClickTime: Long = 0
    fun onMapPress(point: Position, screenPoint: DpOffset, localDensity: Density) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) return
        lastClickTime = currentTime

        val startLocation = _userLocation.value?.let { Location(it.latitude, it.longitude) } ?: return

        navigate.triggerVibration(500)
        navigate.calcRoute(
            startLocation,
            Location(point.latitude, point.longitude),
            "driving-car"
        )
    }
}
