package com.example.appdevproject26s

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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
    val cameraState =
        rememberCameraState(CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0))

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
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val iconModifier = Modifier.size(72.dp)

            CompassOverlay(
                bearing = cameraState.position.bearing,
                onCompassClick = {
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
                },
                modifier = iconModifier
            )
            IconButton(
                onClick = onMenuClick, modifier = iconModifier
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    modifier = Modifier.fillMaxSize()
                )
            }

        }
    }
}

@Composable
fun CompassOverlay(
    bearing: Double, onCompassClick: () -> Unit, modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onCompassClick,
        modifier = modifier.graphicsLayer { rotationZ = -bearing.toFloat() - 45f }) {
        Icon(
            imageVector = Icons.Default.Explore,
            contentDescription = "Compass",
            modifier = Modifier.fillMaxSize()
        )
    }
}