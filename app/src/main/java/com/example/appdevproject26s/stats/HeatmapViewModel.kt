package com.example.appdevproject26s.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

enum class HeatmapRange(val days: Long?, val label: String) {
    WEEK(7, "7 Tage"),
    MONTH(30, "30 Tage"),
    ALL(null, "Alles")
}

/** One recorded route path as (lat, lng) pairs. */
data class RoutePath(val points: List<Pair<Double, Double>>)

sealed interface HeatmapUiState {
    data object Loading : HeatmapUiState
    data class Ready(
        val pings: List<LocationPingResponse>,
        val paths: List<RoutePath>
    ) : HeatmapUiState

    data class Error(val message: String) : HeatmapUiState
}

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val movementApi: MovementApi,
    authRepo: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _range = MutableStateFlow(HeatmapRange.MONTH)
    val range = _range.asStateFlow()

    private val _uiState = MutableStateFlow<HeatmapUiState>(HeatmapUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun setRange(value: HeatmapRange) {
        _range.value = value
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = HeatmapUiState.Loading
            val from = _range.value.days
                ?.let { Instant.now().minus(Duration.ofDays(it)) }
                ?: Instant.EPOCH
            _uiState.value = try {
                val pings = movementApi.getPings(from.toString())
                val paths = movementApi.getRoutes()
                    .filter { route ->
                        val createdAt = route.createdAt?.let {
                            runCatching { Instant.parse(it) }.getOrNull()
                        }
                        createdAt == null || !createdAt.isBefore(from)
                    }
                    .mapNotNull { parseWaypoints(it.waypointJson) }
                HeatmapUiState.Ready(pings, paths)
            } catch (e: Exception) {
                HeatmapUiState.Error(e.message ?: "Heatmap konnte nicht geladen werden")
            }
        }
    }

    /** waypointJson format: [[lat, lng], [lat, lng], ...] (written by AnalyticsRecorder). */
    private fun parseWaypoints(json: String?): RoutePath? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val raw = Gson().fromJson(json, Array<DoubleArray>::class.java)
            val points = raw.filter { it.size >= 2 }.map { it[0] to it[1] }
            if (points.size >= 2) RoutePath(points) else null
        }.getOrNull()
    }
}
