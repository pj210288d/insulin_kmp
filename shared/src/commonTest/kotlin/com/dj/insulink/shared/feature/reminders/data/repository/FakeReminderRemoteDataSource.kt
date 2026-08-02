package com.dj.insulink.shared.feature.reminders.data.repository

import com.dj.insulink.shared.feature.reminders.data.remote.ReminderRemoteDataSource
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder

class FakeReminderRemoteDataSource : ReminderRemoteDataSource {
    val pushed = mutableListOf<Pair<String, Reminder>>()
    val deleted = mutableListOf<Pair<String, Reminder>>()
    var fetchAllResult: List<Reminder> = emptyList()

    override suspend fun pushReminder(userId: String, reminder: Reminder) {
        pushed += userId to reminder
    }

    override suspend fun deleteReminder(userId: String, reminder: Reminder) {
        deleted += userId to reminder
    }

    override suspend fun fetchAllReminders(userId: String): List<Reminder> = fetchAllResult
}
