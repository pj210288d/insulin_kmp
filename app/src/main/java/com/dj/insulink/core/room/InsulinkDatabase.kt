package com.dj.insulink.core.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dj.insulink.feature.fitness.data.room.dao.ExerciseDao
import com.dj.insulink.feature.fitness.data.room.entity.ExerciseEntity
import com.dj.insulink.feature.friends.data.room.dao.FriendDao
import com.dj.insulink.feature.friends.data.room.entity.FriendEntity
import com.dj.insulink.feature.reminders.data.room.dao.ReminderDao
import com.dj.insulink.feature.reminders.data.room.entity.ReminderEntity

@Database(
    entities = [
        FriendEntity::class,
        ReminderEntity::class,
        ExerciseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class InsulinkDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
    abstract fun reminderDao(): ReminderDao
    abstract fun exerciseDao(): ExerciseDao
}
