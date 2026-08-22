package com.example.appdevproject26s.map

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.StatsPopup
import com.example.appdevproject26s.map.route.RoutePlanningPopup
import com.example.appdevproject26s.social.sharing.PinSharingDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavController) {
    val viewModel: MapScreenViewModel = hiltViewModel()
    val isPlanningMode by viewModel.isPlanningMode.collectAsState()
    val isSelectingDestination by viewModel.isSelectingDestination.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val showBottomSheet by viewModel.showBottomSheet.collectAsState()
    val showSharePinDialog by viewModel.showSharePinDialog.collectAsState()
    val selectedMapPin by viewModel.selectedMapPin.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (locationGranted) {
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
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        permissionLauncher.launch(permissions.toTypedArray())
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
                    onClick = { viewModel.toggletracking() },
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
            if(viewModel.trackingstart){
                StatsPopup(
                    viewModel = viewModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                )
            }

            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { viewModel.dismissBottomSheet() },
                    sheetState = rememberModalBottomSheetState()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        ActionButton(
                            icon = Icons.Default.Navigation,
                            label = "Navigate to here",
                            onClick = viewModel::navigateToSelected,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            icon = Icons.Default.Place,
                            label = "Navigate from here",
                            onClick = viewModel::navigateFromSelected,
                            modifier = Modifier.weight(1f)
                        )
                        ActionButton(
                            icon = Icons.Default.Share,
                            label = "Share",
                            onClick = viewModel::openSharePinDialog,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (showSharePinDialog) {
                PinSharingDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.dismissShareDialog() },
                )
            }

            selectedMapPin?.let { pin ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (pin.isMine) "My pin" else "Shared pin",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                pin.note?.let {
                                    Text(text = it, style = MaterialTheme.typography.bodyLarge)
                                } ?: Text(
                                    text = "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }

                            Row {
                                if (pin.isMine) {
                                    IconButton(onClick = { viewModel.deleteSelectedPin() }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.dismissPinPopup() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
