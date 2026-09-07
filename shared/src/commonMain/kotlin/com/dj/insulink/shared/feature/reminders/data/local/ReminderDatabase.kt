package com.dj.insulink.shared.feature.reminders.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dj.insulink.shared.feature.reminders.data.local.dao.ReminderDao
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import com.dj.insulink.shared.core.dispatcher.ioDispatcher

internal const val REMINDER_DATABASE_FILE_NAME = "reminders.db"

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
@ConstructedBy(ReminderDatabaseConstructor::class)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
}

// Room's KSP compiler generates the platform `actual` for this object.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object ReminderDatabaseConstructor : RoomDatabaseConstructor<ReminderDatabase> {
    override fun initialize(): ReminderDatabase
}

fun buildReminderDatabase(builder: RoomDatabase.Builder<ReminderDatabase>): ReminderDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .build()
}
