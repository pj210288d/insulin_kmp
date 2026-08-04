package com.dj.insulink.shared.feature.glucose.data.repository

import com.dj.insulink.shared.feature.glucose.data.remote.GlucoseRemoteDataSource
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading

class FakeGlucoseRemoteDataSource : GlucoseRemoteDataSource {
    val pushed = mutableListOf<Pair<String, GlucoseReading>>()
    val updated = mutableListOf<Pair<String, GlucoseReading>>()
    val deleted = mutableListOf<Pair<String, GlucoseReading>>()
    var fetchAllResult: List<GlucoseReading> = emptyList()

    override suspend fun pushReading(userId: String, reading: GlucoseReading) {
        pushed += userId to reading
    }

    override suspend fun updateReading(userId: String, reading: GlucoseReading) {
        updated += userId to reading
    }

    override suspend fun deleteReading(userId: String, reading: GlucoseReading) {
        deleted += userId to reading
    }

    override suspend fun fetchAllReadings(userId: String): List<GlucoseReading> = fetchAllResult
}
