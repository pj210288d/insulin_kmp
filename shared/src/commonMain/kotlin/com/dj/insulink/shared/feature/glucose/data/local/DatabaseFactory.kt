package com.dj.insulink.shared.feature.glucose.data.local

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<GlucoseDatabase>
}
