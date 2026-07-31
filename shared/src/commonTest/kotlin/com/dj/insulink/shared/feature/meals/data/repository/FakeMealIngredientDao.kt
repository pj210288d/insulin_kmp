package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.local.dao.MealIngredientDao
import com.dj.insulink.shared.feature.meals.data.local.entity.MealIngredientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeMealIngredientDao : MealIngredientDao {
    val insertedMealIngredients = mutableListOf<MealIngredientEntity>()
    val deletedMealIngredients = mutableListOf<MealIngredientEntity>()
    val deleteIngredientsForMealCalledWith = mutableListOf<Long>()
    var insertMealIngredientReturns: Long = 0L

    var ingredientsForMealFlow: Flow<List<MealIngredientEntity>> = flowOf(emptyList())
    var ingredientsForMealSync: List<MealIngredientEntity> = emptyList()

    override fun getIngredientsForMeal(mealId: Long): Flow<List<MealIngredientEntity>> = ingredientsForMealFlow

    override suspend fun getIngredientsForMealSync(mealId: Long): List<MealIngredientEntity> = ingredientsForMealSync

    override suspend fun insertMealIngredient(mealIngredient: MealIngredientEntity): Long {
        insertedMealIngredients += mealIngredient
        return insertMealIngredientReturns
    }

    override suspend fun deleteMealIngredient(mealIngredient: MealIngredientEntity) {
        deletedMealIngredients += mealIngredient
    }

    override suspend fun deleteIngredientsForMeal(mealId: Long) {
        deleteIngredientsForMealCalledWith += mealId
    }
}
