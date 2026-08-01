package com.dj.insulink.shared.feature.meals.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dj.insulink.shared.feature.meals.data.local.entity.MealIngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealIngredientDao {

    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId")
    fun getIngredientsForMeal(mealId: Long): Flow<List<MealIngredientEntity>>

    @Query("SELECT * FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun getIngredientsForMealSync(mealId: Long): List<MealIngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealIngredient(mealIngredient: MealIngredientEntity): Long

    @Delete
    suspend fun deleteMealIngredient(mealIngredient: MealIngredientEntity)

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteIngredientsForMeal(mealId: Long)
}
