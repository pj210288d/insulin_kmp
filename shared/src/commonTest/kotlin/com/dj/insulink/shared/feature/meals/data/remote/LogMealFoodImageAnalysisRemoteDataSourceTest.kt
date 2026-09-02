package com.dj.insulink.shared.feature.meals.data.remote

import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysisException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class LogMealFoodImageAnalysisRemoteDataSourceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private val segmentationBody = """
        {"imageId":2125842,"segmentation_results":[
            {"food_item_position":1,"recognition_results":[{"name":"tomato","prob":0.41}]},
            {"food_item_position":2,"recognition_results":[{"name":"chicken curry","prob":0.32}]}
        ]}
    """.trimIndent()

    private val nutritionBody = """
        {"hasNutritionalInfo":true,"nutritional_info":{"totalNutrients":{
            "ENERC_KCAL":{"label":"Energy","quantity":347.0,"unit":"kcal"},
            "PROCNT":{"label":"Protein","quantity":22.0,"unit":"g"},
            "CHOCDF":{"label":"Carbs","quantity":28.0,"unit":"g"},
            "FAT":{"label":"Fat","quantity":14.0,"unit":"g"},
            "SUGAR":{"label":"Sugars","quantity":5.0,"unit":"g"},
            "NA":{"label":"Sodium","quantity":900.0,"unit":"mg"}
        }}}
    """.trimIndent()

    private fun dataSource(
        apiKey: String = "logmeal-key",
        handler: (isSegmentation: Boolean) -> Pair<HttpStatusCode, String>?
    ): LogMealFoodImageAnalysisRemoteDataSource {
        val engine = MockEngine { request ->
            val isSegmentation = request.url.encodedPath.contains("segmentation")
            val result = handler(isSegmentation) ?: return@MockEngine respondError(HttpStatusCode.NotFound)
            respond(result.second, result.first, jsonHeaders)
        }
        return LogMealFoodImageAnalysisRemoteDataSource(createHttpClient(engine), apiKey)
    }

    @Test
    fun successfulAnalysis_mapsRecognizedNamesAndConvertsSodiumToGrams() = runTest {
        val dataSource = dataSource { isSegmentation ->
            if (isSegmentation) HttpStatusCode.OK to segmentationBody else HttpStatusCode.OK to nutritionBody
        }

        val result = dataSource.analyzeFoodImage(byteArrayOf(1, 2, 3))

        assertEquals(listOf("tomato", "chicken curry"), result.recognizedFoodNames)
        val ingredient = result.estimatedIngredient
        assertEquals("tomato, chicken curry", ingredient.name)
        assertEquals(347.0, ingredient.caloriesPer100g, 0.0001)
        assertEquals(22.0, ingredient.proteinPer100g, 0.0001)
        assertEquals(28.0, ingredient.carbsPer100g, 0.0001)
        assertEquals(14.0, ingredient.fatPer100g, 0.0001)
        assertEquals(5.0, ingredient.sugarPer100g, 0.0001)
        assertEquals(0.9, ingredient.saltPer100g, 0.0001) // 900 mg -> 0.9 g
    }

    @Test
    fun missingNutrients_defaultToZero() = runTest {
        val emptyNutritionBody = """{"hasNutritionalInfo":false,"nutritional_info":{"totalNutrients":{}}}"""
        val dataSource = dataSource { isSegmentation ->
            if (isSegmentation) HttpStatusCode.OK to segmentationBody else HttpStatusCode.OK to emptyNutritionBody
        }

        val ingredient = dataSource.analyzeFoodImage(byteArrayOf(1)).estimatedIngredient

        assertEquals(0.0, ingredient.caloriesPer100g, 0.0001)
        assertEquals(0.0, ingredient.saltPer100g, 0.0001)
    }

    @Test
    fun throws_whenApiKeyIsEmpty() = runTest {
        val dataSource = dataSource(apiKey = "") { HttpStatusCode.OK to segmentationBody }

        assertFailsWith<FoodImageAnalysisException> {
            dataSource.analyzeFoodImage(byteArrayOf(1))
        }
    }

    @Test
    fun throws_whenSegmentationCallFails() = runTest {
        val dataSource = dataSource { isSegmentation ->
            if (isSegmentation) HttpStatusCode.BadRequest to """{"message":"Could not analyze food image"}""" else null
        }

        assertFailsWith<FoodImageAnalysisException> {
            dataSource.analyzeFoodImage(byteArrayOf(1))
        }
    }

    @Test
    fun throws_whenNoFoodIsRecognized() = runTest {
        val emptyResultsBody = """{"imageId":1,"segmentation_results":[]}"""
        val dataSource = dataSource { isSegmentation ->
            if (isSegmentation) HttpStatusCode.OK to emptyResultsBody else null
        }

        assertFailsWith<FoodImageAnalysisException> {
            dataSource.analyzeFoodImage(byteArrayOf(1))
        }
    }

    @Test
    fun throws_whenNutritionLookupFails() = runTest {
        val dataSource = dataSource { isSegmentation ->
            if (isSegmentation) HttpStatusCode.OK to segmentationBody else HttpStatusCode.InternalServerError to "boom"
        }

        assertFailsWith<FoodImageAnalysisException> {
            dataSource.analyzeFoodImage(byteArrayOf(1))
        }
    }
}
