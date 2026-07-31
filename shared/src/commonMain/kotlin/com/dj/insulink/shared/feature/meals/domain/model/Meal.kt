package com.dj.insulink.shared.feature.meals.domain.model

import com.dj.insulink.shared.core.time.currentTimeMillis

data class Meal(
    val id: Long = 0,
    val name: String,
    val timestamp: Long,
    val calories: Int?,
    val carbs: Double?,
    val protein: Double?,
    val fat: Double?,
    val sugar: Double?,
    val salt: Double?,
    val comment: String?,
    val userId: String,
    val firebaseId: String? = null,
    val ingredients: List<MealIngredient> = emptyList(),
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
)

data class MealIngredient(
    val id: Long = 0,
    val mealId: Long,
    val ingredient: Ingredient,
    val quantity: Double, // in grams
    val firebaseId: String? = null,
    val createdAt: Long = currentTimeMillis()
)

data class Ingredient(
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val sugarPer100g: Double,
    val saltPer100g: Double,
    val userId: String? = null, // null for system ingredients, userId for custom ingredients
    val firebaseId: String? = null,
    val createdAt: Long = currentTimeMillis()
)

data class DailyNutrition(
    val calories: Int,
    val carbs: Int,
    val protein: Int,
    val fat: Int,
    val sugar: Int,
    val salt: Double
)
