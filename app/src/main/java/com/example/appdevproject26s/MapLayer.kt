package com.example.appdevproject26s

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLayer(innerPaddingValues: PaddingValues) {
    val cameraState =
        rememberCameraState(CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 12.0))
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
//                ornamentOptions = OrnamentOptions.AllDisabled,
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
}