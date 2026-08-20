package com.example.appdevproject26s.stats

import android.util.Log
import androidx.compose.runtime.snapshotFlow
import com.example.appdevproject26s.auth.AuthRepository
import com.example.appdevproject26s.map.route.Navigate
import com.example.appdevproject26s.map.route.OrsTrackPoint
import com.example.appdevproject26s.map.route.Schrittzahler
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists movement data collected by the map tracking (Fundament's Navigate singleton,
 * consumed read-only via its observable state): every new track point is uploaded as a
 * location ping, and a finished tracking session is uploaded as a route.
 */
@Singleton
class AnalyticsRecorder @Inject constructor(
    private val navigate: Navigate,
    private val zahler: Schrittzahler,
    private val movementApi: MovementApi,
    private val authRepository: AuthRepository
) {

    private var started = false
    private var sessionActive = false
    private var sessionBaselineCount = 0
    private var uploadedCount = 0

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        scope.launch {
            snapshotFlow { navigate.trackingstart }
                .distinctUntilChanged()
                .collect { tracking ->
                    if (tracking) {
                        sessionActive = true
                        sessionBaselineCount = navigate.trackPoints.size
                        uploadedCount = navigate.trackPoints.size
                    } else if (sessionActive) {
                        sessionActive = false
                        finishSession()
                    }
                }
        }

        scope.launch {
            snapshotFlow { navigate.trackPoints.size }
                .distinctUntilChanged()
                .collect { size ->
                    if (sessionActive && size > uploadedCount) {
                        val newPoints = navigate.trackPoints.subList(uploadedCount, size).toList()
                        uploadedCount = size
                        newPoints.forEach { uploadPing(it) }
                    }
                }
        }
    }

    private suspend fun uploadPing(point: OrsTrackPoint) {
        if (!isLoggedIn()) return
        try {
            movementApi.reportPing(ReportPingRequest(lat = point.lat, lng = point.lon))
        } catch (e: Exception) {
            Log.w(TAG, "Ping-Upload fehlgeschlagen: ${e.message}")
        }
    }

    private suspend fun finishSession() {
        val points = navigate.trackPoints.drop(sessionBaselineCount)
        // Independent of navigate.distance, which Fundament's toggle resets on stop:
        // every track point carries its own hop distance in meters.
        val distanceMeters = points.sumOf { it.distanceToPrevious }
        if (points.isEmpty() || distanceMeters < 100.0) return
        if (!isLoggedIn()) return

        val first = points.first()
        val last = points.last()
        val waypointJson = Gson().toJson(points.map { listOf(it.lat, it.lon) })
        try {
            movementApi.createRoute(
                CreateRouteRequest(
                    type = "POINT_TO_POINT",
                    startLat = first.lat,
                    startLng = first.lon,
                    endLat = last.lat,
                    endLng = last.lon,
                    waypointJson = waypointJson,
                    distanceMeters = distanceMeters,
                    estimatedSteps = zahler.schritte
                )
            )
            Log.i(TAG, "Route hochgeladen: ${points.size} Punkte, ${distanceMeters.toInt()} m")
        } catch (e: Exception) {
            Log.w(TAG, "Routen-Upload fehlgeschlagen: ${e.message}")
        }
    }

    private suspend fun isLoggedIn(): Boolean = authRepository.isLoggedInFlow.first()

    companion object {
        private const val TAG = "AnalyticsRecorder"
    }
}
