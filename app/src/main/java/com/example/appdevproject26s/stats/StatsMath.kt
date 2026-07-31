package com.example.appdevproject26s.stats

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Timeframe(val days: Int, val label: String) {
    WEEK(7, "7 Tage"),
    MONTH(30, "30 Tage"),
    QUARTER(90, "90 Tage")
}

data class ActivityStats(
    val points: List<TrendPoint>,
    val averageStepsPerDay: Int,
    val bestDaySteps: Int,
    val bestDayDate: LocalDate?,
    val totalSteps: Int,
    val totalDistanceKm: Double,
    val estimatedKcal: Int
)

data class StreakStats(
    val currentStreakDays: Int,
    val longestStreakDays: Int
)

private const val KCAL_PER_STEP = 0.04

fun TrendPoint.localDate(): LocalDate? =
    runCatching { LocalDate.parse(date) }.getOrNull()

/** Statistics for the selected timeframe window (the last [Timeframe.days] entries). */
fun activityStats(fullTrend: List<TrendPoint>, timeframe: Timeframe): ActivityStats {
    val points = fullTrend.takeLast(timeframe.days)
    val totalSteps = points.sumOf { it.steps }
    val totalDistanceKm = points.sumOf { it.distanceKm }
    val best = points.maxByOrNull { it.steps }

    return ActivityStats(
        points = points,
        averageStepsPerDay = if (points.isEmpty()) 0 else totalSteps / points.size,
        bestDaySteps = best?.steps ?: 0,
        bestDayDate = best?.takeIf { it.steps > 0 }?.localDate(),
        totalSteps = totalSteps,
        totalDistanceKm = totalDistanceKm,
        estimatedKcal = (totalSteps * KCAL_PER_STEP).toInt()
    )
}

/** Streaks over the full loaded history; a day counts as active with steps > 0. */
fun streakStats(fullTrend: List<TrendPoint>): StreakStats {
    val active = fullTrend.map { it.steps > 0 }

    var longest = 0
    var run = 0
    for (isActive in active) {
        run = if (isActive) run + 1 else 0
        if (run > longest) longest = run
    }

    // Current streak counts back from today; an inactive today (morning) falls back to yesterday.
    var index = active.lastIndex
    if (index >= 0 && !active[index]) index--
    var current = 0
    while (index >= 0 && active[index]) {
        current++
        index--
    }

    return StreakStats(currentStreakDays = current, longestStreakDays = longest)
}

data class GoalProgress(
    val todaySteps: Int,
    val weekSteps: Int
)

/** Today's steps and the running Monday-to-today sum, based on the newest trend entry. */
fun goalProgress(fullTrend: List<TrendPoint>): GoalProgress {
    val last = fullTrend.lastOrNull() ?: return GoalProgress(0, 0)
    val lastDate = last.localDate() ?: return GoalProgress(last.steps, last.steps)
    val monday = lastDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    val weekSteps = fullTrend
        .filter { point -> point.localDate()?.let { !it.isBefore(monday) && !it.isAfter(lastDate) } == true }
        .sumOf { it.steps }
    return GoalProgress(todaySteps = last.steps, weekSteps = weekSteps)
}

val WEEKDAY_LABELS = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")

fun formatShortDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("d. MMM", Locale.GERMAN))
