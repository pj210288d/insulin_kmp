package com.dj.insulink.shared.feature.insulin.di

import com.dj.insulink.shared.feature.insulin.data.local.DatabaseFactory
import com.dj.insulink.shared.feature.insulin.data.local.InsulinDatabase
import com.dj.insulink.shared.feature.insulin.data.local.buildInsulinDatabase
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.ui.viewmodel.InsulinViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformInsulinModule(): Module

val insulinModule = module {
    includes(platformInsulinModule())
    single<InsulinDatabase> { buildInsulinDatabase(get<DatabaseFactory>().create()) }
    single { get<InsulinDatabase>().insulinTypeDao() }
    single { InsulinTypeRepository(get(), get()) }
    single { InsulinViewModel(get()) }
}
