package com.dj.insulink.shared.feature.meals.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dj.insulink.shared.feature.meals.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meals WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllMeals(userId: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch') ORDER BY timestamp DESC")
    fun getMealsByDate(userId: String, date: Long): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("SELECT SUM(calories) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalCaloriesForDate(userId: String, date: Long): Int?

    @Query("SELECT SUM(carbs) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalCarbsForDate(userId: String, date: Long): Double?

    @Query("SELECT SUM(protein) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalProteinForDate(userId: String, date: Long): Double?

    @Query("SELECT SUM(fat) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalFatForDate(userId: String, date: Long): Double?

    @Query("SELECT SUM(sugar) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalSugarForDate(userId: String, date: Long): Double?

    @Query("SELECT SUM(salt) FROM meals WHERE userId = :userId AND date(timestamp/1000, 'unixepoch') = date(:date/1000, 'unixepoch')")
    suspend fun getTotalSaltForDate(userId: String, date: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(meals: List<MealEntity>)

    @Query("DELETE FROM meals WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
