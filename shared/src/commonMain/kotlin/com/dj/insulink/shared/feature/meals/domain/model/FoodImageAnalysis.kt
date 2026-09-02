package com.dj.insulink.shared.feature.meals.domain.model

/**
 * Result of running a meal photo through an AI food-recognition service (LogMeal).
 *
 * [estimatedIngredient] packages the whole photographed plate as a single [Ingredient] whose
 * "per100g" fields actually hold the estimated totals for the WHOLE photographed portion. This
 * mirrors how manually created custom ingredients already work in this app (see
 * CreateIngredientDialog): the UI adds it to a meal with a default quantity of 100g, which makes
 * the existing `caloriesPer100g * quantity / 100` math pass the totals through unchanged, while
 * still letting the user scale the quantity down/up (e.g. "I only ate half of this plate").
 */
data class FoodImageAnalysis(
    val recognizedFoodNames: List<String>,
    val estimatedIngredient: Ingredient
)

class FoodImageAnalysisException(message: String) : Exception(message)
