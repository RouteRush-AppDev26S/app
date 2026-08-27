package com.example.appdevproject26s.profile

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApiService {

    @GET("users/me")
    suspend fun getCurrentUser(): UserProfileResponse

    @PUT("users/me")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): UserProfileResponse
}

// Data models matching your backend records/responses
data class UserProfileResponse(
    val id: Long,
    val email: String,
    val username: String,
    val xp: Int,
    val level: Int,
    val admin: Boolean,
    val xpIntoLevel: Int,
    val xpForNextLevel: Int
)

data class UpdateProfileRequest(
    val username: String?,
    val email: String?
)