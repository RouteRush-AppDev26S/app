package com.example.appdevproject26s.social.sharing

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PinApiService {

    @POST("pins")
    suspend fun createPin(
        @Body request: CreatePinRequest
    ): Response<PinResponse>

    @GET("pins")
    suspend fun getMyPins(): Response<List<PinResponse>>

    @DELETE("pins/{id}")
    suspend fun deletePin(
        @Path("id") id: Long
    ): Response<Unit>
}