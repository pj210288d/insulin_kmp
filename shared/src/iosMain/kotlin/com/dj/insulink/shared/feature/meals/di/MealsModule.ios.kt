package com.dj.insulink.shared.feature.meals.di

import com.dj.insulink.shared.feature.meals.data.local.MealsDatabaseFactory
import com.dj.insulink.shared.feature.meals.data.remote.MealRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.NotImplementedMealRemoteDataSource
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformMealsModule(): Module = module {
    single { MealsDatabaseFactory() }
    single<MealRemoteDataSource> { NotImplementedMealRemoteDataSource() }
}
