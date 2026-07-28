package com.example.appdevproject26s.weather

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

sealed interface WeatherUiState {
    data object Off : WeatherUiState
    data object Loading : WeatherUiState
    data class Ready(val info: WeatherInfo) : WeatherUiState
    data class Error(val message: String, val cached: WeatherInfo?) : WeatherUiState
}

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Off)
    val uiState = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun toggle() {
        if (refreshJob != null) {
            refreshJob?.cancel()
            refreshJob = null
            _uiState.value = WeatherUiState.Off
        } else {
            refreshJob = viewModelScope.launch {
                if (repository.lastWeather == null) {
                    _uiState.value = WeatherUiState.Loading
                }
                while (true) {
                    refreshOnce()
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    private suspend fun refreshOnce() {
        val location = awaitCurrentLocation()
        if (location == null) {
            _uiState.value = WeatherUiState.Error("Standort nicht verfügbar", repository.lastWeather)
            return
        }
        repository.fetchCurrent(location.latitude, location.longitude)
            .onSuccess { _uiState.value = WeatherUiState.Ready(it) }
            .onFailure {
                _uiState.value =
                    WeatherUiState.Error("Wetter konnte nicht geladen werden", repository.lastWeather)
            }
    }

    private suspend fun awaitCurrentLocation(): Location? {
        return try {
            suspendCancellableCoroutine { continuation ->
                val cancellationSource = CancellationTokenSource()
                continuation.invokeOnCancellation { cancellationSource.cancel() }
                locationClient
                    .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationSource.token)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) continuation.resume(location)
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15L * 60L * 1000L
    }
}
