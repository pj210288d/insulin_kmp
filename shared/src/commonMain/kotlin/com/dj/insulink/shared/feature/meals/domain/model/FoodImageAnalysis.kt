package com.dj.insulink.shared.feature.meals.domain.model

data class FoodImageAnalysis(
    val recognizedFoodNames: List<String>,
    val estimatedIngredient: Ingredient
)

class FoodImageAnalysisException(message: String) : Exception(message)
