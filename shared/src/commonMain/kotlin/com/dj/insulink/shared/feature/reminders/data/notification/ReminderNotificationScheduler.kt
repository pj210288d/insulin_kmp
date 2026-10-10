package com.dj.insulink.shared.feature.reminders.data.notification

interface ReminderNotificationScheduler {
    suspend fun scheduleDaily(reminderId: Long, title: String, message: String, hour: Int, minute: Int)
    fun cancelReminder(reminderId: Long)
}
