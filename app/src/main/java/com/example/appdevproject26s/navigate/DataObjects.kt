package com.example.appdevproject26s.navigate

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ValhallaApi {
    @POST("route")
    suspend fun getRoute(@Body request: ValhallaRequest): Response<ValhallaResponse>
}

object ValhallaClient {
    private const val BASE_URL = "https://valhalla1.openstreetmap.de/"

    val api: ValhallaApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ValhallaApi::class.java)
}

data class ValhallaRequest(
    val locations: List<VLocation>,
    val costing: String = "auto",
    val costing_options: CostingOptions? = null,
    val directions_options: DirectionsOptions = DirectionsOptions(),
    val filters: Filters = Filters()
)

data class VLocation(
    val lon: Double,
    val lat: Double,
    val type: String = "break"   // break = Stopp, through = Durchfahrt
)

data class CostingOptions(
    val auto: AutoOptions? = null,
    val pedestrian: PedestrianOptions? = null
)

data class AutoOptions(
    val use_highways: Float = 1.0f,   // 0=meiden, 1=bevorzugen
    val use_tolls: Float = 0.5f,
    val top_speed: Int = 130          // km/h max
)

data class PedestrianOptions(
    val walking_speed: Float = 5.0f
)

data class DirectionsOptions(
    val language: String = "de-DE",
    val units: String = "kilometers"
)

data class Filters(
    val attributes: List<String> = listOf(
        "edge.speed",
        "edge.speed_limit",
        "edge.surface",
        "edge.road_class",
        "node.elapsed_time"
    ),
    val action: String = "include"
)

// ---- Response ----

data class ValhallaResponse(
    val trip: Trip
)

data class Trip(
    val legs: List<Leg>,
    val summary: TripSummary,
    val status_message: String
)

data class TripSummary(
    val length: Double,
    val time: Double,
    val min_lat: Double,
    val min_lon: Double,
    val max_lat: Double,
    val max_lon: Double
)

data class Leg(
    val shape: String,
    val maneuvers: List<Maneuver>,
    val summary: LegSummary
)

data class LegSummary(
    val length: Double,
    val time: Double
)

data class Maneuver(
    val type: Int,
    val instruction: String,
    val verbal_pre_transition_instruction: String?,
    val verbal_transition_alert_instruction: String?,
    val street_names: List<String>?,
    val length: Double,
    val time: Double,
    val begin_shape_index: Int,
    val end_shape_index: Int,
    val speed_limit: Int?,          // km/h – kann null sein
    val travel_mode: String         // "drive", "pedestrian" etc.
)