package com.dj.insulink.shared.feature.reminders.data.local

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<ReminderDatabase>
}
