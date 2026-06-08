/*
*                    Written by Hans Wornik
*           Implementation of the Navigate Interface
 */
package com.example.appdevproject26s.navigate

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.google.gson.Gson
import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.INavigate

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.appdevproject26s.navigate.Zeitberechnung
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Navigate @Inject constructor(
    private val db: RouteDao,
    private val vibrator: Vibrator,
    private val zeitberechnung: Zeitberechnung,
    private val api: OrsApi
) : INavigate {
    private var dist: Double = 0.0

    companion object {
        private const val TAG = "Navigate"
    }

    override var noMaut: Boolean = false
    override var noHighway: Boolean = false

    private var routeaktiv: Boolean = false
    var currentTrip: Trip? by mutableStateOf(null)
        private set
    private var ziel: Location = Location(0.0,0.0)

    private var geraet: String = ""

    override var manoevertext: ArrayList<InstructionsNavigate> by mutableStateOf(ArrayList<InstructionsNavigate>())
    override var routePoints: List<Position> by mutableStateOf(emptyList())
        private set

    override var totalLengthKM: Double by mutableStateOf(0.0)
        private set

    override var durationSeconds: Long by mutableStateOf(0L)
        private set

    override var speedLimit: Int? by mutableStateOf(null)
        private set

    override var startAddress: String by mutableStateOf("")
        private set
    override var destinationAddress: String by mutableStateOf("")
        private set

    override var isCalculating: Boolean by mutableStateOf(false)
        private set
    override var errorMessage: String? by mutableStateOf(null)
        private set

    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())

    fun triggerVibration(duration: Long = 500) {
        val currentVibrator = vibrator
        Log.d(TAG, "Triggering vibration: $duration ms")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        duration,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                currentVibrator.vibrate(duration)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }
    //val meineOrsApi = OrsClientAd.create(OpenRouteServiceApi::class.java)
    override fun calcRoute(start: Location, stop: Location, vehicle: String)
    {
        // Alte Route löschen, bevor die neue berechnet wird
        currentTrip = null
        routePoints = emptyList()
        manoevertext = ArrayList()
        totalLengthKM = 0.0
        durationSeconds = 0L
        speedLimit = null
        startAddress = ""
        destinationAddress = ""
        errorMessage = null
        isCalculating = true

        scope.launch {
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
            routeaktiv = true

            // ORS profile mapping (Valhalla uses auto/pedestrian/bicycle, ORS uses driving-car/foot-walking etc.)

            val profile = when (vehicle) {
                "auto", "driving-car" -> "driving-car"
                "pedestrian", "foot-walking" -> "foot-walking"
                "bicycle", "cycling-regular" -> "cycling-regular"
                else -> "driving-car"
            }

            val response = try {
                withContext(Dispatchers.IO) {
                    api.getRoute(API_KEY, profile, request)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ORS API Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    errorMessage = "Netzwerkfehler: ${e.message}"
                    isCalculating = false
                }
                return@launch
            }

            if (response == null || !response.isSuccessful) {
                val code = response?.code()
                val msg = if (code == 429) "Zu viele Anfragen, bitte kurz warten."
                          else "Routenfehler: $code ${response?.message()}"
                Log.e(TAG, "ORS API Error: $code ${response?.message()}")
                withContext(Dispatchers.Main) {
                    errorMessage = msg
                    isCalculating = false
                }
                return@launch
            }

            val body = response.body()
            Log.d(TAG, "ORS API Success. Body: $body")
            val route = body?.routes?.firstOrNull()

            if (route == null) {
                Log.e(TAG, "ORS Route is NULL")
                withContext(Dispatchers.Main) {
                    errorMessage = "Keine Route gefunden."
                    isCalculating = false
                }
                return@launch
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

            startAddress = getAdresseOnce(start)
            destinationAddress = getAdresseOnce(stop)
            try {
                db.insertRoute(
                    RouteEntity(
                        timestamp = System.currentTimeMillis(),
                        startName = startAddress.ifEmpty { "${start.lat}, ${start.lon}" },
                        destinationName = destinationAddress.ifEmpty { "${stop.lat}, ${stop.lon}" },
                        tripJson = RouteConverter.tripToJson(trip),
                        routePointsJson = RouteConverter.pointsToJson(routePoints)
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "DB insertRoute fehlgeschlagen: ${e.message}", e)
            }
            scope.launch { getSpeedLimit(start) }
            withContext(Dispatchers.Main) {
                isCalculating = false
                showChangeDirection()
                NavigateTestaus(start, stop)
            }
        }
    }

    fun NavigateTestaus(start: Location, stop: Location){
        println("------------------------Start Berechne Route-----------------------------------------------------------")
        if (currentTrip != null) {
            println("STATUS: Erfolg")
            println("START:  lat=${start.lat}, lon=${start.lon}")
            println("ZIEL:   lat=${stop.lat}, lon=${stop.lon}")
            println("DISTANZ: ${totalLengthKM} km")
            println("DAUER: ${durationSeconds} Sekunden")
            println("DAUER (min): ${durationSeconds / 60}")
            println("PUNKTE gesamt: ${routePoints.size}")

            val sample = routePoints
                .filterIndexed { i, _ -> i % maxOf(1, routePoints.size / 10) == 0 }
                .take(10)
            println("--- 10 Routenpunkte (gleichmäßig verteilt) ---")
            sample.forEachIndexed { i, p -> println("  [${i + 1}] lat=${p.latitude}, lon=${p.longitude}") }


            //println("SPEED LIMIT (Startpunkt): ${speedLimit ?: "N/A"} km/h")

            // Test Matrix API (Reine Zeitabfrage)
            println("--- Matrix API Test ---")
            scope.launch {
                calcRemainingTimeFromServer(start)
                if (manoevertext != null) {
                    println("Matrix DISTANZ: ${totalLengthKM} km")
                    println("Matrix DAUER: ${durationSeconds} Sekunden")
                } else {
                    println("Matrix API fehlgeschlagen")
                }
            }
        } else {
            Log.e(TAG, "STATUS: Fehler bei der Routenberechnung")
        }
        println("------------------------Stop Berechnen Route-----------------------------------------------------------")
    }

    override fun startRoute() {
        if (currentTrip != null)
            routeaktiv = true;
    }

    override suspend fun loadLastRoute(): Trip? {
        val last = db.getLastRoute() ?: return null
        if (last.tripJson.isEmpty()) return null
        return try {
            val trip = RouteConverter.jsonToTrip(last.tripJson) ?: return null
            currentTrip = trip
            totalLengthKM = trip.summary.distance
            durationSeconds = trip.summary.duration.toLong()
            routePoints = RouteConverter.jsonToPoints(last.routePointsJson) ?: emptyList()
            trip
        } catch (e: Exception) {
            Log.e(TAG, "loadLastRoute fehlgeschlagen: ${e.message}", e)
            null
        }
    }

    override fun updatePosition(now: Location) {
        scope.launch {
            getSpeedLimit(now)
            updateAdresse(now)
            if (routeaktiv) {
                if (zeitberechnung.isOffRoute(now, routePoints, 50.0)) {
                    calcRoute(now, ziel, geraet)
                } else {
                    calcRemainingTimeFromServer(now)
                    showOnlyLeftInstruct(now)
                    showChangeDirection()
                }
            }
        }
    }

    suspend fun getHistory(): List<RouteEntity> = db.getAllRoutes()
    suspend fun clearAllHistory() {
        db.clearHistory()
    }

    suspend fun deleteRouteFromHistory(routeId: Int) {
        db.deleteRouteById(routeId)
    }

    suspend fun getSpeedLimit(now: Location) {
        // 1. Erst in der aktuellen Route suchen (falls vorhanden)
        val trip = currentTrip
        var result: Int? = null

        if (trip != null && routePoints.isNotEmpty()) {
            val closestIndex =
                zeitberechnung.findClosestShapeIndex(Position(now.lon, now.lat), routePoints)
            val limit = trip.extras?.speedlimits?.speedLimitAt(closestIndex)
            if (limit != null) {
                Log.d(TAG, "SpeedLimit from Trip: $limit")
                result = limit
            }
        }

        if (result == null) {
            // 2. Fallback auf Overpass API
            val query = "[out:json];way(around:50,${now.lat},${now.lon})[highway];out tags;"
            println("Overpass query: $query")
            val json = withContext(Dispatchers.IO) {
                OverpassClient.query(query)
            }
            if (json != null) {
                println("Overpass raw: ${json.take(300)}")
                result = try {
                    val parsed = Gson().fromJson(json, OverpassResponse::class.java)
                    val elements = parsed.elements
                    println("Overpass elements: ${elements.size}")
                    if (elements.isEmpty()) {
                        println("Overpass 0 elements – kein Weg in 100m Radius?")
                        null
                    } else {
                        elements.forEach { el ->
                            println(
                                "  way ${el.id}: highway=${el.tags?.get("highway")} maxspeed=${
                                    el.tags?.get(
                                        "maxspeed"
                                    )
                                }"
                            )
                        }
                        val (hw, explicit) = pickBestHighway(elements)
                        println("pickBestHighway -> hw=$hw explicit=$explicit")
                        explicit ?: defaultSpeedForHighway(hw)
                    }
                } catch (e: Exception) {
                    println("Overpass parse Exception: ${e.javaClass.simpleName} ${e.message}")
                    null
                }
            }
        }

        println("SpeedLimit result: $result")
        if (speedLimit != result) {
            speedLimit = result
        }
    }

    suspend fun calcRemainingTimeFromServer(now: Location) {
        val target = ziel ?: return

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
            withContext(Dispatchers.IO) {
                api.getMatrix(API_KEY, profile, request)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ORS Matrix API Exception: ${e.message}", e)
            null
        }

        if (response == null || !response.isSuccessful) {
            Log.e(TAG, "ORS Matrix API Error: ${response?.code()} ${response?.message()}")
            return
        }

        val body = response.body()
        val dist_matrix = body?.distances?.get(0)?.get(1) ?: 0.0 // Von 0 zu 1
        val dur_matrix = body?.durations?.get(0)?.get(1)?.toLong()

        if (dist_matrix != null && dur_matrix != null) {
            totalLengthKM = dist_matrix
            durationSeconds = dur_matrix
        }
    }

    private fun defaultSpeedForHighway(hw: String?): Int? = when (hw) {
        "motorway" -> 130
        "trunk" -> 100
        "primary" -> 100
        "secondary" -> 100
        "tertiary" -> 80
        "unclassified" -> 50
        "residential" -> 30
        "living_street" -> 7
        "service" -> 10
        "track" -> 10   // Feldweg
        "path",
        "footway",
        "cycleway",
        "bridleway" -> null  // kein Kfz-Verkehr
        else -> null
    }

    private fun pickBestHighway(elements: List<OverpassElement>): Pair<String?, Int?> {
        val priority = listOf(
            "motorway", "trunk", "primary", "secondary", "tertiary",
            "unclassified", "residential", "living_street", "service",
            "track", "path", "footway", "cycleway", "bridleway"
        )
        val sorted = elements.sortedBy { el ->
            priority.indexOf(el.tags?.get("highway") ?: "")
                .let { if (it == -1) Int.MAX_VALUE else it }
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
        speedLimit = null
        startAddress = ""
        destinationAddress = ""
        ziel = Location(0.0,0.0)
        geraet = ""
        routeaktiv = false
    }

    suspend fun showChangeDirection() {
        try {
            val newList = ArrayList<InstructionsNavigate>()
            var remainingDist: Double = currentTrip?.summary?.distance ?: 0.0

            newList.add(InstructionsNavigate(
                manoever = "Start",
                distance = 0.0,
                todrivekm = remainingDist,
                adresse = startAddress
            ))

            currentTrip?.segments?.forEach { segment ->
                segment.steps.forEach { step ->
                    var anweisung = maneuversde[step.type] ?: "unbekannt"
                    if (anweisung == "unbekannt") anweisung = "wechsel auf"
                    val schritt = InstructionsNavigate(
                        manoever = "In ${step.distance} km $anweisung",
                        distance = step.distance,
                        todrivekm = remainingDist,
                        adresse = ""
                    )
                    newList.add(schritt)
                    remainingDist -= step.distance
                }
            }

            newList.add(InstructionsNavigate(
                manoever = "Ziel",
                distance = 0.0,
                todrivekm = 0.0,
                adresse = destinationAddress
            ))

            manoevertext = newList
            Log.d(TAG, "Manoevertext generiert: ${manoevertext.size} Einträge")
        } catch (e: Exception) {
            Log.e(TAG, "Fehler in showChangeDirection: ${e.message}", e)
        }
    }
    fun showOnlyLeftInstruct(pos: Location) {
        val retmanlist = ArrayList<InstructionsNavigate>() ?: return


        manoevertext.forEach { instr ->
            // Nur Anweisungen nehmen, die noch "vor" uns liegen (todrivekm > currentRemainingDist ist falsch rum)
            // todrivekm ist die Distanz vom Start bis zu diesem Punkt?
            // Nein, oben habe ich es als "Restdistanz ab diesem Punkt" definiert.
            // Also: Wenn todrivekm < currentRemainingDist, dann haben wir den Punkt schon passiert.
            if (instr.todrivekm <= dist) {
                retmanlist.add(instr)
            }
        }

        if (retmanlist.isNotEmpty()) {
            val nextManeuver = retmanlist[0]
            // Korrektur der Distanz zum nächsten Manöver
            // Das nächste Manöver ist bei 'nextManeuver.todrivekm'
            // Wir sind bei 'currentRemainingDist'
            val distToNext =
                dist - (nextManeuver.todrivekm - nextManeuver.distance)
            nextManeuver.distance = if (distToNext > 0) distToNext else 0.0
        }
        manoevertext=retmanlist;
    }

    private var lastAddressLocation: Location? = null

    suspend fun getCoordinatesFromAddress(address: String): Location? {
        return try {
            val response = withContext(Dispatchers.IO) {
                api.geocode(
                    apiKey = API_KEY,
                    text = address
                )
            }
            val feature = response.features.firstOrNull()
            val coords = feature?.geometry?.coordinates
            if (coords != null && coords.size >= 2) {
                Location(lat = coords[1], lon = coords[0])
            } else null
        } catch (e: retrofit2.HttpException) {
            Log.e("Navi", "Geocoding HTTP ${e.code()}: ${e.message()}")
            null
        } catch (e: Exception) {
            Log.e("Navi", "Fehler beim Geocoding: ${e.message}")
            null
        }
    }

    suspend fun getAdresseOnce(now: Location): String {
        return try {
            val response = withContext(Dispatchers.IO) {
                api.reverseGeocode(
                    apiKey = API_KEY,
                    longitude = now.lon,
                    latitude = now.lat
                )
            }
            response.features.firstOrNull()?.properties?.label ?: ""
        } catch (e: retrofit2.HttpException) {
            Log.e("Navi", "Geocoding HTTP ${e.code()} ${e.message()} body=${e.response()?.errorBody()?.string()}")
            ""
        } catch (e: Exception) {
            Log.e("Navi", "Geocoding Exception ${e.javaClass.simpleName}: ${e.message}")
            ""
        }
    }

    suspend fun updateAdresse(now: Location) {

        // BERECHNUNG: Hat sich die Position deutlich verändert?
        /*val sollteAdresseLaden = lastAddressLocation == null ||
                isOffRoute(
                    now,
                    listOf(Position(lastAddressLocation!!.lon, lastAddressLocation!!.lat)),
                    30.0
                )

        if (sollteAdresseLaden) {*/
            try {
                // Retrofit-Aufruf außerhalb der Composable (in der suspend-Funktion)
                val response = withContext(Dispatchers.IO) {
                    api.reverseGeocode(
                        apiKey = API_KEY,
                        longitude = now.lon,
                        latitude = now.lat
                    )
                }

                // Daten sicher auslesen
                val properties = response.features.firstOrNull()?.properties
                val vollständigeAdresse =
                    properties?.label // "Berliner Straße 45, 69120 Heidelberg"

                if (vollständigeAdresse != null) {
                    // Schicke die Adresse an dein UI (z.B. über ein StateFlow oder LiveData)
                    println(vollständigeAdresse)
                    lastAddressLocation = now
                }
            } catch (e: Exception) {
                // Fange Netzwerkfehler (z.B. kein Internet) ab, damit die App nicht abstürzt
                Log.e("Navi", "Fehler beim Laden der Adresse: ${e.message}")
            }
       // }
    }
    override fun isononePoint(von: Location,nach: Location):Double {
        return zeitberechnung.haversine(Position(von.lon, von.lat),Position(nach.lon, nach.lat))
    }

    override fun routeNachAdresse(adresseStart: String, adresseStop: String) {
        TODO("Not yet implemented")
    }
}
