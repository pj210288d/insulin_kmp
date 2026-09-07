package com.dj.insulink.shared.feature.reminders.di

import com.dj.insulink.shared.feature.reminders.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.reminders.data.notification.IosReminderNotificationScheduler
import com.dj.insulink.shared.feature.reminders.data.notification.ReminderNotificationScheduler
import com.dj.insulink.shared.feature.reminders.data.remote.FirestoreRestReminderRemoteDataSource
import com.dj.insulink.shared.feature.reminders.data.remote.ReminderRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformRemindersModule(): Module = module {
    single { DatabaseFactory() }
    single<ReminderRemoteDataSource> { FirestoreRestReminderRemoteDataSource(get(), get()) }
    single<ReminderNotificationScheduler> { IosReminderNotificationScheduler() }
}
