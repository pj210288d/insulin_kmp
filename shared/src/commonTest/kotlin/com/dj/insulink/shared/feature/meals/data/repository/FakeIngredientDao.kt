package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.local.dao.IngredientDao
import com.dj.insulink.shared.feature.meals.data.local.entity.IngredientEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeIngredientDao : IngredientDao {
    val insertedIngredients = mutableListOf<IngredientEntity>()
    val updatedIngredients = mutableListOf<IngredientEntity>()
    val deletedIngredients = mutableListOf<IngredientEntity>()
    var insertIngredientReturns: Long = 0L

    var searchResultsFlow: Flow<List<IngredientEntity>> = flowOf(emptyList())
    var userIngredientsFlow: Flow<List<IngredientEntity>> = flowOf(emptyList())
    var ingredientsById: Map<Long, IngredientEntity> = emptyMap()

    override fun searchIngredients(query: String, userId: String): Flow<List<IngredientEntity>> = searchResultsFlow

    override fun getUserIngredients(userId: String): Flow<List<IngredientEntity>> = userIngredientsFlow

    override suspend fun getIngredientById(id: Long): IngredientEntity? = ingredientsById[id]

    override suspend fun insertIngredient(ingredient: IngredientEntity): Long {
        insertedIngredients += ingredient
        return insertIngredientReturns
    }

    override suspend fun updateIngredient(ingredient: IngredientEntity) {
        updatedIngredients += ingredient
    }

    override suspend fun deleteIngredient(ingredient: IngredientEntity) {
        deletedIngredients += ingredient
    }
}
