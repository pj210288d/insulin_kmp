package com.dj.insulink.shared.feature.reminders.data.remote

import com.dj.insulink.shared.feature.reminders.domain.model.Reminder

class NotImplementedReminderRemoteDataSource : ReminderRemoteDataSource {
    override suspend fun pushReminder(userId: String, reminder: Reminder): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun deleteReminder(userId: String, reminder: Reminder): Unit =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")

    override suspend fun fetchAllReminders(userId: String): List<Reminder> =
        throw NotImplementedError("Firestore sync nije implementiran za iOS (faza 4)")
}
