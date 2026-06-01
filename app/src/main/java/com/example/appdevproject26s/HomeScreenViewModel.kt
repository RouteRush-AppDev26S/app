package com.example.appdevproject26s

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

class HomeScreenViewModel(private val repository: MapSettingsRepository) : ViewModel() {
    private val defaultPos = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()

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

    fun onMapPress(point: Position, screenPoint: DpOffset) {
        // use point's coords for to begin navigation open menu/slider for options like navigate to/naviagate from
        Log.d("MAP_PRESS", "Clicked at Lat: ${point.latitude}, Lng: ${point.longitude}")
        Log.d("MAP_PRESS", "Screen pixels - X: ${screenPoint.x}, Y: ${screenPoint.y}")
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