package com.dj.insulink.core.notification

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// setExactAndAllowWhileIdle only ever fires ONCE - without re-arming itself here on every firing,
// a "daily" reminder would only ever notify on its very first scheduled occurrence and then go
// silent forever. Hilt-injected (@AndroidEntryPoint) so it can reach ReminderScheduler directly,
// same pattern as the rest of the app's manifest-registered receivers/services.
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: context.getString(com.dj.insulink.R.string.notification_default_title)
        val message = intent.getStringExtra("message") ?: context.getString(com.dj.insulink.R.string.notification_default_message)
        val notificationId = intent.getIntExtra("notificationId", 0)
        val hour = intent.getIntExtra("hour", -1)
        val minute = intent.getIntExtra("minute", -1)

        val notificationHelper = NotificationHelper(context)
        notificationHelper.showNotification(title, message, notificationId)

        if (hour in 0..23 && minute in 0..59) {
            reminderScheduler.scheduleDaily(
                reminderId = notificationId.toLong(),
                title = title,
                message = message,
                hour = hour,
                minute = minute
            )
        }
    }
}