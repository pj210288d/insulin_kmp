package com.dj.insulink.shared.feature.reminders.data.notification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosReminderNotificationScheduler : ReminderNotificationScheduler {

    override suspend fun scheduleDaily(reminderId: Long, title: String, message: String, hour: Int, minute: Int) {
        requestAuthorizationIfNeeded()

        val content = UNMutableNotificationContent()
        content.setTitle(title)
        content.setBody(message)

        val dateComponents = NSDateComponents()
        dateComponents.hour = hour.toLong()
        dateComponents.minute = minute.toLong()

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = true
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = reminderId.toString(),
            content = content,
            trigger = trigger
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { }
    }

    override fun cancelReminder(reminderId: Long) {
        UNUserNotificationCenter.currentNotificationCenter()
            .removePendingNotificationRequestsWithIdentifiers(listOf(reminderId.toString()))
    }

    private suspend fun requestAuthorizationIfNeeded() {
        suspendCancellableCoroutine<Unit> { continuation ->
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { _, _ ->
                    if (continuation.isActive) continuation.resume(Unit)
                }
        }
    }
}
