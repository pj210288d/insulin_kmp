package com.dj.insulink.shared.feature.meals.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dj.insulink.shared.core.time.currentTimeMillis

@Entity(tableName = "meal_ingredients")
data class MealIngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val ingredientId: Long,
    val quantity: Double, // in grams
    val firebaseId: String? = null, // For Firebase sync
    val createdAt: Long = currentTimeMillis()
)
