package com.example.appdevproject26s.gamification.achievements

import retrofit2.http.GET

data class Achievement(
    val achievementType: String,
    val description: String,
    val target: Int,
    val currentProgress: Int,
    val unlocked: Boolean,
    val unlockedAt: String?
)

interface AchievementApi {
    @GET("achievements")
    suspend fun getAchievements(): List<Achievement>
}