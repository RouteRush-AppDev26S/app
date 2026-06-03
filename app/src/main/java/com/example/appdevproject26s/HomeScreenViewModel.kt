package com.example.appdevproject26s

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.navigate.Navigate
import com.example.appdevproject26s.navigate.Location
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import kotlin.math.pow
import kotlin.math.sqrt

class HomeScreenViewModel(private val repository: MapSettingsRepository) : ViewModel() {
    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()
    private var start : Location= Location(0.0,0.0);
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
    private var lastDp: DpOffset = DpOffset.Zero
    fun onMapPress(point: Position, screenPoint: DpOffset) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < 500) return
        lastClickTime = currentTime

        // use point's coords for to begin navigation open menu/slider for options like navigate to/naviagate from
        Log.d("MAP_PRESS", "Clicked at Lat: ${point.latitude}, Lng: ${point.longitude}")
        Log.d("MAP_PRESS", "Screen pixels - X: ${screenPoint.x}, Y: ${screenPoint.y}")
        //Navigate.updatePosition(Location(point.latitude, point.longitude))
        val abstand=sqrt((screenPoint.x-lastDp.x).value.pow(2) + (screenPoint.y-lastDp.y).value.pow(2)).dp
        if(first){
            Navigate.triggerVibration(500)
            start.lon=point.longitude
            start.lat=point.latitude
            first=false
            println("Routenstart ")
        }else if(abstand>10.dp) {
            Navigate.triggerVibration(500)
            Navigate.calcRoute(
                Location(start.lat, start.lon),
                Location(point.latitude, point.longitude),
                "driving-car"
            )
            first=true;
            start.lat=0.0
            println("Route wird berechnet")
        }
        lastDp = screenPoint
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
