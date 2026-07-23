
package com.example.appdevproject26s.tracking

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
// Hilt kann die Klasse jetzt problemlos instanziieren
class Timer @Inject constructor() {

    private var timerThread: Thread? = null
    var elapsedTimeInSeconds = 0L
        public set

    // Hier speichern wir den Callback temporär ab
    private var currentOnTick: ((Long) -> Unit)? = null

    fun start(onTick: (Long) -> Unit) {
        this.currentOnTick = onTick
        if (timerThread != null) return // läuft bereits

        timerThread = Thread {
            try {
                while (!Thread.currentThread().isInterrupted) {
                    Thread.sleep(1000L)
                    elapsedTimeInSeconds++

                    // Callback sicher aufrufen
                    currentOnTick?.invoke(elapsedTimeInSeconds)
                }
            } catch (e: InterruptedException) {
                // Sauberes Beenden
            }
        }.apply {
            start()
        }
    }

    fun stop() {
        timerThread?.interrupt()
        timerThread = null
        elapsedTimeInSeconds = 0L
        currentOnTick = null
    }
}
