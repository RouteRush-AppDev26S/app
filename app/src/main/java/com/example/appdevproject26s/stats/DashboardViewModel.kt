package com.example.appdevproject26s.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appdevproject26s.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val summary: StatsSummary,
        val comparison: StatsComparison,
        val trend: List<TrendPoint>
    ) : DashboardUiState

    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: StatsRepository,
    private val goalsRepository: GoalsRepository,
    authRepo: AuthRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedInFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val goals: StateFlow<StepGoals> = goalsRepository.goalsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StepGoals(GoalsRepository.DEFAULT_DAILY, GoalsRepository.DEFAULT_WEEKLY)
        )

    fun saveGoals(dailySteps: Int, weeklySteps: Int) {
        viewModelScope.launch {
            goalsRepository.setGoals(dailySteps, weeklySteps)
        }
    }

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _timeframe = MutableStateFlow(Timeframe.WEEK)
    val timeframe = _timeframe.asStateFlow()

    fun setTimeframe(value: Timeframe) {
        _timeframe.value = value
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            val summaryDeferred = async { repository.loadSummary() }
            val comparisonDeferred = async { repository.loadComparison() }
            val trendDeferred = async { repository.loadTrend(Timeframe.QUARTER.days) }
            val summary = summaryDeferred.await()
            val comparison = comparisonDeferred.await()
            val trend = trendDeferred.await()

            _uiState.value = when {
                summary.isSuccess && comparison.isSuccess && trend.isSuccess ->
                    DashboardUiState.Ready(
                        summary.getOrThrow(),
                        comparison.getOrThrow(),
                        trend.getOrThrow()
                    )

                else -> DashboardUiState.Error(
                    (summary.exceptionOrNull() ?: comparison.exceptionOrNull()
                        ?: trend.exceptionOrNull())?.message
                        ?: "Statistiken konnten nicht geladen werden"
                )
            }
        }
    }
}
