package com.example.appdevproject26s

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun IconLayer(
    modifier: Modifier = Modifier,
    onResetToNorth: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val context = LocalContext.current

    val app = context.applicationContext as MapApplication
    val repository = app.repository

    val homeViewModel: HomeScreenViewModel = viewModel(
        factory = HomeScreenViewModel.provideFactory(repository)
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val iconModifier = Modifier.size(72.dp)

            CompassOverlay(
                bearing = homeViewModel.cameraState.position.bearing,
                onCompassClick = onResetToNorth,

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
    modifier: Modifier = Modifier,
    bearing: Double,
    onCompassClick: () -> Unit
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