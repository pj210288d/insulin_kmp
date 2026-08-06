package com.dj.insulink.shared.feature.insulin.di

import com.dj.insulink.shared.feature.insulin.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.insulin.data.remote.InsulinRemoteDataSource
import com.dj.insulink.shared.feature.insulin.data.remote.NotImplementedInsulinRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformInsulinModule(): Module = module {
    single { DatabaseFactory() }
    single<InsulinRemoteDataSource> { NotImplementedInsulinRemoteDataSource() }
}
