package com.dj.insulink.shared.feature.glucose.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class DatabaseFactory(private val context: Context) {
    actual fun create(): RoomDatabase.Builder<GlucoseDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(GLUCOSE_DATABASE_FILE_NAME)
        return Room.databaseBuilder(
            context = context.applicationContext,
            name = dbFile.absolutePath
        )
    }
}
