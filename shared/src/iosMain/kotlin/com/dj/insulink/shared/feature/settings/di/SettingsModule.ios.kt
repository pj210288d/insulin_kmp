package com.dj.insulink.shared.feature.settings.di

import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformSettingsModule(): Module = module {
    single { SettingsPreferences() }
}
