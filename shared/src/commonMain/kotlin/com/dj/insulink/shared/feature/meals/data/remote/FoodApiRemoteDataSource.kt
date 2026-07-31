package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.feature.meals.domain.model.Ingredient

interface FoodApiRemoteDataSource {
    suspend fun searchFoods(query: String): List<Ingredient>
}
