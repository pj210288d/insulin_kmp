package com.dj.insulink.shared.feature.glucose.data.remote

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading

interface GlucoseRemoteDataSource {
    suspend fun pushReading(userId: String, reading: GlucoseReading)
    suspend fun deleteReading(userId: String, reading: GlucoseReading)
    suspend fun fetchAllReadings(userId: String): List<GlucoseReading>
}
