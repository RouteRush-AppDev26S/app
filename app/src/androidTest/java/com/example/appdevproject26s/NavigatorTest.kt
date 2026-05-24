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

/**
 * Test für den Navigator, der Ausgaben in die Konsole/Logcat schreibt.
 * Dieser Test muss auf einem Emulator oder physischen Gerät ausgeführt werden.
 */
@RunWith(AndroidJUnit4::class)
class NavigatorTest {

    @Before
    fun setup() {
        // Initialisiere den Navigator mit dem Test-Context für den Datenbankzugriff
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Navigate.init(appContext)
    }

    @Test
    fun testNavigatorOutput() = runBlocking {
        // Beispiel-Koordinaten: Klagenfurt nach Villach
        val start = VLocation(lat = 46.6247, lon = 14.3053)
        val stop = VLocation(lat = 46.6103, lon = 13.8558)
        
        println("--- NAVIGATOR TEST START ---")
        println("Route: Klagenfurt -> Villach")
        
        val trip = Navigate.calcRoute(start, stop, "auto")
        
        if (trip != null) {
            println("STATUS: Erfolg")
            println("DISTANZ: ${Navigate.totalLengthKM} km")
            println("DAUER: ${Navigate.durationSeconds} Sekunden")
            println("DAUER (min): ${Navigate.durationSeconds / 60}")
            println("PUNKTE: ${Navigate.routePoints.size}")
            
            // Verifikation
            assertTrue("Distanz sollte größer als 0 sein", Navigate.totalLengthKM > 0)
            assertTrue("Route sollte Punkte enthalten", Navigate.routePoints.isNotEmpty())
            
            // Speed Limit Test
            val limit = Navigate.getSpeedLimit(start)
            println("SPEED LIMIT (Startpunkt): ${limit ?: "N/A"} km/h")
        } else {
            println("STATUS: Fehler bei der Routenberechnung")
            fail("Routenberechnung fehlgeschlagen")
        }
        
        println("--- NAVIGATOR TEST ENDE ---")
    }
}
