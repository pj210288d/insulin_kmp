package com.dj.insulink.shared.feature.meals.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class MealsDatabaseFactory {
    @OptIn(ExperimentalForeignApi::class)
    actual fun create(): RoomDatabase.Builder<MealsDatabase> {
        val documentDirectory = requireNotNull(
            NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        )
        val dbFilePath = documentDirectory.path + "/$MEALS_DATABASE_FILE_NAME"
        return Room.databaseBuilder<MealsDatabase>(name = dbFilePath)
    }
}
