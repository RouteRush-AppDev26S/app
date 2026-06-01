package com.example.appdevproject26s


import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.navigate.*

interface INavigate
{
    /**
     * Berechnet die Route und speichert sie ab
     */
    suspend fun calcRoute(start: VLocation, stop: VLocation, vehicle: String)
    /*
     *      Startet berechnete route
     */
    suspend fun startRoute()
    /**
     * Letzte Route laden
     */
    suspend fun loadLastRoute(): Trip?
    /*
    *   Aktualisiert alle variablen, wenn keine route aktiv ist verändert sich nur speedlimit
    *   bitte nur in mindestabstand von 5 sec aufrufen
     */
    suspend fun updatePosition(now: VLocation)
    /**
     * Enthält die Route als List<Position>
     */
    val routePoints: List<Position>

    /**
     * Enthält die Streckenlänge in KM
     * kann durch aufruf calcRemainingTimeFromServer(now: VLocation ) ersetzt werden
     */
    val totalLengthKM: Double
    
    /**
     * Enthält die voraussichtliche Reisedauer in Sekunden
     * kann durch aufruf calcRemainingTimeFromServer(now: VLocation ) ersetzt werden
     * beobachtbar
     */
    val durationSeconds: Long

    /**
     * Aktuelles SpeedLimit (beobachtbar)
     */
    val speedLimit: Int?
    

    /**
     * Mautstraßen vermeiden
     */
    var noMaut: Boolean

    /**
     * Autobahnen vermeiden
     */
    var noHighway: Boolean

    /**
     * Route beenden
     */
    fun stopRoute()

    /**
     * Aktualisiert die aktuelle Position und prüft Off-Route
     */
    suspend fun updatePosition(now: VLocation)

    /**
     * TextAusgabe der NavigationsAnweisungen
     * im Format
     * manoevertext.todrive restdistanz
     * manoevertext.distance abstand des Segment zum nächsten
     * manoevertext.todrivekm zurückzulegende kilometer bei Segment
     */
    var manoevertext: ArrayList<InstructionsNavigate>
}
