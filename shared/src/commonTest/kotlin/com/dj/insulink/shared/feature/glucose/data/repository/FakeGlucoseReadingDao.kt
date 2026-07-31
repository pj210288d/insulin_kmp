package com.dj.insulink.shared.feature.glucose.data.repository

import com.dj.insulink.shared.feature.glucose.data.local.dao.GlucoseReadingDao
import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeGlucoseReadingDao : GlucoseReadingDao {
    val insertedEntities = mutableListOf<GlucoseReadingEntity>()
    val deletedEntities = mutableListOf<GlucoseReadingEntity>()
    var insertAllCalledWith: List<GlucoseReadingEntity>? = null
    var deleteAllForUserCalledWith: String? = null

    var allReadingsFlow: Flow<List<GlucoseReadingEntity>> = flowOf(emptyList())
    var dateRangeFlow: Flow<List<GlucoseReadingEntity>> = flowOf(emptyList())
    var earliestTimestamp: Long? = null
    var latestTimestamp: Long? = null

    override fun getAllGlucoseReadingsForUser(userId: String): Flow<List<GlucoseReadingEntity>> = allReadingsFlow

    override suspend fun insert(glucoseReadingEntity: GlucoseReadingEntity): Long {
        insertedEntities += glucoseReadingEntity
        return glucoseReadingEntity.id
    }

    override suspend fun insertAll(readings: List<GlucoseReadingEntity>) {
        insertAllCalledWith = readings
    }

    override suspend fun delete(glucoseReadingEntity: GlucoseReadingEntity) {
        deletedEntities += glucoseReadingEntity
    }

    override suspend fun deleteAllForUser(userId: String) {
        deleteAllForUserCalledWith = userId
    }

    override fun getGlucoseReadingsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<GlucoseReadingEntity>> = dateRangeFlow

    override suspend fun getEarliestTimestamp(userId: String): Long? = earliestTimestamp

    override suspend fun getLatestTimestamp(userId: String): Long? = latestTimestamp
}
