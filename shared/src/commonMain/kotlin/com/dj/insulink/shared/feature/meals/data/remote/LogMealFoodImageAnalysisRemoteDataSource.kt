package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysisException
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
import io.ktor.http.contentType
import io.ktor.http.isSuccess

private const val SEGMENTATION_URL = "https://api.logmeal.com/v2/image/segmentation/complete"
private const val NUTRITIONAL_INFO_URL = "https://api.logmeal.com/v2/nutrition/recipe/nutritionalInfo"

// LogMeal's totalNutrients keys, confirmed against a live response (see dnevnik.md).
private const val NUTRIENT_CALORIES = "ENERC_KCAL"
private const val NUTRIENT_PROTEIN = "PROCNT"
private const val NUTRIENT_CARBS = "CHOCDF"
private const val NUTRIENT_FAT = "FAT"
private const val NUTRIENT_SUGAR = "SUGAR"
private const val NUTRIENT_SODIUM = "NA" // sodium, in mg

/**
 * Recognizes food in a photo and estimates its nutritional content using the LogMeal API
 * (https://logmeal.com/api/). Two calls are required: image segmentation/recognition first
 * returns an `imageId`, which is then used to fetch the nutritional breakdown.
 */
class LogMealFoodImageAnalysisRemoteDataSource(
    private val httpClient: HttpClient,
    private val apiKey: String
) : FoodImageAnalysisRemoteDataSource {

    override suspend fun analyzeFoodImage(imageBytes: ByteArray): FoodImageAnalysis {
        if (apiKey.isEmpty()) {
            throw FoodImageAnalysisException("LogMeal API key is not configured")
        }

        val segmentationResponse = httpClient.submitFormWithBinaryData(
            url = SEGMENTATION_URL,
            formData = formData {
                append(
                    key = "image",
                    value = imageBytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"meal.jpg\"")
                    }
                )
            }
        ) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
        }

        if (!segmentationResponse.status.isSuccess()) {
            throw FoodImageAnalysisException(
                "LogMeal recognition failed (${segmentationResponse.status.value}): ${segmentationResponse.bodyAsText()}"
            )
        }

        val segmentation = segmentationResponse.body<LogMealSegmentationResponse>()
        val recognizedNames = segmentation.segmentationResults
            .mapNotNull { result -> result.recognitionResults.firstOrNull()?.name }
            .distinct()

        if (recognizedNames.isEmpty()) {
            throw FoodImageAnalysisException("No food was recognized in the photo")
        }

        val nutritionResponse = httpClient.post(NUTRITIONAL_INFO_URL) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(LogMealImageIdRequest(imageId = segmentation.imageId))
        }

        if (!nutritionResponse.status.isSuccess()) {
            throw FoodImageAnalysisException(
                "LogMeal nutrition lookup failed (${nutritionResponse.status.value}): ${nutritionResponse.bodyAsText()}"
            )
        }

        val nutritionInfo = nutritionResponse.body<LogMealNutritionalInfoResponse>()
        val nutrients = nutritionInfo.nutritionalInfo?.totalNutrients ?: emptyMap()

        val estimatedIngredient = Ingredient(
            name = recognizedNames.joinToString(", "),
            caloriesPer100g = nutrients[NUTRIENT_CALORIES]?.quantity ?: 0.0,
            proteinPer100g = nutrients[NUTRIENT_PROTEIN]?.quantity ?: 0.0,
            carbsPer100g = nutrients[NUTRIENT_CARBS]?.quantity ?: 0.0,
            fatPer100g = nutrients[NUTRIENT_FAT]?.quantity ?: 0.0,
            sugarPer100g = nutrients[NUTRIENT_SUGAR]?.quantity ?: 0.0,
            saltPer100g = (nutrients[NUTRIENT_SODIUM]?.quantity ?: 0.0) / 1000.0, // mg -> g
            userId = null,
            firebaseId = null,
            createdAt = currentTimeMillis()
        )

        return FoodImageAnalysis(
            recognizedFoodNames = recognizedNames,
            estimatedIngredient = estimatedIngredient
        )
    }
}
