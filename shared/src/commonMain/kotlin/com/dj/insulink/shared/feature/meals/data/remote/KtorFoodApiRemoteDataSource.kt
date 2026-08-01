package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

private const val USDA_BASE_URL = "https://api.nal.usda.gov/fdc/v1/foods/search"
private const val SPOONACULAR_BASE_URL = "https://api.spoonacular.com/food/ingredients/search"

class KtorFoodApiRemoteDataSource(
    private val httpClient: HttpClient,
    private val usdaApiKey: String,
    private val spoonacularApiKey: String
) : FoodApiRemoteDataSource {

    override suspend fun searchFoods(query: String): List<Ingredient> {
        if (usdaApiKey.isNotEmpty()) {
            try {
                val response = httpClient.get(USDA_BASE_URL) {
                    parameter("query", query)
                    parameter("pageSize", 10)
                    parameter("api_key", usdaApiKey)
                }

                if (response.status.isSuccess()) {
                    val body = response.body<UsdaFoodSearchResponse>()
                    return convertUsdaFoodItemsToIngredients(body.foods)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (spoonacularApiKey.isNotEmpty()) {
            try {
                val response = httpClient.get(SPOONACULAR_BASE_URL) {
                    parameter("query", query)
                    parameter("number", 10)
                    parameter("addRecipeNutrition", true)
                    parameter("apiKey", spoonacularApiKey)
                }

                if (response.status.isSuccess()) {
                    val body = response.body<FoodSearchResponse>()
                    return convertFoodItemsToIngredients(body.results)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return emptyList()
    }

    private fun convertFoodItemsToIngredients(foodItems: List<FoodItem>): List<Ingredient> {
        return foodItems.mapNotNull { foodItem ->
            val name = foodItem.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val nutrition = foodItem.nutrition?.nutrients ?: emptyList()

            val calories = nutrition.find { it.name.equals("Calories", ignoreCase = true) }?.amount ?: 0.0
            val protein = nutrition.find { it.name.equals("Protein", ignoreCase = true) }?.amount ?: 0.0
            val carbs = nutrition.find { it.name.equals("Carbohydrates", ignoreCase = true) }?.amount ?: 0.0
            val fat = nutrition.find { it.name.equals("Fat", ignoreCase = true) }?.amount ?: 0.0
            val sugar = nutrition.find { it.name.equals("Sugar", ignoreCase = true) }?.amount ?: 0.0
            val salt = nutrition.find { it.name.equals("Sodium", ignoreCase = true) }?.amount ?: 0.0

            Ingredient(
                id = 0L,
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                sugarPer100g = sugar,
                saltPer100g = salt / 1000.0, // mg -> g
                userId = null,
                firebaseId = null,
                createdAt = currentTimeMillis()
            )
        }
    }

    private fun convertUsdaFoodItemsToIngredients(foodItems: List<UsdaFoodItem>): List<Ingredient> {
        return foodItems.mapNotNull { foodItem ->
            val name = foodItem.description?.takeIf { it.isNotBlank() } ?: return@mapNotNull null

            val nutrients = foodItem.foodNutrients ?: emptyList()

            val calories = nutrients.find { it.nutrientId == 1008 }?.value ?: 0.0 // Energy
            val protein = nutrients.find { it.nutrientId == 1003 }?.value ?: 0.0 // Protein
            val carbs = nutrients.find { it.nutrientId == 1005 }?.value ?: 0.0 // Carbohydrates
            val fat = nutrients.find { it.nutrientId == 1004 }?.value ?: 0.0 // Fat
            val sugar = nutrients.find { it.nutrientId == 2000 }?.value ?: 0.0 // Sugars
            val salt = nutrients.find { it.nutrientId == 1093 }?.value ?: 0.0 // Sodium

            Ingredient(
                id = 0L,
                name = name,
                caloriesPer100g = calories,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                sugarPer100g = sugar,
                saltPer100g = salt / 1000.0, // mg -> g
                userId = null,
                firebaseId = null,
                createdAt = currentTimeMillis()
            )
        }
    }
}
