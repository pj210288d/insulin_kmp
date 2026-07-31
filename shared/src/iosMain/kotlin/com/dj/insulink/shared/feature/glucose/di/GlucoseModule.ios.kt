package com.dj.insulink.shared.feature.glucose.di

import com.dj.insulink.shared.feature.glucose.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.glucose.data.remote.GlucoseRemoteDataSource
import com.dj.insulink.shared.feature.glucose.data.remote.NotImplementedGlucoseRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGlucoseModule(): Module = module {
    single { DatabaseFactory() }
    single<GlucoseRemoteDataSource> { NotImplementedGlucoseRemoteDataSource() }
}
