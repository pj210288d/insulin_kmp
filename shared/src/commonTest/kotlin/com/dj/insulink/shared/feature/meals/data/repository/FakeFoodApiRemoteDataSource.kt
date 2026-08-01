package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.remote.FoodApiRemoteDataSource
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient

class FakeFoodApiRemoteDataSource : FoodApiRemoteDataSource {
    var searchResult: List<Ingredient> = emptyList()
    var throwOnSearch: Throwable? = null
    val searchedQueries = mutableListOf<String>()

    override suspend fun searchFoods(query: String): List<Ingredient> {
        searchedQueries += query
        throwOnSearch?.let { throw it }
        return searchResult
    }
}
