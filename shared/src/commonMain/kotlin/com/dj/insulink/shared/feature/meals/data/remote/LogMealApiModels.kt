package com.dj.insulink.shared.feature.meals.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogMealSegmentationResponse(
    @SerialName("imageId")
    val imageId: Long = 0,
    @SerialName("segmentation_results")
    val segmentationResults: List<LogMealSegmentationResult> = emptyList()
)

@Serializable
data class LogMealSegmentationResult(
    @SerialName("food_item_position")
    val foodItemPosition: Int = 0,
    @SerialName("recognition_results")
    val recognitionResults: List<LogMealRecognitionResult> = emptyList()
)

@Serializable
data class LogMealRecognitionResult(
    @SerialName("name")
    val name: String = "",
    @SerialName("prob")
    val probability: Double = 0.0
)

@Serializable
data class LogMealImageIdRequest(
    @SerialName("imageId")
    val imageId: Long
)

@Serializable
data class LogMealNutritionalInfoResponse(
    @SerialName("hasNutritionalInfo")
    val hasNutritionalInfo: Boolean = false,
    @SerialName("nutritional_info")
    val nutritionalInfo: LogMealNutritionalInfo? = null
)

@Serializable
data class LogMealNutritionalInfo(
    @SerialName("totalNutrients")
    val totalNutrients: Map<String, LogMealNutrientValue> = emptyMap()
)

@Serializable
data class LogMealNutrientValue(
    @SerialName("label")
    val label: String = "",
    @SerialName("quantity")
    val quantity: Double = 0.0,
    @SerialName("unit")
    val unit: String = ""
)
