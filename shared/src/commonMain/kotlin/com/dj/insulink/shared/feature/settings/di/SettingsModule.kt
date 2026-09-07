package com.dj.insulink.shared.feature.settings.di

import com.dj.insulink.shared.feature.settings.ui.viewmodel.SettingsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformSettingsModule(): Module

val settingsModule = module {
    includes(platformSettingsModule())
    single { SettingsViewModel(get()) }
}
