package com.dj.insulink.shared.feature.reports.di

import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel
import com.dj.insulink.shared.feature.settings.di.settingsModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformReportsModule(): Module

// glucoseModule se ne includes-uje ovde eksplicitno (isti razlog kao Faza 1 auth/glucose -
// GlucoseReadingRepository je već registrovan negde drugde u istom startKoin pozivu preko
// statisticsModule -> glucoseModule uključivanja, get() rešava iz zajedničkog kontejnera).
val reportsModule = module {
    includes(platformReportsModule())
    includes(settingsModule)
    single { ReportsViewModel(get(), get(), get(), get()) }
}
