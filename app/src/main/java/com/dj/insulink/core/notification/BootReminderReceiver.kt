package com.dj.insulink.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.dj.insulink.feature.reminders.ui.displayNameRes
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every locally cached reminder's AlarmManager alarm after a device reboot or app
 * update. AlarmManager entries survive neither, so without this reminders would silently stop
 * firing until the user happened to add a brand new one (reopening the Reminders screen does NOT
 * re-schedule existing reminders - only [ReminderReceiver]'s own "re-arm tomorrow" logic and this
 * receiver do).
 */
@AndroidEntryPoint
class BootReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderRepository: ReminderRepository

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminders = reminderRepository.getAllReminders()
                reminders.forEach { reminder ->
                    val localTime = Instant.ofEpochMilli(reminder.time)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                    reminderScheduler.scheduleDaily(
                        reminderId = reminder.id,
                        title = reminder.title,
                        message = context.getString(reminder.reminderType.displayNameRes),
                        hour = localTime.hour,
                        minute = localTime.minute
                    )
                }
                Log.d(TAG, "Re-armed ${reminders.size} reminder(s) after boot/update")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-arm reminders after boot/update", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReminderReceiver"
    }
}
