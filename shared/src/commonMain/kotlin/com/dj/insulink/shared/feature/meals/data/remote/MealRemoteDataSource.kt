package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.feature.meals.domain.model.Meal

interface MealRemoteDataSource {
    suspend fun pushMeal(userId: String, meal: Meal)
    suspend fun deleteMeal(userId: String, meal: Meal)
    suspend fun updateMeal(userId: String, meal: Meal)
    suspend fun fetchAllMeals(userId: String): List<Meal>
}
