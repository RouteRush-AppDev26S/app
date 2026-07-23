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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StepsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val stepsApi: StepsApi
) {
    val readStepsPermission: String = HealthPermission.getReadPermission(StepsRecord::class)

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    fun isHealthConnectAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasStepsPermission(): Boolean =
        healthConnectClient.permissionController.getGrantedPermissions().contains(readStepsPermission)

    suspend fun syncTodaySteps() {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

        val result = healthConnectClient.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startOfDay, Instant.now())
            )
        )
        val steps = (result[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()

        stepsApi.report(ReportStepsRequest(date = today.toString(), steps = steps))
    }
}