package com.example.appdevproject26s

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appdevproject26s.map.MapScreenViewModel
import java.util.Locale


@Composable
fun StatsPopup(
    viewModel: MapScreenViewModel,
    modifier: Modifier = Modifier
) {
    val speedLimit = viewModel.speedLimit
    val currentAddress = viewModel.currentAddress

    val totalSteps = viewModel.schritte
    val durationSeconds: Double = viewModel.durationSeconds
    val averageSpeedKmH = viewModel.averageSpeedKmH

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = currentAddress.ifBlank { "Unbekannter Ort" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Geschwindigkeit", value = String.format(Locale.getDefault(), "%.2f km/h", viewModel.speed))
                StatItem(label = "Limit", value = speedLimit?.let { "$it km/h" } ?: "N/A")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Ø Geschwindigkeit", value = String.format(Locale.getDefault(), "%.2f km/h", averageSpeedKmH))
                StatItem(label = "Schritte/Min", value = String.format(Locale.getDefault(), "%d", viewModel.stepsPerMinute))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Schritte", value = totalSteps.toString())
                // 🟢 HIER KORRIGIERT: %.2f anstatt %.2
                StatItem(label = "Distanz", value = String.format(Locale.getDefault(), "%.2f km", viewModel.distance))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Gesamtzeit", value = viewModel.formatDuration(durationSeconds))
            }
        }
    }
}
@Composable
fun StatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}
