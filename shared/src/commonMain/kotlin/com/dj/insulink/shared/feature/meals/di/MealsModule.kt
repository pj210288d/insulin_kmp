package com.dj.insulink.shared.feature.meals.di

import com.dj.insulink.shared.feature.meals.data.local.MealsDatabase
import com.dj.insulink.shared.feature.meals.data.local.MealsDatabaseFactory
import com.dj.insulink.shared.feature.meals.data.local.buildMealsDatabase
import com.dj.insulink.shared.feature.meals.data.remote.FoodApiRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.FoodImageAnalysisRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.KtorFoodApiRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.LogMealFoodImageAnalysisRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.createHttpClient
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

expect fun platformMealsModule(): Module

fun mealsModule(usdaApiKey: String, spoonacularApiKey: String, logMealApiKey: String): Module = module {
    includes(platformMealsModule())
    single<MealsDatabase> { buildMealsDatabase(get<MealsDatabaseFactory>().create()) }
    single { get<MealsDatabase>().mealDao() }
    single { get<MealsDatabase>().ingredientDao() }
    single { get<MealsDatabase>().mealIngredientDao() }
    single { createHttpClient() }
    single<FoodApiRemoteDataSource> {
        KtorFoodApiRemoteDataSource(get(), usdaApiKey = usdaApiKey, spoonacularApiKey = spoonacularApiKey)
    }
    single<FoodImageAnalysisRemoteDataSource> {
        LogMealFoodImageAnalysisRemoteDataSource(get(), apiKey = logMealApiKey)
    }
    single { MealRepository(get(), get(), get(), get(), get(), get()) }
    single { MealsViewModel(get()) }
}
