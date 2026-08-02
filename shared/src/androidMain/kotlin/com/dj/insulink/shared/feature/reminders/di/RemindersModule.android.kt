package com.dj.insulink.shared.feature.reminders.di

import com.dj.insulink.shared.feature.reminders.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.reminders.data.remote.FirebaseReminderRemoteDataSource
import com.dj.insulink.shared.feature.reminders.data.remote.ReminderRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformRemindersModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single<ReminderRemoteDataSource> { FirebaseReminderRemoteDataSource(get()) }
}
