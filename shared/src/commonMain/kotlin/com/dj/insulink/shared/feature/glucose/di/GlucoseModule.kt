package com.dj.insulink.shared.feature.glucose.di

import com.dj.insulink.shared.feature.glucose.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.glucose.data.local.GlucoseDatabase
import com.dj.insulink.shared.feature.glucose.data.local.buildGlucoseDatabase
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.settings.di.settingsModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformGlucoseModule(): Module

val glucoseModule = module {
    includes(platformGlucoseModule())
    // settingsModule se ovde eksplicitno uključuje jer GlucoseViewModel (deljeni Compose
    // Multiplatform MVP ekran - vidi tu klasu) zavisi od SettingsPreferences. Bezbedno je
    // uključiti ga i ovde i na mestu gde se startKoin poziva - Koin modul-inclusion je
    // idempotentan po instanci modula.
    includes(settingsModule)
    single<GlucoseDatabase> { buildGlucoseDatabase(get<DatabaseFactory>().create()) }
    single { get<GlucoseDatabase>().glucoseReadingDao() }
    single { GlucoseReadingRepository(get(), get()) }
    single { GlucoseViewModel(get(), get()) }
}
