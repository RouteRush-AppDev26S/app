package com.example.appdevproject26s.steps

import retrofit2.http.Body
import retrofit2.http.POST

data class DailyStepsResponse(val id: Long, val date: String, val steps: Int)

data class ReportStepsRequest(val date: String, val steps: Int)

interface StepsApi {
    @POST("steps")
    suspend fun report(@Body request: ReportStepsRequest): DailyStepsResponse
}