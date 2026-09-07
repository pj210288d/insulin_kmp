package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.meals.data.local.dao.IngredientDao
import com.dj.insulink.shared.feature.meals.data.local.dao.MealDao
import com.dj.insulink.shared.feature.meals.data.local.dao.MealIngredientDao
import com.dj.insulink.shared.feature.meals.data.mapper.toDomain
import com.dj.insulink.shared.feature.meals.data.mapper.toEntity
import com.dj.insulink.shared.feature.meals.data.remote.FoodApiRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.FoodImageAnalysisRemoteDataSource
import com.dj.insulink.shared.feature.meals.data.remote.MealRemoteDataSource
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import com.dj.insulink.shared.core.dispatcher.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MealRepository(
    private val mealDao: MealDao,
    private val mealIngredientDao: MealIngredientDao,
    private val ingredientDao: IngredientDao,
    private val mealRemoteDataSource: MealRemoteDataSource,
    private val foodApiRemoteDataSource: FoodApiRemoteDataSource,
    private val foodImageAnalysisRemoteDataSource: FoodImageAnalysisRemoteDataSource
) {

    fun getAllMealsForUser(userId: String): Flow<List<Meal>> {
        return mealDao.getAllMeals(userId).map {
            it.map { mealEntity ->
                val ingredients = getMealIngredients(mealEntity.id)
                mealEntity.toDomain(ingredients)
            }
        }
    }

    fun getMealsByDateForUser(userId: String, date: Long): Flow<List<Meal>> {
        return mealDao.getMealsByDate(userId, date).map {
            it.map { mealEntity ->
                val ingredients = getMealIngredients(mealEntity.id)
                mealEntity.toDomain(ingredients)
            }
        }
    }

    suspend fun insert(userId: String, meal: Meal) {
        withContext(ioDispatcher) {
            try {
                val mealWithUniqueId = if (meal.id == 0L) {
                    meal.copy(id = currentTimeMillis())
                } else {
                    meal
                }

                val mealId = mealDao.insertMeal(mealWithUniqueId.toEntity())

                mealWithUniqueId.ingredients.forEach { mealIngredient ->
                    val ingredientEntity = mealIngredient.ingredient.toEntity()
                    val ingredientId = if (ingredientEntity.id == 0L) {
                        ingredientDao.insertIngredient(ingredientEntity)
                    } else {
                        ingredientEntity.id
                    }

                    val mealIngredientEntity = mealIngredient.copy(
                        mealId = mealId,
                        ingredient = mealIngredient.ingredient.copy(id = ingredientId)
                    ).toEntity()

                    mealIngredientDao.insertMealIngredient(mealIngredientEntity)
                }

                mealRemoteDataSource.pushMeal(userId, mealWithUniqueId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun delete(userId: String, meal: Meal) {
        withContext(ioDispatcher) {
            try {
                mealIngredientDao.deleteIngredientsForMeal(meal.id)
                mealDao.deleteMeal(meal.toEntity())
                mealRemoteDataSource.deleteMeal(userId, meal)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun update(userId: String, meal: Meal) {
        withContext(ioDispatcher) {
            try {
                mealDao.updateMeal(meal.toEntity())

                mealIngredientDao.deleteIngredientsForMeal(meal.id)
                meal.ingredients.forEach { mealIngredient ->
                    val ingredientEntity = mealIngredient.ingredient.toEntity()
                    val ingredientId = if (ingredientEntity.id == 0L) {
                        ingredientDao.insertIngredient(ingredientEntity)
                    } else {
                        ingredientEntity.id
                    }

                    val mealIngredientEntity = mealIngredient.copy(
                        mealId = meal.id,
                        ingredient = mealIngredient.ingredient.copy(id = ingredientId)
                    ).toEntity()

                    mealIngredientDao.insertMealIngredient(mealIngredientEntity)
                }

                mealRemoteDataSource.updateMeal(userId, meal)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun fetchAllMealsForUserAndUpdateDatabase(userId: String) {
        withContext(ioDispatcher) {
            val fetchedMeals = mealRemoteDataSource.fetchAllMeals(userId)
            mealDao.deleteAllForUser(userId)
            mealDao.insertAll(fetchedMeals.map { it.toEntity() })
        }
    }

    suspend fun getDailyNutrition(userId: String, date: Long): DailyNutrition {
        val calories = mealDao.getTotalCaloriesForDate(userId, date) ?: 0
        val carbs = mealDao.getTotalCarbsForDate(userId, date) ?: 0.0
        val protein = mealDao.getTotalProteinForDate(userId, date) ?: 0.0
        val fat = mealDao.getTotalFatForDate(userId, date) ?: 0.0
        val sugar = mealDao.getTotalSugarForDate(userId, date) ?: 0.0
        val salt = mealDao.getTotalSaltForDate(userId, date) ?: 0.0

        return DailyNutrition(
            calories = calories,
            carbs = carbs.toInt(),
            protein = protein.toInt(),
            fat = fat.toInt(),
            sugar = sugar.toInt(),
            salt = salt
        )
    }

    fun searchIngredients(query: String, userId: String): Flow<List<Ingredient>> {
        return ingredientDao.searchIngredients(query, userId)
            .map { entities -> entities.map { it.toDomain() } }
            .map { localIngredients ->
                if (query.length < 3) {
                    return@map localIngredients
                }

                try {
                    val apiIngredients = foodApiRemoteDataSource.searchFoods(query)
                    val localNames = localIngredients.map { it.name.lowercase() }.toSet()
                    val uniqueApiIngredients = apiIngredients.filter {
                        it.name.lowercase() !in localNames
                    }
                    localIngredients + uniqueApiIngredients
                } catch (e: Exception) {
                    localIngredients
                }
            }
    }

    suspend fun insertIngredient(ingredient: Ingredient) {
        ingredientDao.insertIngredient(ingredient.toEntity())
    }

    fun getUserIngredients(userId: String): Flow<List<Ingredient>> {
        return ingredientDao.getUserIngredients(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun deleteIngredient(ingredient: Ingredient) {
        ingredientDao.deleteIngredient(ingredient.toEntity())
    }

    suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodImageAnalysis {
        return withContext(ioDispatcher) {
            foodImageAnalysisRemoteDataSource.analyzeFoodImage(imageBytes)
        }
    }

    private suspend fun getMealIngredients(mealId: Long): List<MealIngredient> {
        val mealIngredientEntities = mealIngredientDao.getIngredientsForMealSync(mealId)
        return mealIngredientEntities.mapNotNull { mealIngredientEntity ->
            val ingredient = ingredientDao.getIngredientById(mealIngredientEntity.ingredientId)?.toDomain()
            ingredient?.let { mealIngredientEntity.toDomain(it) }
        }
    }
}
