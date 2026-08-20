/*
*                    Written by Hans Wornik
*           Implementation of the Navigate Interface
 */
package com.example.appdevproject26s.map.route
import android.content.ContentValues.TAG
import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.google.gson.Gson
import org.maplibre.spatialk.geojson.Position


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Locale
import kotlin.collections.first
import kotlin.collections.forEach
import kotlin.text.split

@Singleton
class Navigate @Inject constructor (
    @ApplicationContext private val context: Context,
    private val db: RouteDao,
    private val vibrator: Vibrator,
    private val zeitberechnung: Zeitberechnung,
    private val api: OrsApi,
    private val tracking: MatheFile,
    private val zahler: Schrittzahler,
    private val time: Timer
) : INavigate {
    private var dist: Double = 0.0

    /*companion object {
        private const val TAG = "Navigate"
    }*/
    override var trackingstart by mutableStateOf(false)
    var oldadresse: String = ""
    override var noMaut: Boolean = false

    override var noHighway: Boolean = false
    var old: Location? = null
    private var lastTrackedPos: Location? = null

    override var routeaktiv: Boolean = false
    override var currentTrip: Trip? by mutableStateOf(null)
        private set
    override var distance: Double by mutableStateOf(0.0)
    private var ziel: Location = Location(0.0,0.0)

    private var geraet: String = ""

    override var manoevertext: ArrayList<InstructionsNavigate> by mutableStateOf(ArrayList<InstructionsNavigate>())
    override var routePoints: List<Position> by mutableStateOf(emptyList())
        private set
    override var trackPoints: List<OrsTrackPoint> by  mutableStateOf(emptyList())
    override var totalLengthKM: Double by mutableStateOf(0.0)
        private set

    override var durationSeconds: Double by mutableDoubleStateOf(0.0)
        private set

    override var speedLimit: Int? by mutableStateOf(null)
        private set

    override var currentAddress: String by mutableStateOf("")
        private set

    override var currentPosition: Location? by mutableStateOf(null)
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
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

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
    override fun routeReset(){
        currentTrip = null
        routePoints = emptyList()
        manoevertext = ArrayList()
        totalLengthKM = 0.0
        durationSeconds = 0.0
        speedLimit = null
        startAddress = ""
        destinationAddress = ""
        errorMessage = null
        isCalculating = false
        routeaktiv = false
    }
    override fun calcRoute(start: Location, stop: Location, vehicle: String)
    {
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

            withContext(Dispatchers.Main) {
                val current = currentTrip
                if (current == null) {
                    currentTrip = trip
                } else {
                    currentTrip = Trip(
                        summary = OrsSummary(
                            distance = current.summary.distance + trip.summary.distance,
                            duration = current.summary.duration + trip.summary.duration
                        ),
                        segments = current.segments + trip.segments,
                        extras = trip.extras
                    )
                }
                totalLengthKM = currentTrip?.summary?.distance ?: 0.0
                durationSeconds = currentTrip?.summary?.duration?.toDouble() ?: 0.0

                val newPoints = route.toLatLngList().map { Position(it.longitude, it.latitude) }
                routePoints = routePoints + if (routePoints.isNotEmpty()) newPoints.drop(1) else newPoints

                //startAddress = getAdresseOnce(start)
                destinationAddress = getAdresseOnce(stop)
                //scope.launch { getSpeedLimit(start) }
                println(" In zielberechnung ")
                isCalculating = false
                showChangeDirection(true,true)
                println(" print von calc Ziel")
                //NavigateTestaus(start, stop)
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
            durationSeconds = trip.summary.duration.toDouble()
            routePoints = RouteConverter.jsonToPoints(last.routePointsJson) ?: emptyList()
            trip
        } catch (e: Exception) {
            Log.e(TAG, "loadLastRoute fehlgeschlagen: ${e.message}", e)
            null
        }
    }

    override fun updatePosition(now: Location) {
        scope.launch {
            val lastPos = old
            old = now
            distance += if (lastPos != null) tracking.haversineDistance(lastPos, now)/1000 else 0.0

            val lastTracked = lastTrackedPos
            if (lastTracked == null || tracking.haversineDistance(lastTracked, now) > 100) {
                lastTrackedPos = now
                getSpeedLimit(now)
                val addr = updateAdresse(now)
                withContext(Dispatchers.Main) {
                    currentAddress = addr
                    currentPosition = now
                }

                if (trackingstart) {
                    val currentKmh = if (lastPos != null) tracking.calculateKmhWithLocation(distance, lastPos, now) else 0.0
                    trackPoints = tracking.addTrakkingData(
                        trackPoints = trackPoints,
                        start = lastTracked ?: now,
                        stop = now,
                        schritte = zahler.schritte,
                        kmh = currentKmh
                    )
                }
            }

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
        val dur_matrix = body?.durations?.get(0)?.get(1)?.toDouble()

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
        durationSeconds = 0.0
        speedLimit = null
        startAddress = ""
        destinationAddress = ""
        ziel = Location(0.0,0.0)
        geraet = ""
        routeaktiv = false
    }

    override suspend fun storeRoute() {
        // Implementation for storing the current route if needed
        // Currently, calcRoute already inserts into DB.
    }

    suspend fun showChangeDirection(erstes: Boolean = false, letztes: Boolean = false) {
        try {
            val newList = ArrayList<InstructionsNavigate>()
            var remainingDist: Double = currentTrip?.summary?.distance ?: 0.0
            if(!erstes) {
                newList.add(
                    InstructionsNavigate(
                        manoever = "Start",
                        distance = 0.0,
                        todrivekm = remainingDist,
                        adresse = startAddress
                    )
                )
            }

            currentTrip?.segments?.forEach { segment ->
                segment.steps.forEach { step ->
                    var anweisung = maneuversde[step.type] ?: "unbekannt"
                    val startKombinationsIndex: Int = step.way_points.first()
                    val endKombinationsIndex: Int = step.way_points.last()
                    var strassepoint=startKombinationsIndex+(endKombinationsIndex-startKombinationsIndex)/2
                    // Jetzt holen wir uns die echte Location aus der Gesamtliste
                    //val manoeverLocation: Position? = if (startKombinationsIndex = step.way_points?.lastOrNull() != null && startKombinationsIndex < routePoints.size) {
                    val manoeverLocation = routePoints[step.way_points.first()]
                    if (anweisung == "unbekannt") anweisung = "wechsel auf"
                    val schritt = InstructionsNavigate(
                        manoever = "In ${step.distance} km $anweisung",
                        distance = step.distance,
                        todrivekm = remainingDist,
                        adresse = getAdresseOnce(Location(manoeverLocation.latitude, manoeverLocation.longitude))
                    )
                    var newadresse = schritt.adresse.split(",").first()
                        .split(Regex("(?=\\d)"), limit = 2).first()
                    if ((schritt.adresse.isEmpty() || newadresse == oldadresse) && (step.way_points.last() + 1) < routePoints.size) {
                        val nextManoeverLocation = routePoints[step.way_points.last() + 1]
                        schritt.adresse = getAdresseOnce(Location(nextManoeverLocation.latitude, nextManoeverLocation.longitude))
                    }
                    oldadresse=newadresse;
                    newList.add(schritt)
                    remainingDist -= step.distance
                }
            }
            if(!letztes) {
                newList.add(
                    InstructionsNavigate(
                        manoever = "Ziel",
                        distance = 0.0,
                        todrivekm = 0.0,
                        adresse = destinationAddress
                    )
                )
            }

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

    override suspend fun getCoordinatesFromAddress(address: String): Location? {
        return withContext(Dispatchers.IO) {
            try {
                if (address.isEmpty()) return@withContext null
                Log.d("Navi", "Geocoding: $address")

                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                val addr = results?.firstOrNull()

                if (addr != null) {
                    Log.d("Navi", "Geocoder success: ${addr.latitude}, ${addr.longitude}")
                    Location(lat = addr.latitude, lon = addr.longitude)
                } else {
                    Log.d("Navi", "Geocoder found nothing, trying ORS fallback")
                    // Fallback auf ORS falls Geocoder nichts findet
                    val response = api.geocode(apiKey = API_KEY, text = address)
                    val feature = response.features.firstOrNull()
                    val coords = feature?.geometry?.coordinates
                    if (coords != null && coords.size >= 2) {
                        Location(lat = coords[1], lon = coords[0])
                    } else null
                }
            } catch (e: Exception) {
                Log.e("Navi", "Geocoder Name failed: ${e.message}")
                null
            }
        }
    }

    override suspend fun getAdresseOnce(now: Location): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("Navi", "Reverse Geocoding: ${now.lat}, ${now.lon}")
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(now.lat, now.lon, 1)
                val addr = results?.firstOrNull()

                if (addr != null) {
                    val street = addr.thoroughfare ?: ""
                    val house = addr.subThoroughfare ?: ""
                    val city = addr.locality ?: ""

                    val result = if (street.isNotEmpty()) {
                        buildString {
                            append(street)
                            if (house.isNotEmpty()) append(" ").append(house)
                            if (city.isNotEmpty()) append(", ").append(city)
                        }
                    } else {
                        addr.getAddressLine(0) ?: "Unbekannte Straße"
                    }
                    Log.d("Navi", "Geocoder result: $result")
                    result
                } else {
                    Log.d("Navi", "Geocoder reverse found nothing, trying ORS fallback")
                    // Fallback auf ORS
                    val response = api.reverseGeocode(
                        apiKey = API_KEY,
                        longitude = now.lon,
                        latitude = now.lat
                    )
                    val label = response.features.firstOrNull()?.properties?.label ?: "Adresse nicht gefunden"
                    Log.d("Navi", "ORS fallback result: $label")
                    label
                }
            } catch (e: Exception) {
                Log.e("Navi", "Geocoder Reverse failed: ${e.message}")
                "Fehler bei Adressabfrage"
            }
        }
    }

    suspend fun updateAdresse(now: Location): String {

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
                    return vollständigeAdresse
                }
            } catch (e: Exception) {
                // Fange Netzwerkfehler (z.B. kein Internet) ab, damit die App nicht abstürzt
                Log.e("Navi", "Fehler beim Laden der Adresse: ${e.message}")
            }
            return ""
        }

    override fun isononePoint(von: Location, nach: Location): Double {
        return zeitberechnung.haversine(Position(von.lon, von.lat),Position(nach.lon, nach.lat))
    }

    override fun routeNachAdresse(adresseStart: String, adresseStop: String) {
        TODO("Not yet implemented")
    }
}
