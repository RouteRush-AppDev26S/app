package com.example.appdevproject26s.route

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold

@Composable
fun HomeScreen(navController: NavController) {
    val viewModel: HomeScreenViewModel = hiltViewModel()
    val isPlanningMode by viewModel.isPlanningMode.collectAsState()
    val isSelectingDestination by viewModel.isSelectingDestination.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.startLocationUpdates()
        }
    }

    val stepsPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.contains(viewModel.stepsReadPermission)) {
            viewModel.syncSteps()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn && viewModel.isHealthConnectAvailable()) {
            if (viewModel.hasStepsPermission()) {
                viewModel.syncSteps()
            } else {
                stepsPermissionLauncher.launch(setOf(viewModel.stepsReadPermission))
            }
        }
    }

    ScreenScaffold(
        navController = navController,
        title = stringResource(R.string.home_title),
        showTopBar = false,
        showBackButton = false,
        useInnerPadding = false
    ) { openDrawer ->
        Box(modifier = Modifier.fillMaxSize()) {
            MapLayer(onMenuClick = { openDrawer() })

            if (isSelectingDestination) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 8.dp
                ) {
                    Text(
                        text = "Bitte Ziel auf der Karte lange drücken",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { viewModel.togglePlanningMode() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = "Routenplanung")
                }
            }

            if (isPlanningMode) {
                RoutePlanningPopup(
                    viewModel = viewModel,
                    onDismiss = { viewModel.togglePlanningMode() }
                )
            }
        }
    }
}

