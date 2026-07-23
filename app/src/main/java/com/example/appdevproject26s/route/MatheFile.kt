package com.example.appdevproject26s.tracking

import com.example.appdevproject26s.navigate.Location
import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

data class Coordinate(val lon: Double, val lat: Double) {
    fun toJsonArray() = listOf(lon, lat)
}

data class OrsTrackPoint(
    val lon: Double,
    val lat: Double,
    val timestamp: String,
    val distanceToPrevious: Double, // in Metern
    val kmh: Double,
    val steps: Int,
)

@Singleton
class MatheFile @Inject constructor() {

    var distanceSum: Double by mutableStateOf(0.0)
    fun haversineDistance(l1: Location, l2: Location): Double {
        return l1.distanceTo(l2).toDouble()
    }

    fun calculateKmhWithLocation(distanceMeters: Double, l1: Location, l2: Location): Double {
        val timeDeltaSeconds = (l2.time - l1.time) / 1000.0
        return if (timeDeltaSeconds <= 0) {
            0.0
        } else {
            (distanceMeters / timeDeltaSeconds) * 3.6
        }
    }

    fun addTrakkingData(
        trackPoints: List<OrsTrackPoint>,
        start: Location,
        stop: Location,
        schritte: Int,
        kmh: Double
    ): List<OrsTrackPoint> {
        val distance = haversineDistance(start, stop)
        if (distance < 100) return trackPoints
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val timestamp = sdf.format(Date(stop.time))

        val newPoint = OrsTrackPoint(
            lon = stop.lon,
            lat = stop.lat,
            timestamp = timestamp,
            distanceToPrevious = distance,
            kmh = kmh,
            steps = schritte
        )
        distanceSum+=distance;
        return trackPoints + newPoint
    }

    fun calculateAverageKmh(trackPoints: List<OrsTrackPoint>): Double {
        if (trackPoints.isEmpty()) return 0.0
        return trackPoints.map { it.kmh }.average()
    }

    fun calculateStepsPerMinute(trackPoints: List<OrsTrackPoint>): Double {
        if (trackPoints.size < 2) return 0.0
        
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        try {
            val firstTime = sdf.parse(trackPoints.first().timestamp)?.time ?: return 0.0
            val lastTime = sdf.parse(trackPoints.last().timestamp)?.time ?: return 0.0
            val durationMinutes = (lastTime - firstTime) / 60000.0
            
            if (durationMinutes <= 0) return 0.0
            
            val totalSteps = trackPoints.sumOf { it.steps }
            return totalSteps / durationMinutes
        } catch (e: Exception) {
            return 0.0
        }
    }

    fun calculateTotalDistanceKm(trackPoints: List<OrsTrackPoint>): Double {
        return trackPoints.sumOf { it.distanceToPrevious } / 1000.0
    }

    fun calculateTotalSteps(trackPoints: List<OrsTrackPoint>): Int {
        return trackPoints.sumOf { it.steps }
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }

    fun parseLocationHistoryToAndroidList(
        jsonString: String,
        minDistanceMeters: Float = 100.0f
    ): List<Location> {
        val jsonObject = JSONObject(jsonString)
        val locationsJson = jsonObject.optJSONArray("locations") ?: return emptyList()

        val processedLocations = mutableListOf<Location>()
        var lastSavedLocation: Location? = null
        val defaultSdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until locationsJson.length()) {
            val locJson = locationsJson.getJSONObject(i)
            if (locJson.has("latitudeE7") && locJson.has("longitudeE7")) {
                val lat = locJson.getLong("latitudeE7") / 10000000.0
                val lon = locJson.getLong("longitudeE7") / 10000000.0
                val timestampStr = locJson.optString("timestamp", defaultSdf.format(Date()))
                val steps = locJson.optInt("steps", 0)

                val timeMs = try {
                    defaultSdf.parse(timestampStr)?.time ?: System.currentTimeMillis()
                } catch (e: Exception) {
                    try {
                        val sdfShort = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        sdfShort.parse(timestampStr)?.time ?: System.currentTimeMillis()
                    } catch (e2: Exception) {
                        System.currentTimeMillis()
                    }
                }

                val currentLoc = Location(lat = lat, lon = lon).apply {
                    time = timeMs
                    extras = Bundle().apply { putInt("steps", steps) }
                }

                if (lastSavedLocation == null) {
                    currentLoc.speed = 0.0f
                    processedLocations.add(currentLoc)
                    lastSavedLocation = currentLoc
                } else {
                    val distance = lastSavedLocation.distanceTo(currentLoc)
                    if (distance >= minDistanceMeters) {
                        val timeDeltaSec = (currentLoc.time - lastSavedLocation.time) / 1000.0
                        if (timeDeltaSec > 0) {
                            val speedKmh = (distance / timeDeltaSec) * 3.6
                            currentLoc.speed = speedKmh.toFloat()
                        }
                        processedLocations.add(currentLoc)
                        lastSavedLocation = currentLoc
                    }
                }
            }
        }
        return processedLocations
    }

    fun printTrackMetadata(trackPoints: List<OrsTrackPoint>) {
        trackPoints.forEachIndexed { index, point ->
            println("Punkt $index | Zeit: ${point.timestamp} | +${point.distanceToPrevious}m | Tempo: ${point.kmh} km/h | Schritte: ${point.steps}")
        }
    }

}
