package com.dj.insulink.shared.feature.reminders.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.reminders.domain.model.notificationMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

class BootReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }

        val pendingResult = goAsync()
        val koin = KoinPlatform.getKoin()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val reminderRepository = koin.get<ReminderRepository>()
                val scheduler = koin.get<ReminderNotificationScheduler>()
                val reminders = reminderRepository.getAllReminders()
                reminders.forEach { reminder ->
                    val timeOfDay = localTimeOfDay(reminder.time)
                    runCatching {
                        scheduler.scheduleDaily(
                            reminderId = reminder.id,
                            title = reminder.title,
                            message = reminder.reminderType.notificationMessage(),
                            hour = timeOfDay.hour,
                            minute = timeOfDay.minute
                        )
                    }
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
