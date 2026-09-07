package com.dj.insulink.shared.feature.reminders.data.notification

// Vidi opširan komentar u commonMain ReminderNotificationScheduler.kt - namerno no-op na
// Android-u. Android-ov pravi Reminders ekran (van :shared) već ima potpuno funkcionalne
// notifikacije preko sopstvenog AlarmManager lanca.
class AndroidReminderNotificationScheduler : ReminderNotificationScheduler {
    override suspend fun scheduleDaily(reminderId: Long, title: String, message: String, hour: Int, minute: Int) {
        // no-op - vidi komentar u interfejsu
    }

    override fun cancelReminder(reminderId: Long) {
        // no-op - vidi komentar u interfejsu
    }
}
