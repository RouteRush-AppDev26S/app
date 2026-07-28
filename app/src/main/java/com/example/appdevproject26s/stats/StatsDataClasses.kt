package com.example.appdevproject26s.stats

data class StatsSummary(
    val totalSteps: Int,
    val totalDistanceKm: Double,
    val totalRoutes: Int,
    val level: Int,
    val xp: Int
)

data class TrendPoint(
    val date: String,
    val steps: Int,
    val distanceKm: Double
)

data class StatsComparison(
    val thisWeekSteps: Int,
    val lastWeekSteps: Int,
    val thisWeekDistanceKm: Double,
    val lastWeekDistanceKm: Double,
    val thisMonthSteps: Int,
    val lastMonthSteps: Int,
    val thisMonthDistanceKm: Double,
    val lastMonthDistanceKm: Double
)
