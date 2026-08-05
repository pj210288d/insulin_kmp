package com.dj.insulink.shared.feature.insulin.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<InsulinDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(INSULIN_DATABASE_FILE_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}
