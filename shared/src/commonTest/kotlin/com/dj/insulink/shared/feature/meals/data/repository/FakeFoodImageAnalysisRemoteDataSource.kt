package com.dj.insulink.shared.feature.meals.data.repository

import com.dj.insulink.shared.feature.meals.data.remote.FoodImageAnalysisRemoteDataSource
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient

class FakeFoodImageAnalysisRemoteDataSource : FoodImageAnalysisRemoteDataSource {
    var analysisResult: FoodImageAnalysis = FoodImageAnalysis(
        recognizedFoodNames = listOf("tomato"),
        estimatedIngredient = Ingredient(name = "tomato", caloriesPer100g = 20.0, proteinPer100g = 1.0, carbsPer100g = 4.0, fatPer100g = 0.2, sugarPer100g = 2.0, saltPer100g = 0.0)
    )
    var throwOnAnalyze: Throwable? = null
    val analyzedImages = mutableListOf<ByteArray>()

    override suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodImageAnalysis {
        analyzedImages += imageBytes
        throwOnAnalyze?.let { throw it }
        return analysisResult
    }
}
