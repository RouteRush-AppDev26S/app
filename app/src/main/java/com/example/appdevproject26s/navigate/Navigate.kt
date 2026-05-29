/*
*                    Written by Hans Wornik
*           Implementation of the Navigate Interface
 */
package com.example.appdevproject26s.navigate

import android.content.Context
import android.os.Build
import com.google.gson.Gson
import org.maplibre.android.geometry.LatLng
import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.INavigate

import android.util.Log
import androidx.annotation.RequiresApi
import com.example.appdevproject26s.navigate.maneuversde

object Navigate : INavigate{
    private const val TAG = "Navigate"

    private var db: RouteDao? = null
    
    override var noMaut: Boolean = false
    override var noHighway: Boolean = false

    var currentTrip: Trip? = null
        private set
    private var ziel: VLocation?= null

    private var geraet: String?=null

    override var manoevertext: ArrayList<InstructionsNavigate> = ArrayList<InstructionsNavigate>()
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
        val avoids = mutableListOf<String>()
        if (noMaut) avoids.add("tollways")
        if (noHighway) avoids.add("highways")
        val orsOptions = if (avoids.isNotEmpty()) OrsOptions(avoid_features = avoids) else null

        val request = OrsRequest(
            coordinates = listOf(listOf(start.lon, start.lat), listOf(stop.lon, stop.lat)),
            options = orsOptions
        )
        ziel = stop   
        geraet = vehicle
        
        // ORS profile mapping (Valhalla uses auto/pedestrian/bicycle, ORS uses driving-car/foot-walking etc.)
        val profile = when (vehicle) {
            "auto", "driving-car" -> "driving-car"
            "pedestrian", "foot-walking" -> "foot-walking"
            "bicycle", "cycling-regular" -> "cycling-regular"
            else -> "driving-car"
        }

        val response = try {
            OrsClient.api.getRoute(OrsClient.API_KEY, profile, request)
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
        showChangeDirection();
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
        // 1. Erst in der aktuellen Route suchen (falls vorhanden)
        val trip = currentTrip
        if (trip != null && routePoints.isNotEmpty()) {
            val closestIndex = Zeitberechnung.findClosestShapeIndex(Position(now.lon, now.lat), routePoints)
            val limit = trip.extras?.speedlimits?.speedLimitAt(closestIndex)
            if (limit != null) {
                Log.d(TAG, "SpeedLimit from Trip: $limit")
                return limit
            }
        }

        // 2. Fallback auf Overpass API
        val query = "[out:json];way(around:100,${now.lat},${now.lon})[highway];out tags;"
        println("Overpass query: $query")
        val json = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            OverpassClient.query(query)
        } ?: return null
        println("Overpass raw: ${json.take(300)}")
        return try {
            val parsed = Gson().fromJson(json, OverpassResponse::class.java)
            val elements = parsed.elements
            println("Overpass elements: ${elements.size}")
            if (elements.isEmpty()) {
                println("Overpass 0 elements – kein Weg in 100m Radius?")
                return null
            }
            elements.forEach { el ->
                println("  way ${el.id}: highway=${el.tags?.get("highway")} maxspeed=${el.tags?.get("maxspeed")}")
            }
            val (hw, explicit) = pickBestHighway(elements)
            println("pickBestHighway -> hw=$hw explicit=$explicit")
            val result = explicit ?: defaultSpeedForHighway(hw)
            println("SpeedLimit result: $result")
            result
        } catch (e: Exception) {
            println("Overpass parse Exception: ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }

    override suspend fun calcRemainingTimeFromServer(now: VLocation): Pair<Double, Long>? {
        val target = ziel ?: return null
        
        val avoids = mutableListOf<String>()
        if (noMaut) avoids.add("tollways")
        if (noHighway) avoids.add("highways")
        val orsOptions = if (avoids.isNotEmpty()) OrsOptions(avoid_features = avoids) else null

        val request = OrsMatrixRequest(
            locations = listOf(listOf(now.lon, now.lat), listOf(target.lon, target.lat)),
            options = orsOptions
        )

        val profile = when (geraet) {
            "auto", "driving-car" -> "driving-car"
            "pedestrian", "foot-walking" -> "foot-walking"
            "bicycle", "cycling-regular" -> "cycling-regular"
            else -> "driving-car"
        }

        val response = try {
            OrsClient.api.getMatrix(OrsClient.API_KEY, profile, request)
        } catch (e: Exception) {
            Log.e(TAG, "ORS Matrix API Exception: ${e.message}", e)
            null
        }

        if (response == null || !response.isSuccessful) {
            Log.e(TAG, "ORS Matrix API Error: ${response?.code()} ${response?.message()}")
            return null
        }

        val body = response.body()
        val dist = body?.distances?.get(0)?.get(1) // Von 0 zu 1
        val dur = body?.durations?.get(0)?.get(1)

        return if (dist != null && dur != null) {
            dist to dur.toLong()
        } else {
            null
        }
    }

    private fun defaultSpeedForHighway(hw: String?): Int? = when (hw) {
        "motorway"       -> 130
        "trunk"          -> 100
        "primary"        -> 100
        "secondary"      -> 100
        "tertiary"       -> 80
        "unclassified"   -> 50
        "residential"    -> 30
        "living_street"  -> 7
        "service"        -> 10
        "track"          -> 10   // Feldweg
        "path",
        "footway",
        "cycleway",
        "bridleway"      -> null  // kein Kfz-Verkehr
        else             -> null
    }
    private fun pickBestHighway(elements: List<OverpassElement>): Pair<String?, Int?> {
        val priority = listOf(
            "motorway", "trunk", "primary", "secondary", "tertiary",
            "unclassified", "residential", "living_street", "service",
            "track", "path", "footway", "cycleway", "bridleway"
        )
        val sorted = elements.sortedBy { el ->
            priority.indexOf(el.tags?.get("highway") ?: "").let { if (it == -1) Int.MAX_VALUE else it }
        }
        val explicit = sorted.mapNotNull { it.tags?.get("maxspeed") }
            .firstNotNullOfOrNull { parseMaxspeed(it) }
        val hw = sorted.firstNotNullOfOrNull { it.tags?.get("highway") }
        return hw to explicit
    }
    override fun stopRoute() {
        currentTrip = null
        routePoints = emptyList()
        totalLengthKM = 0.0
        durationSeconds = 0
        ziel = null
        geraet = null
    }

    fun showChangeDirection() {
        try {
            val newList = ArrayList<InstructionsNavigate>()
            var remainingDist: Double = currentTrip?.summary?.distance ?: 0.0
            
            currentTrip?.segments?.forEach { segment ->
                segment.steps.forEach { step ->
                    val anweisung = maneuversde[step.type] ?: "unbekannt"

                            val schritt = InstructionsNavigate(
                                manoever = "In ${step.distance} km $anweisung",
                                distance = step.distance,
                                todrivekm = remainingDist
                            )
                            newList.add(schritt)
                            remainingDist -= step.distance
                        }
            }
            manoevertext = newList
            Log.d(TAG, "Manoevertext generiert: ${manoevertext.size} Einträge")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler in showChangeDirection: ${e.message}", e)
        }
    }

    override suspend fun showOnlyLeftInstruct(pos: VLocation): ArrayList<InstructionsNavigate> {
        val retmanlist = ArrayList<InstructionsNavigate>()
        val dist = calcRemainingTimeFromServer(pos) ?: return retmanlist
        
        val currentRemainingDist = dist.first
        
        manoevertext.forEach { instr ->
            // Nur Anweisungen nehmen, die noch "vor" uns liegen (todrivekm > currentRemainingDist ist falsch rum)
            // todrivekm ist die Distanz vom Start bis zu diesem Punkt? 
            // Nein, oben habe ich es als "Restdistanz ab diesem Punkt" definiert.
            // Also: Wenn todrivekm < currentRemainingDist, dann haben wir den Punkt schon passiert.
            if (instr.todrivekm <= currentRemainingDist) {
                retmanlist.add(instr)
            }
        }
        
        if (retmanlist.isNotEmpty()) {
            val nextManeuver = retmanlist[0]
            // Korrektur der Distanz zum nächsten Manöver
            // Das nächste Manöver ist bei 'nextManeuver.todrivekm'
            // Wir sind bei 'currentRemainingDist'
            val distToNext = currentRemainingDist - (nextManeuver.todrivekm - nextManeuver.distance)
            nextManeuver.distance = if (distToNext > 0) distToNext else 0.0
        }

        return retmanlist
    }
}
