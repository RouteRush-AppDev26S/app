package com.example.appdevproject26s

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.navigate.Location
import com.example.appdevproject26s.navigate.Navigate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import android.location.Location as AndroidLocation

const val PIXELS_PER_TILE = 256

class HomeScreenViewModel(private val repository: MapSettingsRepository) : ViewModel() {
    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()
    private var start : Location? = null
    private var first : Boolean=true
    var cameraState = CameraState(
        firstPosition = defaultPos
    )

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

    fun onMapTap(point: Position, screePoint: DpOffset) {
        _fullscreen.value = !_fullscreen.value
    }
    private var lastClickTime: Long = 0
    fun onMapPress(point: Position, screenPoint: DpOffset, localDensity: Density) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) return
        lastClickTime = currentTime

        // use point's coords for to begin navigation open menu/slider for options like navigate to/naviagate from
        Log.d("MAP_PRESS", "Clicked at Lat: ${point.latitude}, Lng: ${point.longitude}")
        Log.d("MAP_PRESS", "Screen pixels - X: ${screenPoint.x}, Y: ${screenPoint.y}")
        //Navigate.updatePosition(Location(point.latitude, point.longitude))
        if(first || start == null) {
            Navigate.triggerVibration(500)
            start = Location(point.latitude, point.longitude)
            first=false
            println("Routenstart ")
        }else {
            val currentStart = start!!

            val earthDistanceMeters = FloatArray(1)
            AndroidLocation.distanceBetween(
                currentStart.lat, currentStart.lon,
                point.latitude, point.longitude,
                earthDistanceMeters
            )
            val distanceInMeters = earthDistanceMeters[0]

            // 2. Calculate the Circumference of the Earth at this Latitude
            // This is a physical constant of the planet, not a map setting.
            val earthCircumference = 40075017.0
            val latRad = point.latitude * PI / 180.0

            // zoom represents how many 2^zoom 256 pixels tiles are used to cover the circumference
            val zoom = cameraState.position.zoom
            val metersPerPixel = (earthCircumference * cos(latRad)) / (PIXELS_PER_TILE * 2.0.pow(zoom))

            val thresholdInMeters = 10.0 * (metersPerPixel * localDensity.density)

            if(distanceInMeters>thresholdInMeters) {
            Navigate.triggerVibration(500)
                Navigate.calcRoute(
                    Location(currentStart.lat, currentStart.lon),
                    Location(point.latitude, point.longitude),
                    "driving-car"
                )
                first = true
                start = null
                println("Route wird berechnet")
            }
        }
    }

    companion object {
        fun provideFactory(repository: MapSettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress ("UNCHECKED_CAST")
                override  fun<T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeScreenViewModel(repository) as T
                }
            }

    }



}
