package com.example.appdevproject26s.steps

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // one shared instance for whole app
class StepsRepository @Inject constructor(

    // Hilt gives app's context automatically
    @ApplicationContext private val context: Context,

    // Hilt gives API to send steps to server
    private val stepsApi: StepsApi
) {
    // Need permission to read step count
    val stepsPermission: String = HealthPermission.getReadPermission(StepsRecord::class)

    // Create Health Connect client
    private val healthConnectClient = HealthConnectClient.getOrCreate(context)

    // Check if Health Connect is installed
    fun isHealthConnectAvailable(): Boolean {
        val status = HealthConnectClient.getSdkStatus(context)
        return status == HealthConnectClient.SDK_AVAILABLE
    }

    // Check if user approved permission
    suspend fun hasStepsPermission(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.contains(stepsPermission)
    }

    // Get today's step count
    suspend fun syncTodaySteps() {

        // Time range (From midnight until now)
        val today = LocalDate.now()
        val zoneId = ZoneId.systemDefault()
        val startOfDay = today.atStartOfDay(zoneId).toInstant()
        val now = Instant.now()
        val timeRange = TimeRangeFilter.between(startOfDay, now)

        // Add up all steps in time range
        val request = AggregateRequest(metrics = setOf(StepsRecord.COUNT_TOTAL), timeRangeFilter = timeRange)
        val result = healthConnectClient.aggregate(request)

        // If there are no steps yet, use 0
        val totalSteps = result[StepsRecord.COUNT_TOTAL]
        val steps: Int = totalSteps?.toInt() ?: 0

        // Send to backend
        val reportRequest = ReportStepsRequest(date = today.toString(), steps = steps)
        stepsApi.report(reportRequest)
    }
}