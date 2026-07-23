package com.example.appdevproject26s.route

import com.example.appdevproject26s.user.UserProfileResponse

data class Route(
    val id: Long?,
    val user: UserProfileResponse?,
    val type: String?,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val waypointJson: String?,
    val distanceMeters: Double,
    val estimatedSteps: Int,
    val createdAt: String?
)

data class Pin(
    val id: Long?,
    val user: UserProfileResponse?,
    val lat: Double,
    val lng: Double,
    val note: String?,
    val createdAt: String?
)