package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.remote.MealRemoteDataSource
import com.dj.insulink.shared.feature.meals.domain.model.Meal

class FakeMealRemoteDataSource : MealRemoteDataSource {
    val pushed = mutableListOf<Pair<String, Meal>>()
    val deleted = mutableListOf<Pair<String, Meal>>()
    val updated = mutableListOf<Pair<String, Meal>>()
    var fetchAllResult: List<Meal> = emptyList()

    override suspend fun pushMeal(userId: String, meal: Meal) {
        pushed += userId to meal
    }

    override suspend fun deleteMeal(userId: String, meal: Meal) {
        deleted += userId to meal
    }

    override suspend fun updateMeal(userId: String, meal: Meal) {
        updated += userId to meal
    }

    override suspend fun fetchAllMeals(userId: String): List<Meal> = fetchAllResult
}
