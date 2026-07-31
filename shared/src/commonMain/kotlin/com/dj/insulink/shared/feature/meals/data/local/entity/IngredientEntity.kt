package com.dj.insulink.shared.feature.meals.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dj.insulink.shared.core.time.currentTimeMillis

@Entity(tableName = "ingredients")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val sugarPer100g: Double,
    val saltPer100g: Double,
    val userId: String? = null, // null for system ingredients, userId for custom ingredients
    val firebaseId: String? = null, // For Firebase sync
    val createdAt: Long = currentTimeMillis()
)
