package com.dj.insulink.shared.feature.glucose.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.glucose.data.local.dao.GlucoseReadingDao
import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import kotlinx.coroutines.Dispatchers

internal const val GLUCOSE_DATABASE_FILE_NAME = "glucose_readings.db"

@Database(entities = [GlucoseReadingEntity::class], version = 1, exportSchema = false)
@ConstructedBy(GlucoseDatabaseConstructor::class)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseReadingDao(): GlucoseReadingDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object GlucoseDatabaseConstructor : RoomDatabaseConstructor<GlucoseDatabase> {
    override fun initialize(): GlucoseDatabase
}

fun buildGlucoseDatabase(builder: RoomDatabase.Builder<GlucoseDatabase>): GlucoseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
