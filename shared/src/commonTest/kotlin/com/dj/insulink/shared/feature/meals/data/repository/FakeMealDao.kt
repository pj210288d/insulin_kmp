package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.local.dao.MealDao
import com.dj.insulink.shared.feature.meals.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMealDao : MealDao {
    val insertedMeals = mutableListOf<MealEntity>()
    val updatedMeals = mutableListOf<MealEntity>()
    val deletedMeals = mutableListOf<MealEntity>()
    var insertAllCalledWith: List<MealEntity>? = null
    var deleteAllForUserCalledWith: String? = null
    var insertMealReturns: Long = 0L

    var allMealsFlow: Flow<List<MealEntity>> = flowOf(emptyList())
    var mealsByDateFlow: Flow<List<MealEntity>> = flowOf(emptyList())
    var totalCalories: Int? = null
    var totalCarbs: Double? = null
    var totalProtein: Double? = null
    var totalFat: Double? = null
    var totalSugar: Double? = null
    var totalSalt: Double? = null

    override fun getAllMeals(userId: String): Flow<List<MealEntity>> = allMealsFlow

    override fun getMealsByDate(userId: String, date: Long): Flow<List<MealEntity>> = mealsByDateFlow

    override suspend fun insertMeal(meal: MealEntity): Long {
        insertedMeals += meal
        return insertMealReturns
    }

    override suspend fun updateMeal(meal: MealEntity) {
        updatedMeals += meal
    }

    override suspend fun deleteMeal(meal: MealEntity) {
        deletedMeals += meal
    }

    override suspend fun getTotalCaloriesForDate(userId: String, date: Long): Int? = totalCalories
    override suspend fun getTotalCarbsForDate(userId: String, date: Long): Double? = totalCarbs
    override suspend fun getTotalProteinForDate(userId: String, date: Long): Double? = totalProtein
    override suspend fun getTotalFatForDate(userId: String, date: Long): Double? = totalFat
    override suspend fun getTotalSugarForDate(userId: String, date: Long): Double? = totalSugar
    override suspend fun getTotalSaltForDate(userId: String, date: Long): Double? = totalSalt

    override suspend fun insertAll(meals: List<MealEntity>) {
        insertAllCalledWith = meals
    }

    override suspend fun deleteAllForUser(userId: String) {
        deleteAllForUserCalledWith = userId
    }
}
