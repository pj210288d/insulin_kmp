package com.dj.insulink.shared.feature.glucose.di

import com.dj.insulink.shared.feature.glucose.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.glucose.data.remote.FirebaseGlucoseRemoteDataSource
import com.dj.insulink.shared.feature.glucose.data.remote.GlucoseRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformGlucoseModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single<GlucoseRemoteDataSource> { FirebaseGlucoseRemoteDataSource(get()) }
}
