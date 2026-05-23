package com.example.appdevproject26s.navigate

import android.content.Context
import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.INavigate
/*
*                    Written by Hans Wornik
*           Implementation of the Navigate Interface
 */
object Navigate : INavigate{

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
        val request = ValhallaRequest(
            locations = listOf(start, stop),
            costing = vehicle
        )

        val response = try {
            ValhallaClient.api.getRoute(request)
        } catch (_: Exception) {
            null
        }

        if (response == null || !response.isSuccessful) return null

        val trip = response.body()?.trip ?: return null
        currentTrip = trip
        totalLengthKM = trip.summary.length
        durationSeconds = trip.summary.time.toLong()
        val leg = trip.legs.firstOrNull()
        routePoints = if (leg != null) decodePolyline(leg.shape) else emptyList()

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
        totalLengthKM = trip.summary.length
        durationSeconds = trip.summary.time.toLong()
        routePoints = RouteConverter.jsonToPoints(last.routePointsJson)
        return trip
    }

    suspend fun getHistory(): List<RouteEntity> = db?.getAllRoutes() ?: emptyList()
    suspend fun clearAllHistory() { db?.clearHistory() }
    suspend fun deleteRouteFromHistory(routeId: Int) { db?.deleteRouteById(routeId) }

    override suspend fun getSpeedLimit(now: VLocation): Int? {
        val trip = currentTrip
        if (trip != null && routePoints.isNotEmpty()) {
            val closestIndex = Zeitberechnung.findClosestShapeIndex(Position(now.lon, now.lat), routePoints)
            val maneuver = trip.legs.firstOrNull()?.maneuvers?.find {
                closestIndex >= it.begin_shape_index && closestIndex <= it.end_shape_index
            }
            return maneuver?.speed_limit
        } else {
            val request = TraceAttributesRequest(shape = listOf(now))
            val response = try {
                ValhallaClient.api.getTraceAttributes(request)
            } catch (_: Exception) {
                null
            }
            return response?.body()?.edges?.firstOrNull()?.speed_limit
        }
    }

    override fun stopRoute() {
        currentTrip = null
        routePoints = emptyList()
        totalLengthKM = 0.0
        durationSeconds = 0
    }

    private fun decodePolyline(encoded: String, precision: Double = 1e6): List<Position> {
        val poly = ArrayList<Position>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(Position(lng / precision, lat / precision))
        }
        return poly
    }
}
