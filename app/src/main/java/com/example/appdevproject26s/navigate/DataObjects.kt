package com.example.appdevproject26s.navigate

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.maplibre.android.geometry.LatLng
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * written by Hans Wornik
 * Sammlung der Daten und Objekte für Navigate – OpenRouteService
 */

// ---- API Interface ----

interface OrsApi {
    @Headers("Accept: application/json")
    @POST("v2/directions/{profile}")
    suspend fun getRoute(
        @Header("Authorization") apiKey: String,
        @Path("profile") profile: String,
        @Body request: OrsRequest
    ): Response<OrsResponse>

    @Headers("Accept: application/json")
    @POST("v2/matrix/{profile}")
    suspend fun getMatrix(
        @Header("Authorization") apiKey: String,
        @Path("profile") profile: String,
        @Body request: OrsMatrixRequest
    ): Response<OrsMatrixResponse>
}

// ---- Client ----

object OrsClient {
    private const val BASE_URL = "https://api.openrouteservice.org/"
    const val API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6ImZkYTFlY2Q2N2FiNTQyMTdiMjAyMmZkMDNmNTI0ZTk0IiwiaCI6Im11cm11cjY0In0="

    val api: OrsApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OrsApi::class.java)
}

// ---- Hilfsfunktion ----

fun coordsOf(lon: Double, lat: Double): List<Double> = listOf(lon, lat)

// ---- Request ----

data class OrsRequest(
    val coordinates: List<List<Double>>,        // [[lon,lat], [lon,lat]]
    val language: String = "de-de",
    val units: String = "km",
    val instructions: Boolean = true,
    val instructions_format: String = "text",
    val extra_info: List<String> = listOf("surface", "waytype"),
    val options: OrsOptions? = null
)

data class OrsMatrixRequest(
    val locations: List<List<Double>>,          // [[lon,lat], [lon,lat]]
    val metrics: List<String> = listOf("distance", "duration"),
    val units: String = "km",
    val options: OrsOptions? = null
)

data class OrsOptions(
    val avoid_features: List<String>? = null
)

// ---- Response (JSON-Format, nicht GeoJSON) ----

data class OrsResponse(
    val bbox: List<Double>,
    val routes: List<OrsRoute>,
    val metadata: OrsMetadata?
)

data class OrsMetadata(
    val attribution: String?,
    val service: String?,
    val timestamp: Long?,
    val query: OrsQuery?
)

data class OrsQuery(
    val coordinates: List<List<Double>>,
    val profile: String?,
    val format: String?
)

data class OrsRoute(
    val summary: OrsSummary,
    val segments: List<OrsSegment>,
    val bbox: List<Double>,
    val geometry: String,           // encoded polyline (Precision 5) → decodePolyline()
    val way_points: List<Int>,      // Indices der Wegpunkte in der Geometrie
    val extras: OrsExtras?
) {
    fun toLatLngList(): List<LatLng> = decodePolyline(geometry)
}

data class OrsSummary(
    val distance: Double,           // km
    val duration: Double            // Sekunden
)

data class OrsSegment(
    val distance: Double,
    val duration: Double,
    val steps: List<OrsStep>
)

data class OrsStep(
    val distance: Double,
    val duration: Double,
    val type: Int,                  // Manöver-Typ (siehe unten)
    val instruction: String,
    val name: String,
    val way_points: List<Int>       // [startIndex, endIndex] in geometry
)

// ---- Geometry Decoder (Precision 5 für ORS) ----

fun decodePolyline(encoded: String, precision: Int = 5): List<LatLng> {
    val factor = Math.pow(10.0, precision.toDouble())
    val result = mutableListOf<LatLng>()
    var index = 0; var lat = 0; var lng = 0

    while (index < encoded.length) {
        var b: Int; var shift = 0; var result2 = 0
        do { b = encoded[index++].code - 63; result2 = result2 or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20)
        lat += if (result2 and 1 != 0) (result2 shr 1).inv() else result2 shr 1

        shift = 0; result2 = 0
        do { b = encoded[index++].code - 63; result2 = result2 or (b and 0x1f shl shift); shift += 5 } while (b >= 0x20)
        lng += if (result2 and 1 != 0) (result2 shr 1).inv() else result2 shr 1

        result.add(LatLng(lat / factor, lng / factor))
    }
    return result
}

// ---- Standort ----

data class VLocation(val lat: Double, val lon: Double)

// ---- Trip (berechnete Route) ----

data class Trip(
    val summary: OrsSummary,
    val segments: List<OrsSegment>,
    val extras: OrsExtras?
)

// ---- Extras (Geschwindigkeitslimits, Belag, Wegtyp) ----

data class OrsExtras(
    val speedlimits: OrsExtraData?,
    val surface: OrsExtraData?,
    val waytype: OrsExtraData?
)

data class OrsExtraData(
    val values: List<List<Int>>,        // [[startIndex, endIndex, value], ...]
    val summary: List<OrsExtraSummary>
) {
    fun speedLimitAt(index: Int): Int? {
        for (range in values) {
            if (range.size >= 3 && index >= range[0] && index < range[1]) return range[2]
        }
        return null
    }
}

data class OrsExtraSummary(
    val value: Int,
    val distance: Double,
    val amount: Double
)

// ---- Matrix Response ----

data class OrsMatrixResponse(
    val distances: List<List<Double>>?,         // [from][to]
    val durations: List<List<Double>>?,          // [from][to]
    val metadata: OrsMetadata?
)

// ---- Overpass API (Speed Limits via OSM) ----

object OverpassClient {
    private const val URL = "https://lz4.overpass-api.de/api/interpreter"

    private val client = OkHttpClient()

    fun query(overpassQuery: String): String? {
        val body = FormBody.Builder()
            .add("data", overpassQuery)
            .build()
        val request = Request.Builder()
            .url(URL)
            .addHeader("User-Agent", "AppDevProject26S/1.0")
            .post(body)
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                println("Overpass raw HTTP ${resp.code}")
                if (resp.isSuccessful) resp.body?.string() else {
                    val err = resp.body?.string()
                    println("Overpass error body: ${err?.take(300)}")
                    null
                }
            }
        } catch (e: Exception) {
            println("Overpass OkHttp Exception: ${e.javaClass.simpleName} ${e.message}")
            null
        }
    }
}
/*
*                    Written by Hans Wornik
*           Implementation of the DataObjects
 */
data class OverpassResponse(val elements: List<OverpassElement>)

data class OverpassElement(
    val type: String,
    val id: Long,
    val tags: Map<String, String>?
)

fun parseMaxspeed(value: String): Int? {
    val v = value.trim().lowercase()
    return when {
        v == "none" || v == "signals" || v == "unlimited" -> null
        v == "walk" -> 7
        v.endsWith("mph") -> {
            val mph = v.removeSuffix("mph").trim().toDoubleOrNull() ?: return null
            (mph * 1.60934).toInt()
        }
        v.matches(Regex("\\d+")) -> v.toInt()
        v.contains("motorway") -> 130
        v.contains("rural") -> 100
        v.contains("urban") -> 50
        v.contains("living_zone") || v.contains("living zone") -> 10
        else -> null
    }
}

/*
    ORS Manöver-Typen (step.type):
     0 = Links abbiegen
     1 = Rechts abbiegen
     2 = Scharf links
     3 = Scharf rechts
     4 = Leicht links
     5 = Leicht rechts
     6 = Geradeaus
     7 = Einbiegen (roundabout)
     8 = U-Turn
    10 = Ziel erreicht
    11 = Start
    12 = Kreisverkehr verlassen
*/
