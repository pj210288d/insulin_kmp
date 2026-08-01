package com.dj.insulink.shared.feature.fitness.data.local

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<ExerciseDatabase>
}
