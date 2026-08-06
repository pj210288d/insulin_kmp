package com.dj.insulink.shared.feature.reminders.data.remote

import com.dj.insulink.shared.feature.reminders.domain.model.Reminder

interface ReminderRemoteDataSource {
    suspend fun pushReminder(userId: String, reminder: Reminder)
    suspend fun deleteReminder(userId: String, reminder: Reminder)
    suspend fun fetchAllReminders(userId: String): List<Reminder>
}
