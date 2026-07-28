package com.example.appdevproject26s.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.goalDataStore by preferencesDataStore(name = "goal_settings")

data class StepGoals(
    val dailySteps: Int,
    val weeklySteps: Int
)

@Singleton
class GoalsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val dailyKey = intPreferencesKey("daily_step_goal")
    private val weeklyKey = intPreferencesKey("weekly_step_goal")

    val goalsFlow: Flow<StepGoals> = context.goalDataStore.data.map { prefs ->
        StepGoals(
            dailySteps = prefs[dailyKey] ?: DEFAULT_DAILY,
            weeklySteps = prefs[weeklyKey] ?: DEFAULT_WEEKLY
        )
    }

    suspend fun setGoals(dailySteps: Int, weeklySteps: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[dailyKey] = dailySteps.coerceAtLeast(1)
            prefs[weeklyKey] = weeklySteps.coerceAtLeast(1)
        }
    }

    companion object {
        const val DEFAULT_DAILY = 10_000
        const val DEFAULT_WEEKLY = 70_000
    }
}
