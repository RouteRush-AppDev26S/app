package com.example.appdevproject26s.social.sharing

import jakarta.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepository @Inject constructor (
    private val pinApiService: PinApiService
) {
    suspend fun createPin(lat: Double, lng: Double, note: String?): Result<PinResponse> {
        return runCatching {
            val response = pinApiService.createPin(CreatePinRequest(lat, lng, note))
            response.body() ?: throw Exception("Failed to create pin: ${response.code()}")
        }
    }

    suspend fun getMyPins(): Result<List<PinResponse>> {
        return runCatching {
            val response = pinApiService.getMyPins()
            response.body() ?: emptyList()
        }
    }

    suspend fun deletePin(pinId: Long): Result<Unit> {
        return runCatching {
            val response = pinApiService.deletePin(pinId)
            if (!response.isSuccessful) {
                throw Exception("Failed to delete pin: ${response.code()}")
            }
        }
    }
}