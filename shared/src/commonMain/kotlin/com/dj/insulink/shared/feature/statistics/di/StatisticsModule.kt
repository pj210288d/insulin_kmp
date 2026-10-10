package com.dj.insulink.shared.feature.statistics.di

import com.dj.insulink.shared.feature.glucose.di.glucoseModule
import com.dj.insulink.shared.feature.settings.di.settingsModule
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import org.koin.dsl.module

val statisticsModule = module {
    includes(glucoseModule, settingsModule)
    single { StatisticsViewModel(get(), get()) }
}
