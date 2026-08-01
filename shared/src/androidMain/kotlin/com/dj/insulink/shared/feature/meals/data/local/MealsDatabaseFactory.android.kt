package com.dj.insulink.shared.feature.meals.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class MealsDatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<MealsDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(MEALS_DATABASE_FILE_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}
