package com.dj.insulink.shared.feature.reminders.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE userId = :userId")
    fun getAllRemindersForUser(userId: String): Flow<List<ReminderEntity>>

    @Insert
    suspend fun insert(reminderEntity: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<ReminderEntity>)

    @Delete
    suspend fun delete(reminderEntity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
