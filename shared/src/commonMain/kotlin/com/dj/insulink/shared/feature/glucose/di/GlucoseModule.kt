package com.dj.insulink.shared.feature.glucose.di

import com.dj.insulink.shared.feature.glucose.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.glucose.data.local.GlucoseDatabase
import com.dj.insulink.shared.feature.glucose.data.local.buildGlucoseDatabase
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformGlucoseModule(): Module

val glucoseModule = module {
    includes(platformGlucoseModule())
    single<GlucoseDatabase> { buildGlucoseDatabase(get<DatabaseFactory>().create()) }
    single { get<GlucoseDatabase>().glucoseReadingDao() }
    single { GlucoseReadingRepository(get(), get()) }
}
