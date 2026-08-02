package com.dj.insulink.shared.feature.reminders.di

import com.dj.insulink.shared.feature.reminders.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.reminders.data.local.ReminderDatabase
import com.dj.insulink.shared.feature.reminders.data.local.buildReminderDatabase
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformRemindersModule(): Module

val remindersModule = module {
    includes(platformRemindersModule())
    single<ReminderDatabase> { buildReminderDatabase(get<DatabaseFactory>().create()) }
    single { get<ReminderDatabase>().reminderDao() }
    single { ReminderRepository(get(), get()) }
}
