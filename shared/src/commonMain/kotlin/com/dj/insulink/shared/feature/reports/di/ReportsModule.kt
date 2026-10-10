package com.dj.insulink.shared.feature.reports.di

import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel
import com.dj.insulink.shared.feature.settings.di.settingsModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformReportsModule(): Module

val reportsModule = module {
    includes(platformReportsModule())
    includes(settingsModule)
    single { ReportsViewModel(get(), get(), get(), get()) }
}
