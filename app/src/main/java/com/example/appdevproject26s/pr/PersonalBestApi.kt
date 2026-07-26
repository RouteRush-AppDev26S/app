package com.example.appdevproject26s.pr

import retrofit2.http.GET

data class PersonalBest(
    val type: String,
    val description: String,
    val value: Double,
    val achievedAt: String?
)

interface PersonalBestApi {
    @GET("personal-bests")
    suspend fun getPersonalBests(): List<PersonalBest>
}