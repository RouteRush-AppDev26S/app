package com.example.appdevproject26s

import retrofit2.http.GET

data class WeeklyChallenge(
    val challengeType: String,
    val description: String,
    val target: Int,
    val weekStart: String,
    val currentProgress: Int,
    val completed: Boolean
)

interface ChallengeApi {
    @GET("challenges/current")
    suspend fun getCurrentChallenge(): WeeklyChallenge
}