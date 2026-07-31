package com.example.appdevproject26s.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherFeature(
    iconModifier: Modifier = Modifier,
    viewModel: WeatherViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val enabled = state !is WeatherUiState.Off

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AnimatedVisibility(visible = enabled) {
            WeatherCard(state)
        }
        IconButton(onClick = { viewModel.toggle() }, modifier = iconModifier) {
            Icon(
                imageVector = if (enabled) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                contentDescription = if (enabled) "Wetter ausblenden" else "Wetter anzeigen",
                tint = if (enabled) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun WeatherCard(state: WeatherUiState) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp,
        modifier = Modifier.widthIn(max = 240.dp)
    ) {
        when (state) {
            is WeatherUiState.Loading -> Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = "Wetter wird geladen…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            is WeatherUiState.Ready -> WeatherCardContent(info = state.info)

            is WeatherUiState.Error -> Column(modifier = Modifier.padding(bottom = 4.dp)) {
                if (state.cached != null) {
                    WeatherCardContent(info = state.cached)
                }
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            is WeatherUiState.Off -> Unit
        }
    }
}

@Composable
private fun WeatherCardContent(info: WeatherInfo) {
    val time = SimpleDateFormat("HH:mm", Locale.GERMANY).format(Date(info.fetchedAtMillis))
    Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = info.description,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
            Column {
                Text(
                    text = String.format(Locale.GERMANY, "%.1f °C", info.temperatureC),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (info.feelsLikeC != null) {
                    Text(
                        text = String.format(Locale.GERMANY, "Gefühlt %.1f °C", info.feelsLikeC),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = info.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = String.format(Locale.GERMANY, "Wind %.0f km/h · %s", info.windKmh, time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        if (info.hourlyRain.isNotEmpty()) {
            Column {
                Text(
                    text = "Regen nächste ${info.hourlyRain.size} Std.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    info.hourlyRain.forEach { hour ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = hour.hourLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = hour.probabilityPercent?.let { "$it%" } ?: "–",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
