package com.example.appdevproject26s.weather

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,apparent_temperature,weather_code,wind_speed_10m,is_day",
        @Query("hourly") hourly: String = "precipitation_probability",
        @Query("forecast_hours") forecastHours: Int = 6,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoResponse
}
