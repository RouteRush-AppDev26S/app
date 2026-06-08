package com.example.appdevproject26s


import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.navigate.*

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
    /**
     * Letzte Route laden
     */
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
     * TextAusgabe der NavigationsAnweisungen
     * im Format
     * manoevertext.todrive restdistanz
     * manoevertext.distance abstand des Segment zum nächsten
     * manoevertext.todrivekm zurückzulegende kilometer bei Segment
     */
    var manoevertext: ArrayList<InstructionsNavigate>

    val startAddress: String
    val destinationAddress: String
    val isCalculating: Boolean
    val errorMessage: String?

    fun isononePoint(von: Location,nach: Location):Double

    fun routeNachAdresse(adresseStart:String,adresseStop:String)
}
