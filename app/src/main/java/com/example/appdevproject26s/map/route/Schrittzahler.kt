package com.example.appdevproject26s.map.route

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import android.util.Log

@Singleton
class Schrittzahler @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    var schritte: Int by mutableStateOf(0)
        private set

    private var initialSteps: Int = -1

    fun start() {
        Log.d("Schrittzahler", "Starting sensor... Sensor available: ${stepCounterSensor != null}")
        stepCounterSensor?.let {
            val registered = sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            Log.d("Schrittzahler", "Sensor registered: $registered")
        }
    }

    fun stop() {
        Log.d("Schrittzahler", "Stopping sensor")
        sensorManager.unregisterListener(this)
        initialSteps = -1
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            Log.d("Schrittzahler", "Sensor changed: totalSteps=$totalSteps")
            
            if (initialSteps == -1) {
                initialSteps = totalSteps
                Log.d("Schrittzahler", "Initial steps set to $initialSteps")
            }
            
            schritte = totalSteps - initialSteps
            Log.d("Schrittzahler", "Calculated steps: $schritte")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
