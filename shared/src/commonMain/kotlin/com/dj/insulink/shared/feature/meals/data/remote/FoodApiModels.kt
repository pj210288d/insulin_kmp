package com.dj.insulink.shared.feature.meals.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FoodSearchResponse(
    @SerialName("results")
    val results: List<FoodItem> = emptyList()
)

@Serializable
data class FoodItem(
    @SerialName("id")
    val id: Int = 0,
    @SerialName("title")
    val title: String? = null,
    @SerialName("image")
    val image: String? = null,
    @SerialName("imageType")
    val imageType: String? = null,
    @SerialName("nutrition")
    val nutrition: FoodNutrition? = null
)

@Serializable
data class FoodNutrition(
    @SerialName("nutrients")
    val nutrients: List<Nutrient> = emptyList()
)

@Serializable
data class Nutrient(
    @SerialName("name")
    val name: String = "",
    @SerialName("amount")
    val amount: Double = 0.0,
    @SerialName("unit")
    val unit: String = ""
)

@Serializable
data class UsdaFoodSearchResponse(
    @SerialName("foods")
    val foods: List<UsdaFoodItem> = emptyList()
)

@Serializable
data class UsdaFoodItem(
    @SerialName("fdcId")
    val fdcId: Int = 0,
    @SerialName("description")
    val description: String? = null,
    @SerialName("dataType")
    val dataType: String? = null,
    @SerialName("foodNutrients")
    val foodNutrients: List<UsdaFoodNutrient>? = null
)

@Serializable
data class UsdaFoodNutrient(
    @SerialName("nutrientId")
    val nutrientId: Int = 0,
    @SerialName("nutrientName")
    val nutrientName: String = "",
    @SerialName("value")
    val value: Double = 0.0,
    @SerialName("unitName")
    val unitName: String = ""
)
