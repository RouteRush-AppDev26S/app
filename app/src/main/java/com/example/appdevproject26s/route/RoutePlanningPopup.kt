package com.example.appdevproject26s.route

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutePlanningPopup(
    viewModel: MapScreenViewModel,
    onDismiss: () -> Unit,
) {
    val isRoundTrip by viewModel.isRoundTrip.collectAsState()
    val startAddress by viewModel.startAddressInput.collectAsState()
    val destinations by viewModel.destinations.collectAsState()
    val selectedVehicle by viewModel.selectedVehicle.collectAsState()
    val vehicleOptions = viewModel.vehicleOptions

    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Routenplanung",
                    style = MaterialTheme.typography.titleLarge
                )

                // Vehicle Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vehicleOptions.find { it.second == selectedVehicle }?.first ?: "Auto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Verkehrsmittel") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        vehicleOptions.forEach { (label, value) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    viewModel.setSelectedVehicle(value)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRoundTrip, onCheckedChange = { viewModel.setRoundTrip(it) })
                    Text("Rundreise")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var avoidHighways by remember { mutableStateOf(false) }
                    Checkbox(checked = avoidHighways, onCheckedChange = { 
                        avoidHighways = it
                        viewModel.setAvoidHighways(it) 
                    })
                    Text("Autobahnen meiden")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    var avoidTolls by remember { mutableStateOf(false) }
                    Checkbox(checked = avoidTolls, onCheckedChange = { 
                        avoidTolls = it
                        viewModel.setAvoidTolls(it) 
                    })
                    Text("Keine Mautstraßen")
                }

                // Start
                OutlinedTextField(
                    value = startAddress,
                    onValueChange = { viewModel.updateStartAddress(it) },
                    label = { Text("Start") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Destinations (Scrollable if many)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    destinations.forEachIndexed { index, dest ->
                        OutlinedTextField(
                            value = dest,
                            onValueChange = { viewModel.updateDestinationAddress(index, it) },
                            label = { Text(if (index == 0) "Ziel" else "Zwischenziel $index") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.startSelectingDestination() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Ziel hinzufügen")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { viewModel.calculateRouteFromPlanning() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Route berechnen")
                }
            }
        }
    }
}
