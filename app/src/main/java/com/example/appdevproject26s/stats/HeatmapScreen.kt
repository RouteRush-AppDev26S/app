package com.example.appdevproject26s.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.social.LoginPrompt
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.MultiPoint
import org.maplibre.spatialk.geojson.Position

@Composable
fun HeatmapScreen(
    navController: NavController,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val range by viewModel.range.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.load()
    }

    ScreenScaffold(navController = navController, title = stringResource(R.string.heatmap_title)) {
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "your heatmap",
                onNavigateToLogin = { navController.navigate("profile") }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HeatmapRange.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = range == entry,
                            onClick = { viewModel.setRange(entry) },
                            shape = SegmentedButtonDefaults.itemShape(index, HeatmapRange.entries.size)
                        ) {
                            Text(entry.label)
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    val ready = uiState as? HeatmapUiState.Ready
                    HeatmapMap(
                        pings = ready?.pings ?: emptyList(),
                        paths = ready?.paths ?: emptyList()
                    )

                    when (val state = uiState) {
                        is HeatmapUiState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )

                        is HeatmapUiState.Error -> OverlayHint(
                            text = state.message,
                            modifier = Modifier.align(Alignment.TopCenter)
                        )

                        is HeatmapUiState.Ready -> if (state.pings.isEmpty() && state.paths.isEmpty()) {
                            OverlayHint(
                                text = "Noch keine Bewegungsdaten im Zeitraum – starte ein Tracking auf der Karte!",
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapMap(pings: List<LocationPingResponse>, paths: List<RoutePath>) {
    val cameraState = remember {
        CameraState(
            firstPosition = CameraPosition(target = Position(14.2659460, 46.6163897), zoom = 11.0)
        )
    }

    // Center the camera on the data once it arrives
    LaunchedEffect(pings) {
        if (pings.isNotEmpty()) {
            val centerLat = pings.sumOf { it.lat } / pings.size
            val centerLng = pings.sumOf { it.lng } / pings.size
            cameraState.position = CameraPosition(
                target = Position(centerLng, centerLat),
                zoom = 12.0
            )
        }
    }

    MaplibreMap(
        baseStyle = BaseStyle.Uri("https://tiles.openfreemap.org/styles/liberty"),
        cameraState = cameraState,
        styleState = rememberStyleState(),
        options = MapOptions(ornamentOptions = OrnamentOptions.AllDisabled)
    ) {
        // Strava-style frequency glow via translucency stacking, drawn in three
        // passes (all halos, then all mids, then all cores) so that the bright
        // cores of overlapping trips stack ON TOP of every halo. A path traveled
        // once reads as a thin dark red line; a corridor traveled many times
        // builds up to a glowing orange band with a bright yellow center.
        val lineSources = paths.mapIndexed { index, path ->
            key(index) {
                rememberGeoJsonSource(
                    data = GeoJsonData.Features(
                        LineString(path.points.map { (lat, lng) -> Position(lng, lat) })
                    )
                )
            }
        }
        lineSources.forEachIndexed { index, lineSource ->
            key(index) {
                LineLayer(
                    id = "route-halo-$index",
                    source = lineSource,
                    color = const(Color(0xFF67001F)),
                    width = const(8.dp),
                    opacity = const(0.15f),
                    blur = const(3.dp),
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round)
                )
            }
        }
        lineSources.forEachIndexed { index, lineSource ->
            key(index) {
                LineLayer(
                    id = "route-mid-$index",
                    source = lineSource,
                    color = const(Color(0xFFE64A19)),
                    width = const(3.dp),
                    opacity = const(0.30f),
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round)
                )
            }
        }
        lineSources.forEachIndexed { index, lineSource ->
            key(index) {
                LineLayer(
                    id = "route-core-$index",
                    source = lineSource,
                    color = const(Color(0xFFFFD54F)),
                    width = const(1.5.dp),
                    opacity = const(0.30f),
                    cap = const(LineCap.Round),
                    join = const(LineJoin.Round)
                )
            }
        }

        // Faint dots from raw pings add the fine "traveled here" texture
        val pingSource = rememberGeoJsonSource(
            data = GeoJsonData.Features(
                MultiPoint(pings.map { Position(it.lng, it.lat) })
            )
        )
        if (pings.isNotEmpty()) {
            CircleLayer(
                id = "ping-dots",
                source = pingSource,
                color = const(Color(0xFF67001F)),
                radius = const(1.5.dp),
                opacity = const(0.3f)
            )
        }
    }
}

@Composable
private fun OverlayHint(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
