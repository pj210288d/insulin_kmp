package com.dj.insulink.shared.feature.statistics.di

import com.dj.insulink.shared.feature.glucose.di.glucoseModule
import com.dj.insulink.shared.feature.settings.di.settingsModule
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import org.koin.dsl.module

// Statistika nema svoj lokalni DB/remote sloj (čiste funkcije nad GlucoseReadingRepository -
// vidi StatisticsCalculator), zato ovaj modul samo uključuje glucoseModule/settingsModule
// (idempotentno, isti obrazac kao GlucoseModule.kt) i registruje ViewModel.
val statisticsModule = module {
    includes(glucoseModule, settingsModule)
    single { StatisticsViewModel(get(), get()) }
}
