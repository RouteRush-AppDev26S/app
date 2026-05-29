package com.example.appdevproject26s


import org.maplibre.spatialk.geojson.Position
import com.example.appdevproject26s.navigate.*

interface INavigate
{
    /**
     * Berechnet die Route und speichert sie ab
     */
    suspend fun calcRoute(start: VLocation, stop: VLocation, vehicle: String): Trip?
    
    /**
     * Letzte Route laden
     */
    suspend fun loadLastRoute(): Trip?
    
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
     */
    val durationSeconds: Long
    
    /**
     *  SpeedLimit abfragen
     */
    suspend fun getSpeedLimit(now: VLocation): Int?
    
    /**
     * Schätzt die verbleibende Zeit und Distanz zum aktuellen Ziel über die Server Matrix API
     * @return Pair(Distanz_in_KM, Dauer_in_Sekunden)
     */
    suspend fun calcRemainingTimeFromServer(now: VLocation ): Pair<Double, Long>?

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
    /**
     * TextAusgabe der NavigationsAnweisungen ab aktueller Position
     * im Format
     * manoevertext.todrive restdistanz
     * manoevertext.distance abstand des Segmentes zum nächsten
     * manoevertext.todrivekm zurückzulegende kilometer bei Segment
     *
     *  Bei first Element in distance der aktuelle Abstand zum nächsten NavPunkt
     */
    suspend fun showOnlyLeftInstruct(pos : VLocation) : ArrayList<InstructionsNavigate>
}
