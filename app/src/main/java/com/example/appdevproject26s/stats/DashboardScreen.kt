package com.example.appdevproject26s.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.appdevproject26s.R
import com.example.appdevproject26s.ScreenScaffold
import com.example.appdevproject26s.social.LoginPrompt
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) viewModel.load()
    }

    ScreenScaffold(navController = navController, title = stringResource(R.string.stats_title)) {
        if (!isLoggedIn) {
            LoginPrompt(
                feature = "your statistics",
                onNavigateToLogin = { navController.navigate("profile") }
            )
        } else {
            when (val state = uiState) {
                is DashboardUiState.Loading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

                is DashboardUiState.Error -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { viewModel.load() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Erneut versuchen")
                    }
                }

                is DashboardUiState.Ready -> {
                    val timeframe by viewModel.timeframe.collectAsState()
                    val goals by viewModel.goals.collectAsState()
                    DashboardContent(
                        summary = state.summary,
                        comparison = state.comparison,
                        trend = state.trend,
                        timeframe = timeframe,
                        onTimeframeChange = viewModel::setTimeframe,
                        goals = goals,
                        onSaveGoals = viewModel::saveGoals
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    summary: StatsSummary,
    comparison: StatsComparison,
    trend: List<TrendPoint>,
    timeframe: Timeframe,
    onTimeframeChange: (Timeframe) -> Unit,
    goals: StepGoals,
    onSaveGoals: (Int, Int) -> Unit
) {
    val activity = activityStats(trend, timeframe)
    val streaks = streakStats(trend)
    val progress = goalProgress(trend)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActivityCard(activity, timeframe, onTimeframeChange)
        GoalsCard(goals, progress, onSaveGoals)
        StreakCard(streaks)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Schritte gesamt", formatInt(summary.totalSteps), Modifier.weight(1f))
            SummaryCard("Distanz gesamt", formatKm(summary.totalDistanceKm), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Routen", formatInt(summary.totalRoutes), Modifier.weight(1f))
            SummaryCard("Level ${summary.level}", "${formatInt(summary.xp)} XP", Modifier.weight(1f))
        }

        ComparisonCard(
            title = "Diese Woche vs. letzte Woche",
            stepsNow = comparison.thisWeekSteps,
            stepsBefore = comparison.lastWeekSteps,
            distanceNow = comparison.thisWeekDistanceKm,
            distanceBefore = comparison.lastWeekDistanceKm
        )
        ComparisonCard(
            title = "Dieser Monat vs. letzter Monat",
            stepsNow = comparison.thisMonthSteps,
            stepsBefore = comparison.lastMonthSteps,
            distanceNow = comparison.thisMonthDistanceKm,
            distanceBefore = comparison.lastMonthDistanceKm
        )

        CaloriesCard(activity, timeframe)
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityStats,
    timeframe: Timeframe,
    onTimeframeChange: (Timeframe) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Schritte pro Tag",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                Timeframe.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = timeframe == entry,
                        onClick = { onTimeframeChange(entry) },
                        shape = SegmentedButtonDefaults.itemShape(index, Timeframe.entries.size)
                    ) {
                        Text(entry.label)
                    }
                }
            }
            if (activity.totalSteps == 0) {
                Text(
                    text = "Noch keine Schrittdaten im Zeitraum.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StepsBarChart(
                values = activity.points.map { it.steps },
                xLabels = dailyXLabels(activity.points, timeframe),
                averageValue = activity.averageStepsPerDay
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabeledValue("Ø pro Tag", formatInt(activity.averageStepsPerDay))
                LabeledValue(
                    label = activity.bestDayDate?.let { "Bester Tag (${formatShortDate(it)})" }
                        ?: "Bester Tag",
                    value = formatInt(activity.bestDaySteps)
                )
            }
        }
    }
}

@Composable
private fun GoalsCard(goals: StepGoals, progress: GoalProgress, onSaveGoals: (Int, Int) -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ziele",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Ziele bearbeiten",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            GoalProgressRow("Heute", progress.todaySteps, goals.dailySteps)
            GoalProgressRow("Diese Woche", progress.weekSteps, goals.weeklySteps)
        }
    }

    if (showEditDialog) {
        GoalsEditDialog(
            goals = goals,
            onDismiss = { showEditDialog = false },
            onSave = { daily, weekly ->
                onSaveGoals(daily, weekly)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun GoalProgressRow(label: String, current: Int, goal: Int) {
    val fraction = if (goal > 0) (current.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val percent = if (goal > 0) (current * 100L / goal).toInt() else 0
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${formatInt(current)} / ${formatInt(goal)} ($percent %)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun GoalsEditDialog(
    goals: StepGoals,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    var dailyInput by remember { mutableStateOf(goals.dailySteps.toString()) }
    var weeklyInput by remember { mutableStateOf(goals.weeklySteps.toString()) }
    val daily = dailyInput.toIntOrNull()
    val weekly = weeklyInput.toIntOrNull()
    val valid = daily != null && daily > 0 && weekly != null && weekly > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ziele bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dailyInput,
                    onValueChange = { dailyInput = it.filter(Char::isDigit) },
                    label = { Text("Tagesziel (Schritte)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weeklyInput,
                    onValueChange = { weeklyInput = it.filter(Char::isDigit) },
                    label = { Text("Wochenziel (Schritte)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(daily ?: 0, weekly ?: 0) }
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
private fun StreakCard(streaks: StreakStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Serie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LabeledValue("Aktuelle Serie", "${streaks.currentStreakDays} Tage")
                LabeledValue("Längste Serie (90 Tage)", "${streaks.longestStreakDays} Tage")
            }
        }
    }
}

@Composable
private fun CaloriesCard(activity: ActivityStats, timeframe: Timeframe) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Kalorien (${timeframe.label})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            LabeledValue("Verbrannt", "≈ ${formatInt(activity.estimatedKcal)} kcal")
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun dailyXLabels(points: List<TrendPoint>, timeframe: Timeframe): List<Pair<Int, String>> {
    if (points.isEmpty()) return emptyList()
    return when (timeframe) {
        Timeframe.WEEK -> points.mapIndexedNotNull { index, point ->
            point.localDate()?.let { index to WEEKDAY_LABELS[it.dayOfWeek.value - 1] }
        }

        else -> listOf(0, points.size / 2, points.lastIndex).distinct().mapNotNull { index ->
            points[index].localDate()?.let { index to formatShortDate(it) }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComparisonCard(
    title: String,
    stepsNow: Int,
    stepsBefore: Int,
    distanceNow: Double,
    distanceBefore: Double
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            MetricRow("Schritte", formatInt(stepsNow), formatInt(stepsBefore), delta(stepsNow.toDouble(), stepsBefore.toDouble()))
            HorizontalDivider()
            MetricRow("Distanz", formatKm(distanceNow), formatKm(distanceBefore), delta(distanceNow, distanceBefore))
        }
    }
}

@Composable
private fun MetricRow(label: String, now: String, before: String, deltaText: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "vorher: $before",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = now,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = deltaText ?: "–",
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    deltaText == null -> MaterialTheme.colorScheme.onSurfaceVariant
                    deltaText.startsWith("▲") -> MaterialTheme.colorScheme.primary
                    deltaText.startsWith("▼") -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

private fun formatInt(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)

private fun formatKm(value: Double): String =
    String.format(Locale.GERMANY, "%.1f km", value)

private fun delta(now: Double, before: Double): String? {
    if (before <= 0.0) return null
    val percent = (now - before) / before * 100.0
    return when {
        percent > 0.0 -> String.format(Locale.GERMANY, "▲ +%.0f %%", percent)
        percent < 0.0 -> String.format(Locale.GERMANY, "▼ %.0f %%", percent)
        else -> "±0 %"
    }
}
