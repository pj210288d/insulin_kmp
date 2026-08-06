package com.dj.insulink.shared.feature.fitness.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.fitness.data.local.dao.ExerciseDao
import com.dj.insulink.shared.feature.fitness.data.local.entity.ExerciseEntity
import kotlinx.coroutines.Dispatchers

internal const val EXERCISE_DATABASE_FILE_NAME = "exercises.db"

@Database(entities = [ExerciseEntity::class], version = 1, exportSchema = false)
@ConstructedBy(ExerciseDatabaseConstructor::class)
abstract class ExerciseDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ExerciseDatabaseConstructor : RoomDatabaseConstructor<ExerciseDatabase> {
    override fun initialize(): ExerciseDatabase
}

fun buildExerciseDatabase(builder: RoomDatabase.Builder<ExerciseDatabase>): ExerciseDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
