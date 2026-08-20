package com.example.appdevproject26s.map

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiPoint

import androidx.hilt.navigation.compose.hiltViewModel
import org.maplibre.spatialk.geojson.Position

@Composable
fun MapLayer(
    onMenuClick: () -> Unit = {},
    homeViewModel: MapScreenViewModel = hiltViewModel()
) {
    val density = LocalDensity.current

    val scope = rememberCoroutineScope()

    val cameraState = homeViewModel.cameraState

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
            cameraState = homeViewModel.cameraState,
            styleState = rememberStyleState(),
            // 3. Click Interaction
            onMapClick = { point, screenPoint ->
                // Return Pass to allow the event to propagate to other layers
                homeViewModel.onMapTap(point, screenPoint)
                ClickResult.Pass
            },
            onMapLongClick = { point, screenPoint ->
                homeViewModel.onMapPress(point, screenPoint, density)
                ClickResult.Consume
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
            val routePoints = homeViewModel.routePoints
            val trackPoints = homeViewModel.trackPoints

            // Verhindert Absturz, wenn noch keine Route vorhanden ist
            val routeSource = rememberGeoJsonSource(
                data = if (routePoints.size >= 2) {
                    GeoJsonData.Features(LineString(routePoints))
                } else {
                    GeoJsonData.Features(MultiPoint(emptyList()))
                }
            )

            // Tracking-Pfad (Traceroute)
            val trackPathPoints = trackPoints.map { Position(it.lon, it.lat) }
            val trackSource = rememberGeoJsonSource(
                data = if (trackPathPoints.size >= 2) {
                    GeoJsonData.Features(LineString(trackPathPoints))
                } else {
                    GeoJsonData.Features(MultiPoint(emptyList()))
                }
            )

            val pointsSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(
                    MultiPoint(
                        buildList {
                            routePoints.firstOrNull()?.let { add(it) }
                            routePoints.lastOrNull()?.let { add(it) }
                        }
                    )
                )
            )

            val userLocation by homeViewModel.userLocation.collectAsState()
            val locationSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(MultiPoint(listOfNotNull(userLocation)))
            )
            if (userLocation != null) {
                CircleLayer(
                    id = "user-location",
                    source = locationSource,
                    color = const(Color(0xFF2196F3)),
                    radius = const(5.dp)
                )
            }

            val selectedLocation by homeViewModel.selectedLocation.collectAsState()
            val selectedLocationSource = rememberGeoJsonSource(
                data = GeoJsonData.Features(MultiPoint(listOfNotNull(selectedLocation)))
            )
            if (selectedLocation != null) {
                CircleLayer(
                    id = "selected-location",
                    source = selectedLocationSource,
                    color = const(Color.Red),
                    radius = const(7.dp)
                )
            }

            CircleLayer(
                id = "planning-points",
                source = rememberGeoJsonSource(
                    data = GeoJsonData.Features(MultiPoint(homeViewModel.planningPoints.collectAsState().value))
                ),
                color = const(Color.Green),
                radius = const(7.dp)
            )

            //Add Map Layer ex:SymbolLayer、CircleLayer、LineLayer...
            if (routePoints.isNotEmpty()) {
                LineLayer(
                    id = "route-layer",
                    source = routeSource,
                    color = const(Color.Blue),
                    width = const(5.dp)
                )
                CircleLayer(
                    id = "route-points",
                    source = pointsSource,
                    color = const(Color.Red),
                    radius = const(6.dp)
                )
            }

            // Tracking Layer zeichnen
            if (trackPathPoints.size >= 2) {
                LineLayer(
                    id = "track-layer",
                    source = trackSource,
                    color = const(Color(0xFF4CAF50)), // Grün für Tracking
                    width = const(4.dp)
                )
            }
            if (trackPathPoints.isNotEmpty()) {
                CircleLayer(
                    id = "track-points",
                    source = trackSource,
                    color = const(Color(0xFF2E7D32)), // Dunkleres Grün für Punkte
                    radius = const(3.dp)
                )
            }
        }
        if (!homeViewModel.fullscreen.collectAsState().value) {
            IconLayer(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(16.dp),
                onMenuClick = onMenuClick,
                onResetToNorth = resetNorth
            )
        }
    }
}
