package com.example.appdevproject26s

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position

class HomeScreenViewModel: ViewModel() {
    private val _fullscreen = MutableStateFlow<Boolean>(false)
    val fullscreen = _fullscreen.asStateFlow()

    var cameraState = CameraState(
        firstPosition = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0)
    )

    fun onMapTap() {
        _fullscreen.value = !_fullscreen.value
    }
}