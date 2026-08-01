package com.dj.insulink.shared.feature.meals.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dj.insulink.shared.feature.meals.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {

    @Query("SELECT * FROM ingredients WHERE name LIKE '%' || :query || '%' AND (userId IS NULL OR userId = :userId) ORDER BY name ASC")
    fun searchIngredients(query: String, userId: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE userId = :userId ORDER BY name ASC")
    fun getUserIngredients(userId: String): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getIngredientById(id: Long): IngredientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredient(ingredient: IngredientEntity): Long

    @Update
    suspend fun updateIngredient(ingredient: IngredientEntity)

    @Delete
    suspend fun deleteIngredient(ingredient: IngredientEntity)
}
