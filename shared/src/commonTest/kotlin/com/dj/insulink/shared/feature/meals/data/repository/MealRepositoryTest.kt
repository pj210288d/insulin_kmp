package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.local.entity.IngredientEntity
import com.dj.insulink.shared.feature.meals.data.local.entity.MealEntity
import com.dj.insulink.shared.feature.meals.data.local.entity.MealIngredientEntity
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class MealRepositoryTest {

    private val mealDao = FakeMealDao()
    private val mealIngredientDao = FakeMealIngredientDao()
    private val ingredientDao = FakeIngredientDao()
    private val mealRemoteDataSource = FakeMealRemoteDataSource()
    private val foodApiRemoteDataSource = FakeFoodApiRemoteDataSource()
    private val repository = MealRepository(mealDao, mealIngredientDao, ingredientDao, mealRemoteDataSource, foodApiRemoteDataSource)

    private fun ingredientEntity(id: Long, name: String) = IngredientEntity(
        id = id, name = name, caloriesPer100g = 100.0, proteinPer100g = 10.0,
        carbsPer100g = 20.0, fatPer100g = 5.0, sugarPer100g = 2.0, saltPer100g = 1.0,
        userId = "u1", firebaseId = null, createdAt = 0L
    )

    private fun apiIngredient(name: String) = Ingredient(
        id = 0, name = name, caloriesPer100g = 50.0, proteinPer100g = 1.0,
        carbsPer100g = 1.0, fatPer100g = 1.0, sugarPer100g = 1.0, saltPer100g = 0.1,
        userId = null, firebaseId = null, createdAt = 0L
    )

    @Test
    fun getDailyNutrition_aggregatesDaoTotals() = runTest {
        mealDao.totalCalories = 300
        mealDao.totalCarbs = 40.0
        mealDao.totalProtein = 20.0
        mealDao.totalFat = 10.0
        mealDao.totalSugar = 5.0
        mealDao.totalSalt = 1.5

        assertEquals(DailyNutrition(300, 40, 20, 10, 5, 1.5), repository.getDailyNutrition("u1", 1L))
    }

    @Test
    fun getDailyNutrition_defaultsNullsToZero() = runTest {
        assertEquals(DailyNutrition(0, 0, 0, 0, 0, 0.0), repository.getDailyNutrition("u1", 1L))
    }

    @Test
    fun insertIngredient_storesViaTheIngredientDao() = runTest {
        repository.insertIngredient(apiIngredient("Custom"))
        assertEquals("Custom", ingredientDao.insertedIngredients.single().name)
    }

    @Test
    fun deleteIngredient_removesViaTheIngredientDao() = runTest {
        repository.deleteIngredient(apiIngredient("Custom"))
        assertEquals("Custom", ingredientDao.deletedIngredients.single().name)
    }

    @Test
    fun getUserIngredients_mapsEntitiesToDomain() = runTest {
        ingredientDao.userIngredientsFlow = flowOf(listOf(ingredientEntity(5, "Oats")))

        val result = repository.getUserIngredients("u1").first()

        assertEquals(listOf("Oats"), result.map { it.name })
    }

    @Test
    fun searchIngredients_withAShortQuery_returnsOnlyLocalMatches() = runTest {
        ingredientDao.searchResultsFlow = flowOf(listOf(ingredientEntity(5, "Oats")))

        val result = repository.searchIngredients("oa", "u1").first()

        assertEquals(listOf("Oats"), result.map { it.name })
        assertEquals(emptyList<String>(), foodApiRemoteDataSource.searchedQueries)
    }

    @Test
    fun searchIngredients_mergesUniqueApiResultsForALongerQuery() = runTest {
        ingredientDao.searchResultsFlow = flowOf(listOf(ingredientEntity(5, "Oats")))
        foodApiRemoteDataSource.searchResult = listOf(apiIngredient("Oats"), apiIngredient("Apple"))

        val result = repository.searchIngredients("oats", "u1").first()

        // local "Oats" kept, api "Oats" dropped as duplicate, api "Apple" added
        assertEquals(listOf("Oats", "Apple"), result.map { it.name })
    }

    @Test
    fun searchIngredients_fallsBackToLocalResultsWhenTheApiThrows() = runTest {
        ingredientDao.searchResultsFlow = flowOf(listOf(ingredientEntity(5, "Oats")))
        foodApiRemoteDataSource.throwOnSearch = RuntimeException("network")

        val result = repository.searchIngredients("oats", "u1").first()

        assertEquals(listOf("Oats"), result.map { it.name })
    }

    @Test
    fun getMealsByDateForUser_mapsMealsTogetherWithTheirIngredients() = runTest {
        val mealEntity = MealEntity(
            id = 1, name = "Lunch", timestamp = 1L, calories = 100, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1", firebaseId = null,
            createdAt = 0L, updatedAt = 0L
        )
        mealDao.mealsByDateFlow = flowOf(listOf(mealEntity))
        mealIngredientDao.ingredientsForMealSync = listOf(MealIngredientEntity(id = 10, mealId = 1, ingredientId = 5, quantity = 50.0))
        ingredientDao.ingredientsById = mapOf(5L to ingredientEntity(5, "Oats"))

        val meals = repository.getMealsByDateForUser("u1", 1L).first()

        assertEquals(1, meals.size)
        assertEquals(1, meals.first().ingredients.size)
        assertEquals("Oats", meals.first().ingredients.first().ingredient.name)
    }

    @Test
    fun insert_storesTheMealLocally() = runTest {
        mealDao.insertMealReturns = 99L
        val meal = Meal(
            id = 0, name = "Lunch", timestamp = 1L, calories = 100, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1", ingredients = emptyList()
        )

        repository.insert("u1", meal)

        val inserted = mealDao.insertedMeals.single()
        assertEquals("Lunch", inserted.name)
        assertEquals(true, inserted.id != 0L)
    }

    @Test
    fun delete_removesTheMealAndItsIngredientsLocally() = runTest {
        val meal = Meal(
            id = 3, name = "Lunch", timestamp = 1L, calories = 100, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1"
        )

        repository.delete("u1", meal)

        assertEquals(listOf(3L), mealIngredientDao.deleteIngredientsForMealCalledWith)
        assertEquals(3L, mealDao.deletedMeals.single().id)
    }
}
