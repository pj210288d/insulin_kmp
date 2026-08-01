package com.dj.insulink.shared.feature.meals.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dj.insulink.shared.core.time.currentTimeMillis

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
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
    val firebaseId: String? = null, // For Firebase sync
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
)
