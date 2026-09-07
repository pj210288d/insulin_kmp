package com.dj.insulink.shared.feature.reminders.data.repository

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.local.dao.ReminderDao
import com.dj.insulink.shared.feature.reminders.data.mapper.toDomain
import com.dj.insulink.shared.feature.reminders.data.mapper.toEntity
import com.dj.insulink.shared.feature.reminders.data.remote.ReminderRemoteDataSource
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.core.dispatcher.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val remoteDataSource: ReminderRemoteDataSource
) {

    fun getAllRemindersForUser(userId: String): Flow<List<Reminder>> {
        return reminderDao.getAllRemindersForUser(userId).map { entities ->
            entities.toDomain().sortedBy { reminder -> localTimeOfDay(reminder.time) }
        }
    }

    suspend fun getAllReminders(): List<Reminder> {
        return withContext(ioDispatcher) {
            reminderDao.getAllReminders().toDomain()
        }
    }

    suspend fun insert(userId: String, reminder: Reminder): Long {
        val reminderWithUniqueId = if (reminder.id == DEFAULT_REMINDER_ID) {
            reminder.copy(id = currentTimeMillis())
        } else {
            reminder
        }

        withContext(ioDispatcher) {
            try {
                reminderDao.insert(reminderWithUniqueId.toEntity())
                remoteDataSource.pushReminder(userId, reminderWithUniqueId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return reminderWithUniqueId.id
    }

    suspend fun delete(userId: String, reminder: Reminder) {
        withContext(ioDispatcher) {
            try {
                reminderDao.delete(reminder.toEntity())
                remoteDataSource.deleteReminder(userId, reminder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllRemindersForUserAndUpdateDatabase(userId: String) {
        withContext(ioDispatcher) {
            val fetchedReminders = remoteDataSource.fetchAllReminders(userId)
            reminderDao.deleteAllForUser(userId)
            reminderDao.insertAll(fetchedReminders.map { it.toEntity() })
        }
    }
}

private const val DEFAULT_REMINDER_ID = 0L
