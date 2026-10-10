package com.dj.insulink.shared.feature.glucose.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.dj.insulink.shared.feature.glucose.data.local.dao.GlucoseReadingDao
import com.dj.insulink.shared.feature.glucose.data.local.entity.GlucoseReadingEntity
import com.dj.insulink.shared.core.dispatcher.ioDispatcher

internal const val GLUCOSE_DATABASE_FILE_NAME = "glucose_readings.db"

@Database(entities = [GlucoseReadingEntity::class], version = 2, exportSchema = false)
@ConstructedBy(GlucoseDatabaseConstructor::class)
abstract class GlucoseDatabase : RoomDatabase() {
    abstract fun glucoseReadingDao(): GlucoseReadingDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object GlucoseDatabaseConstructor : RoomDatabaseConstructor<GlucoseDatabase> {
    override fun initialize(): GlucoseDatabase
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE glucose_readings ADD COLUMN insulinTypeId INTEGER")
        connection.execSQL("ALTER TABLE glucose_readings ADD COLUMN insulinUnits REAL")
        connection.execSQL("ALTER TABLE glucose_readings ADD COLUMN linkedMealId INTEGER")
    }
}

fun buildGlucoseDatabase(builder: RoomDatabase.Builder<GlucoseDatabase>): GlucoseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .addMigrations(MIGRATION_1_2)
        .build()
}
