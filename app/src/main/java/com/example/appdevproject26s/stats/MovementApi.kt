package com.example.appdevproject26s.stats

import retrofit2.http.Body
import retrofit2.http.POST

data class ReportPingRequest(val lat: Double, val lng: Double)

data class CreateRouteRequest(
    val type: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val waypointJson: String,
    val distanceMeters: Double,
    val estimatedSteps: Int
)

data class LocationPingResponse(
    val id: Long,
    val lat: Double,
    val lng: Double,
    val recordedAt: String
)

data class RouteDto(
    val id: Long,
    val waypointJson: String?,
    val createdAt: String?
)

interface MovementApi {

    @POST("location-pings")
    suspend fun reportPing(@Body request: ReportPingRequest): retrofit2.Response<Unit>

    @POST("routes")
    suspend fun createRoute(@Body request: CreateRouteRequest): retrofit2.Response<Unit>

    @retrofit2.http.GET("location-pings")
    suspend fun getPings(@retrofit2.http.Query("from") from: String): List<LocationPingResponse>

    @retrofit2.http.GET("routes")
    suspend fun getRoutes(): List<RouteDto>
}
