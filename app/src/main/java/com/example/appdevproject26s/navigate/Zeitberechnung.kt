/*
*                    Written by Hans Wornik
*           Implementation of the TimeManagnment
 */
package com.example.appdevproject26s.navigate

import org.maplibre.spatialk.geojson.Position
import kotlin.math.*

object Zeitberechnung {

    /**
     * Berechnet die verbleibende Distanz zum Ziel in Kilometern.
     */
    fun getRemainingDistance(now: VLocation, routePoints: List<Position>): Double {
        if (routePoints.isEmpty()) return 0.0
        
        val closestIndex = findClosestShapeIndex(Position(now.lon, now.lat), routePoints)
        var distance = 0.0
        
        for (i in closestIndex until routePoints.size - 1) {
            distance += haversine(routePoints[i], routePoints[i + 1])
        }
        
        return distance / 1000.0 // km
    }

    /**
     * Schätzt die verbleibende Zeit in Sekunden.
     * Nutzt die verbleibende Distanz und eine Durchschnittsgeschwindigkeit.
     */
    fun getRemainingTime(now: VLocation, routePoints: List<Position>, trip: Trip?): Double {
        val remainingDist = getRemainingDistance(now, routePoints)
        if (remainingDist <= 0 || trip == null) return 0.0
        
        // Einfache Schätzung: Verhältnis der Distanz zur ursprünglichen Zeit
        val totalDist = trip.summary.distance
        val totalTime = trip.summary.duration
        
        return if (totalDist > 0) (remainingDist / totalDist) * totalTime else 0.0
    }

    /**
     * Prüft, ob der Nutzer Off-Route ist.
     */
    fun isOffRoute(
        now: VLocation,
        routePoints: List<Position>,
        thresholdMeters: Double = 50.0
    ): Boolean {
        if (routePoints.isEmpty()) return false
        val currentPos = Position(now.lon, now.lat)
        
        var minDistance = Double.MAX_VALUE
        for (point in routePoints) {
            val dist = haversine(currentPos, point)
            if (dist < minDistance) minDistance = dist
        }
        return minDistance > thresholdMeters

    }

    /**
     * Hilfsfunktion: Findet den Index des nächstgelegenen Punktes auf der Route.
     */
    fun findClosestShapeIndex(now: Position, routePoints: List<Position>): Int {
        var minDistance = Double.MAX_VALUE
        var closestIndex = 0

        for (i in routePoints.indices) {
            val dist = haversine(now, routePoints[i])
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }
        return closestIndex
    }

    /**
     * Haversine Formel zur Distanzberechnung in Metern.
     */
    fun haversine(p1: Position, p2: Position): Double {
        val r = 6371000.0 // m
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
