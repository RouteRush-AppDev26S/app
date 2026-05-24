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
     */
    val totalLengthKM: Double
    
    /**
     * Enthält die voraussichtliche Reisedauer in Sekunden
     */
    val durationSeconds: Long
    
    /**
     *  SpeedLimit abfragen
     */
    suspend fun getSpeedLimit(now: VLocation): Int?
    
    /**
     * Route beenden
     */
    fun stopRoute()
}
