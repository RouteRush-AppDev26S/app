package com.example.appdevproject26s.weather

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    @SerializedName("current") val current: CurrentWeatherDto?,
    @SerializedName("hourly") val hourly: HourlyDto?
)

data class CurrentWeatherDto(
    @SerializedName("time") val time: String?,
    @SerializedName("temperature_2m") val temperature: Double?,
    @SerializedName("apparent_temperature") val apparentTemperature: Double?,
    @SerializedName("weather_code") val weatherCode: Int?,
    @SerializedName("wind_speed_10m") val windSpeed: Double?,
    @SerializedName("is_day") val isDay: Int?
)

data class HourlyDto(
    @SerializedName("time") val time: List<String>?,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>?
)

data class HourRain(
    val hourLabel: String,
    val probabilityPercent: Int?
)

data class WeatherInfo(
    val temperatureC: Double,
    val feelsLikeC: Double?,
    val windKmh: Double,
    val description: String,
    val icon: ImageVector,
    val hourlyRain: List<HourRain>,
    val fetchedAtMillis: Long
)

fun OpenMeteoResponse.toWeatherInfo(fetchedAtMillis: Long): WeatherInfo? {
    val currentDto = current ?: return null
    val temp = currentDto.temperature ?: return null
    val code = currentDto.weatherCode ?: return null

    val times = hourly?.time.orEmpty()
    val probabilities = hourly?.precipitationProbability.orEmpty()
    val hourlyRain = times.zip(probabilities) { time, probability ->
        // Open-Meteo hour format: "2026-07-28T14:00" -> label "14"
        HourRain(hourLabel = time.substringAfter('T').take(2), probabilityPercent = probability)
    }

    return WeatherInfo(
        temperatureC = temp,
        feelsLikeC = currentDto.apparentTemperature,
        windKmh = currentDto.windSpeed ?: 0.0,
        description = wmoDescription(code),
        icon = wmoIcon(code, currentDto.isDay != 0),
        hourlyRain = hourlyRain,
        fetchedAtMillis = fetchedAtMillis
    )
}

fun wmoDescription(code: Int): String = when (code) {
    0 -> "Klar"
    1 -> "Überwiegend klar"
    2 -> "Teilweise bewölkt"
    3 -> "Bedeckt"
    45, 48 -> "Nebel"
    51, 53, 55, 56, 57 -> "Nieselregen"
    61, 63, 65, 66, 67 -> "Regen"
    71, 73, 75, 77 -> "Schneefall"
    80, 81, 82 -> "Regenschauer"
    85, 86 -> "Schneeschauer"
    95, 96, 99 -> "Gewitter"
    else -> "Unbekannt"
}

fun wmoIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) Icons.Default.WbSunny else Icons.Default.NightsStay
    2 -> Icons.Default.WbCloudy
    3 -> Icons.Default.Cloud
    45, 48 -> Icons.Default.Dehaze
    51, 53, 55, 56, 57 -> Icons.Default.Grain
    61, 63, 65, 66, 67 -> Icons.Default.Opacity
    71, 73, 75, 77, 85, 86 -> Icons.Default.AcUnit
    80, 81, 82 -> Icons.Default.Grain
    95, 96, 99 -> Icons.Default.Bolt
    else -> Icons.Default.Cloud
}
