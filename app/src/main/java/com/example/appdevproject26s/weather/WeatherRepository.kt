package com.example.appdevproject26s.weather

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val weatherApi: WeatherApi
) {

    var lastWeather: WeatherInfo? = null
        private set

    suspend fun fetchCurrent(latitude: Double, longitude: Double): Result<WeatherInfo> {
        return try {
            val response = weatherApi.getCurrentWeather(latitude, longitude)
            val info = response.toWeatherInfo(System.currentTimeMillis())
                ?: return Result.failure(IllegalStateException("Leere Wetterdaten"))
            lastWeather = info
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
