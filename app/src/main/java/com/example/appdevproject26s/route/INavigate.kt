package com.example.appdevproject26s.route


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.maplibre.spatialk.geojson.Position



interface INavigate
{
    /**
     * Berechnet die Route und speichert sie ab
     */
    fun calcRoute(start: Location, stop: Location, vehicle: String)
    /*
     *      Startet berechnete route
     */
    fun startRoute()
    fun routeReset()
    suspend fun storeRoute()
    suspend fun loadLastRoute(): Trip?
    /*
    *   Aktualisiert alle variablen, wenn keine route aktiv ist verändert sich nur speedlimit
    *   bitte nur in mindestabstand von 5 sec aufrufen
     */
    fun updatePosition(now: Location)
    /**
     * Enthält die Route als List<Position>
     */
    val routePoints: List<Position>
    /*
    * enthält die tracking Daten
     */
    var trackPoints: List<OrsTrackPoint>
    /**
     * Enthält die Streckenlänge in KM
     * kann durch aufruf calcRemainingTimeFromServer(now: Location ) ersetzt werden
     */
    val totalLengthKM: Double
    
    /**
     * Enthält die voraussichtliche Reisedauer in Sekunden
     * kann durch aufruf calcRemainingTimeFromServer(now: Location ) ersetzt werden
     * beobachtbar
     */
    val durationSeconds: Double

    /**
     * Aktuelles SpeedLimit (beobachtbar)
     */
    val speedLimit: Int?
    val currentAddress: String?
    val currentPosition: Location?
    var noMaut: Boolean

    /**
     * Autobahnen vermeiden
     */
    var noHighway: Boolean
    var routeaktiv: Boolean
    val currentTrip: Trip?
    fun stopRoute()
    //fun triggerVibration(duration: Long = 500)
    suspend fun getCoordinatesFromAddress(address: String): Location?
    suspend fun getAdresseOnce(now: Location): String
    var manoevertext: ArrayList<InstructionsNavigate>

    val startAddress: String
    val destinationAddress: String
    val isCalculating: Boolean
    val errorMessage: String?
    fun isononePoint(von: Location, nach: Location): Double
    fun routeNachAdresse(adresseStart: String, adresseStop: String)
    /*
    Tracking
     */
    var trackingstart : Boolean
    var distance: Double

}
