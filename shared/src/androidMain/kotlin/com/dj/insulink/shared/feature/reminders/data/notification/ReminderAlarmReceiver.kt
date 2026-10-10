package com.dj.insulink.shared.feature.reminders.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

internal const val REMINDER_ACTION = "com.dj.insulink.REMINDER_ACTION"
internal const val EXTRA_TITLE = "title"
internal const val EXTRA_MESSAGE = "message"
internal const val EXTRA_NOTIFICATION_ID = "notificationId"
internal const val EXTRA_HOUR = "hour"
internal const val EXTRA_MINUTE = "minute"
private const val CHANNEL_ID = "reminder_channel"

class ReminderAlarmReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Podsetnik"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Imate podsetnik!"
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)

        showReminderNotification(context, title, message, notificationId)

        if (hour in 0..23 && minute in 0..59) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val scheduler = KoinPlatform.getKoin().get<ReminderNotificationScheduler>()
                    scheduler.scheduleDaily(
                        reminderId = notificationId.toLong(),
                        title = title,
                        message = message,
                        hour = hour,
                        minute = minute
                    )
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

@RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
internal fun showReminderNotification(context: Context, title: String, message: String, notificationId: Int) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Dnevni podsetnici",
        NotificationManager.IMPORTANCE_HIGH
    )
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(context.applicationInfo.icon)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(notificationId, notification)
}
