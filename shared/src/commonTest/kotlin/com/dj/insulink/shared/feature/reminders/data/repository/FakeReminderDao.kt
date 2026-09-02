package com.dj.insulink.shared.feature.reminders.data.repository

import com.dj.insulink.shared.feature.reminders.data.local.dao.ReminderDao
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeReminderDao : ReminderDao {
    val insertedReminders = mutableListOf<ReminderEntity>()
    val deletedReminders = mutableListOf<ReminderEntity>()
    var insertAllCalledWith: List<ReminderEntity>? = null
    var deleteAllForUserCalledWith: String? = null
    var insertReturns: Long = 0L

    var allRemindersFlow: Flow<List<ReminderEntity>> = flowOf(emptyList())
    var allReminders: List<ReminderEntity> = emptyList()

    override fun getAllRemindersForUser(userId: String): Flow<List<ReminderEntity>> = allRemindersFlow

    override suspend fun getAllReminders(): List<ReminderEntity> = allReminders

    override suspend fun insert(reminderEntity: ReminderEntity): Long {
        insertedReminders += reminderEntity
        return insertReturns
    }

    override suspend fun insertAll(reminders: List<ReminderEntity>) {
        insertAllCalledWith = reminders
    }

    override suspend fun delete(reminderEntity: ReminderEntity) {
        deletedReminders += reminderEntity
    }

    override suspend fun deleteAllForUser(userId: String) {
        deleteAllForUserCalledWith = userId
    }
}
