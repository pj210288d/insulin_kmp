package com.dj.insulink.shared.feature.insulin.di

import com.dj.insulink.shared.feature.insulin.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.insulin.data.remote.FirebaseInsulinRemoteDataSource
import com.dj.insulink.shared.feature.insulin.data.remote.InsulinRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformInsulinModule(): Module = module {
    single { DatabaseFactory(androidContext()) }
    single<InsulinRemoteDataSource> { FirebaseInsulinRemoteDataSource(get()) }
}
