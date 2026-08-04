package com.dj.insulink.shared.feature.glucose.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlucoseReadingDao {

    @Query("SELECT * FROM glucose_readings WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllGlucoseReadingsForUser(userId: String): Flow<List<GlucoseReadingEntity>>

    @Insert
    suspend fun insert(glucoseReadingEntity: GlucoseReadingEntity): Long

    @Update
    suspend fun update(glucoseReadingEntity: GlucoseReadingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<GlucoseReadingEntity>)

    @Delete
    suspend fun delete(glucoseReadingEntity: GlucoseReadingEntity)

    @Query("DELETE FROM glucose_readings WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    @Query("SELECT * FROM glucose_readings WHERE userId = :userId AND timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp ASC")
    fun getGlucoseReadingsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<GlucoseReadingEntity>>

    @Query("SELECT MIN(timestamp) FROM glucose_readings WHERE userId = :userId")
    suspend fun getEarliestTimestamp(userId: String): Long?

    @Query("SELECT MAX(timestamp) FROM glucose_readings WHERE userId = :userId")
    suspend fun getLatestTimestamp(userId: String): Long?
}
