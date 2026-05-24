package com.example.appdevproject26s.navigate

import android.content.Context
import org.maplibre.android.geometry.LatLng
import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.INavigate
/*
*                    Written by Hans Wornik
*           Implementation of the Navigate Interface
 */
import android.util.Log

object Navigate : INavigate{
    private const val TAG = "Navigate"

    private var db: RouteDao? = null

    var currentTrip: Trip? = null
        private set

    override var routePoints: List<Position> = emptyList()
        private set

    override var totalLengthKM: Double = 0.0
        private set

    override var durationSeconds: Long = 0
        private set

    fun init(context: Context) {
        db = RouteDatabase.getDatabase(context).routeDao()
    }

    override suspend fun calcRoute(start: VLocation, stop: VLocation, vehicle: String): Trip? {
        val request = OrsRequest(
            coordinates = listOf(listOf(start.lon, start.lat), listOf(stop.lon, stop.lat))
        )

        val response = try {
            OrsClient.api.getRoute(OrsClient.API_KEY, request)
        } catch (e: Exception) {
            Log.e(TAG, "ORS API Exception: ${e.message}", e)
            null
        }

        if (response == null || !response.isSuccessful) {
            val errorMsg = "ORS API Error: ${response?.code()} ${response?.message()}"
            Log.e(TAG, errorMsg)
            val errorBody = response?.errorBody()?.string()
            Log.e(TAG, "ORS Error Body: $errorBody")
            return null
        }

        val body = response.body()
        Log.d(TAG, "ORS API Success. Body: $body")
        val route = body?.routes?.firstOrNull()

        if (route == null) {
            Log.e(TAG, "ORS Route is NULL")
            return null
        }

        val trip = Trip(
            summary = route.summary,
            segments = route.segments,
            extras = route.extras
        )

        currentTrip = trip
        totalLengthKM = trip.summary.distance
        durationSeconds = trip.summary.duration.toLong()

        routePoints = route.toLatLngList().map { Position(it.longitude, it.latitude) }

        db?.insertRoute(
            RouteEntity(
                timestamp = System.currentTimeMillis(),
                startName = "${start.lat}, ${start.lon}",
                destinationName = "${stop.lat}, ${stop.lon}",
                tripJson = RouteConverter.tripToJson(trip),
                routePointsJson = RouteConverter.pointsToJson(routePoints)
            )
        )

        return trip
    }

    override suspend fun loadLastRoute(): Trip? {
        val last = db?.getLastRoute() ?: return null
        val trip = RouteConverter.jsonToTrip(last.tripJson)
        currentTrip = trip
        totalLengthKM = trip.summary.distance
        durationSeconds = trip.summary.duration.toLong()
        routePoints = RouteConverter.jsonToPoints(last.routePointsJson)
        return trip
    }

    suspend fun getHistory(): List<RouteEntity> = db?.getAllRoutes() ?: emptyList()
    suspend fun clearAllHistory() { db?.clearHistory() }
    suspend fun deleteRouteFromHistory(routeId: Int) { db?.deleteRouteById(routeId) }

    override suspend fun getSpeedLimit(now: VLocation): Int? {
        val query = "[out:json];way(around:50,${now.lat},${now.lon})[highway];out tags;"
        Log.d(TAG, "Overpass query: $query")
        return try {
            val response = OverpassClient.api.query(query)
            Log.d(TAG, "Overpass HTTP ${response.code()}")
            if (response.isSuccessful) {
                val elements = response.body()?.elements ?: run {
                    Log.e(TAG, "Overpass body null")
                    return null
                }
                Log.d(TAG, "Overpass elements: ${elements.size}")
                elements.forEach { el ->
                    Log.d(TAG, "  way ${el.id}: highway=${el.tags?.get("highway")} maxspeed=${el.tags?.get("maxspeed")}")
                }
                val explicit = elements
                    .mapNotNull { it.tags?.get("maxspeed") }
                    .firstNotNullOfOrNull { parseMaxspeed(it) }
                val hw = elements.firstNotNullOfOrNull { it.tags?.get("highway") }
                val result = explicit ?: defaultSpeedForHighway(hw)
                Log.d(TAG, "SpeedLimit result: $result (explicit=$explicit, highway=$hw)")
                result
            } else {
                Log.e(TAG, "Overpass Error: ${response.code()} body=${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overpass Exception: ${e.message}", e)
            null
        }
    }

    private fun defaultSpeedForHighway(highway: String?): Int? = when (highway) {
        "motorway"                    -> 130
        "trunk"                       -> 100
        "primary", "secondary"        -> 100
        "tertiary"                    -> 70
        "unclassified", "residential" -> 50
        "living_street"               -> 7
        "service"                     -> 10
        else                          -> null
    }

    override fun stopRoute() {
        currentTrip = null
        routePoints = emptyList()
        totalLengthKM = 0.0
        durationSeconds = 0
    }
}
