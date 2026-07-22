package com.example.appdevproject26s.gamification.leaderboard

import retrofit2.http.GET

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val xp: Int,
    val level: Int
)

interface LeaderboardApi {
    @GET("leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>
}