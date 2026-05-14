package com.example.appdevproject26s

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLayer(onMenuClick: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    var fullscreen by remember { mutableStateOf(false) }
    val cameraState =
        rememberCameraState(CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0))

    val resetNorth: () -> Unit = {
        scope.launch {
            val startBearing = cameraState.position.bearing
            val targetBearing =
                if (startBearing > 180) startBearing - 360 else startBearing
            val animatable = Animatable(targetBearing.toFloat())

            animatable.animateTo(
                targetValue = 0f, animationSpec = tween(
                    durationMillis = 400, easing = FastOutSlowInEasing
                )
            ) {
                cameraState.position = CameraPosition(
                    target = cameraState.position.target,
                    zoom = cameraState.position.zoom,
                    bearing = this@animateTo.value.toDouble()
                )
            }
        }

    }

    Box {
        MaplibreMap(
            // 1. Set the style (as discussed previously)
            baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
            // 2. State Management
            cameraState = cameraState,
            styleState = rememberStyleState(),
            // 3. Click Interaction
            onMapClick = { point, screenPoint ->
                // Return Pass to allow the event to propagate to other layers
                fullscreen = !fullscreen
                ClickResult.Pass
            },
            // 4. Map Options (UI and Gestures)
            options =
                MapOptions(
                    // Disable default UI elements (Compass, Logo, etc.)
                    ornamentOptions = OrnamentOptions.AllDisabled,
                    // Explicitly enable user gestures
                    gestureOptions =
                        GestureOptions(
                            isTiltEnabled = true,
                            isZoomEnabled = true,
                            isRotateEnabled = true,
                            isScrollEnabled = true,
                        ),
                ),
        ) {
            //Add Map Layer ex:SymbolLayer、CircleLayer、LineLayer...
        }
        if (!fullscreen) {
            IconLayer(
                cameraState = cameraState,
                onResetToNorth = resetNorth,
                onMenuClick = onMenuClick
            )
        }
    }
}

