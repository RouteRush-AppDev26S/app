package com.example.appdevproject26s.stats

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StatsRepository @Inject constructor(
    private val statsApi: StatsApi
) {

    suspend fun loadSummary(): Result<StatsSummary> =
        runCatching { statsApi.getSummary() }

    suspend fun loadComparison(): Result<StatsComparison> =
        runCatching { statsApi.getComparison() }

    suspend fun loadTrend(days: Int): Result<List<TrendPoint>> =
        runCatching { statsApi.getTrend(days) }
}
