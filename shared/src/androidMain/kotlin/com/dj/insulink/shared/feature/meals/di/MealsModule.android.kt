package com.dj.insulink.shared.feature.meals.di

import com.dj.insulink.shared.feature.meals.data.local.MealsDatabaseFactory
import com.dj.insulink.shared.feature.meals.data.remote.FirebaseMealRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.MealRemoteDataSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMealsModule(): Module = module {
    single { MealsDatabaseFactory(androidContext()) }
    single<MealRemoteDataSource> { FirebaseMealRemoteDataSource(get()) }
}
