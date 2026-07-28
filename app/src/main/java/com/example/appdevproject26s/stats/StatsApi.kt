package com.example.appdevproject26s.stats

import retrofit2.http.GET

interface StatsApi {

    @GET("stats/summary")
    suspend fun getSummary(): StatsSummary

    @GET("stats/comparison")
    suspend fun getComparison(): StatsComparison

    @GET("stats/trend")
    suspend fun getTrend(@retrofit2.http.Query("days") days: Int): List<TrendPoint>
}
