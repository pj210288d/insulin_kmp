package com.dj.insulink.shared.feature.glucose.data.repository

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.glucose.data.local.dao.GlucoseReadingDao
import com.dj.insulink.shared.feature.glucose.data.mapper.toDomain
import com.dj.insulink.shared.feature.glucose.data.mapper.toEntity
import com.dj.insulink.shared.feature.glucose.data.remote.GlucoseRemoteDataSource
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class GlucoseReadingRepository(
    private val glucoseReadingDao: GlucoseReadingDao,
    private val remoteDataSource: GlucoseRemoteDataSource
) {

    fun getAllGlucoseReadingsForUser(userId: String): Flow<List<GlucoseReading>> {
        return glucoseReadingDao.getAllGlucoseReadingsForUser(userId).map {
            it.toDomain()
        }
    }

    fun getGlucoseReadingsByDateRange(
        userId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<GlucoseReading>> {
        return glucoseReadingDao.getGlucoseReadingsByDateRange(userId, startDate, endDate).map {
            it.toDomain()
        }
    }

    suspend fun getDateRange(userId: String): Pair<Long?, Long?> {
        return withContext(Dispatchers.IO) {
            val minDate = glucoseReadingDao.getEarliestTimestamp(userId)
            val maxDate = glucoseReadingDao.getLatestTimestamp(userId)
            Pair(minDate, maxDate)
        }
    }

    suspend fun insert(userId: String, reading: GlucoseReading) {
        withContext(Dispatchers.IO) {
            try {
                val readingWithUniqueId = if (reading.id == 0L) {
                    reading.copy(id = currentTimeMillis())
                } else {
                    reading
                }

                glucoseReadingDao.insert(readingWithUniqueId.toEntity())
                remoteDataSource.pushReading(userId, readingWithUniqueId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun delete(userId: String, reading: GlucoseReading) {
        withContext(Dispatchers.IO) {
            try {
                glucoseReadingDao.delete(reading.toEntity())
                remoteDataSource.deleteReading(userId, reading)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId: String) {
        withContext(Dispatchers.IO) {
            val fetchedReadings = remoteDataSource.fetchAllReadings(userId)
            glucoseReadingDao.deleteAllForUser(userId)
            glucoseReadingDao.insertAll(fetchedReadings.map { it.toEntity() })
        }
    }
}
