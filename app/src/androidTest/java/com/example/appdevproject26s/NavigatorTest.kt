package com.example.appdevproject26s

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.appdevproject26s.navigate.Navigate
import com.example.appdevproject26s.navigate.VLocation
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class NavigatorTest {

    @Before
    fun setup() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Navigate.init(appContext)
    }

    @Test
    fun testNavigatorOutput(): Unit = runBlocking {
        val start = VLocation(lat = 46.6247, lon = 14.3053)
        val stop  = VLocation(lat =  47.073863, lon = 15.415192)

        println("--- NAVIGATOR TEST START ---")
        println("Route: Klagenfurt -> Villach")

        val trip = try {
            Navigate.calcRoute(start, stop, "auto")
        } catch (e: Exception) {
            println("EXCEPTION während calcRoute: ${e.message}")
            null
        }

        if (trip != null) {
            println("STATUS: Erfolg")
            println("START:  lat=${start.lat}, lon=${start.lon}")
            println("ZIEL:   lat=${stop.lat}, lon=${stop.lon}")
            println("DISTANZ: ${Navigate.totalLengthKM} km")
            println("DAUER: ${Navigate.durationSeconds} Sekunden")
            println("DAUER (min): ${Navigate.durationSeconds / 60}")
            println("PUNKTE gesamt: ${Navigate.routePoints.size}")

            val sample = Navigate.routePoints
                .filterIndexed { i, _ -> i % maxOf(1, Navigate.routePoints.size / 10) == 0 }
                .take(10)
            println("--- 10 Routenpunkte (gleichmäßig verteilt) ---")
            sample.forEachIndexed { i, p -> println("  [${i + 1}] lat=${p.latitude}, lon=${p.longitude}") }

            assertTrue("Distanz sollte größer als 0 sein", Navigate.totalLengthKM > 0)
            assertTrue("Route sollte Punkte enthalten", Navigate.routePoints.isNotEmpty())

            val limit = Navigate.getSpeedLimit(start)
            println("SPEED LIMIT (Startpunkt): ${limit ?: "N/A"} km/h")
        } else {
            println("STATUS: Fehler bei der Routenberechnung")
            fail("Routenberechnung fehlgeschlagen")
        }

        println("--- NAVIGATOR TEST ENDE ---")
    }
}
