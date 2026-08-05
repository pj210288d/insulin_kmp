package com.dj.insulink.shared.feature.insulin.data.local

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<InsulinDatabase>
}
