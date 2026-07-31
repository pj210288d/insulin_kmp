package com.dj.insulink.shared.feature.meals.data.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class KtorFoodApiRemoteDataSourceTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun dataSource(
        usdaApiKey: String = "usda-key",
        spoonacularApiKey: String = "",
        handler: (isUsda: Boolean) -> Pair<HttpStatusCode, String>?
    ): Pair<KtorFoodApiRemoteDataSource, MockEngine> {
        val engine = MockEngine { request ->
            val isUsda = request.url.encodedPath.contains("fdc/v1/foods/search")
            val result = handler(isUsda)
                ?: return@MockEngine respondError(HttpStatusCode.NotFound)
            respond(result.second, result.first, jsonHeaders)
        }
        val dataSource = KtorFoodApiRemoteDataSource(createHttpClient(engine), usdaApiKey, spoonacularApiKey)
        return dataSource to engine
    }

    @Test
    fun usdaSuccess_mapsNutrientsById_andConvertsSodiumToGrams() = runTest {
        val body = """
            {"foods":[{"description":"Banana","foodNutrients":[
                {"nutrientId":1008,"value":89.0},
                {"nutrientId":1003,"value":1.1},
                {"nutrientId":1005,"value":22.8},
                {"nutrientId":1004,"value":0.3},
                {"nutrientId":2000,"value":12.2},
                {"nutrientId":1093,"value":1000.0}
            ]}]}
        """.trimIndent()
        val (dataSource, _) = dataSource { isUsda -> if (isUsda) HttpStatusCode.OK to body else null }

        val result = dataSource.searchFoods("banana")

        assertEquals(1, result.size)
        val ingredient = result.first()
        assertEquals("Banana", ingredient.name)
        assertEquals(89.0, ingredient.caloriesPer100g, 0.0001)
        assertEquals(1.1, ingredient.proteinPer100g, 0.0001)
        assertEquals(22.8, ingredient.carbsPer100g, 0.0001)
        assertEquals(0.3, ingredient.fatPer100g, 0.0001)
        assertEquals(12.2, ingredient.sugarPer100g, 0.0001)
        assertEquals(1.0, ingredient.saltPer100g, 0.0001) // 1000 mg -> 1.0 g
    }

    @Test
    fun usdaItemsWithNullOrBlankNames_areDropped() = runTest {
        val body = """{"foods":[{"description":null},{"description":"   "},{"description":"Apple"}]}"""
        val (dataSource, _) = dataSource { isUsda -> if (isUsda) HttpStatusCode.OK to body else null }

        val result = dataSource.searchFoods("a")

        assertEquals(listOf("Apple"), result.map { it.name })
    }

    @Test
    fun usdaMissingNutrients_defaultToZero() = runTest {
        val body = """{"foods":[{"description":"Water"}]}"""
        val (dataSource, _) = dataSource { isUsda -> if (isUsda) HttpStatusCode.OK to body else null }

        val ingredient = dataSource.searchFoods("water").first()

        assertEquals(0.0, ingredient.caloriesPer100g, 0.0001)
        assertEquals(0.0, ingredient.saltPer100g, 0.0001)
    }

    @Test
    fun fallsBackToSpoonacular_whenUsdaResponseIsUnsuccessful() = runTest {
        val spoonacularBody = """
            {"results":[{"title":"Egg","nutrition":{"nutrients":[
                {"name":"Calories","amount":155.0},
                {"name":"Protein","amount":13.0},
                {"name":"Carbohydrates","amount":1.1},
                {"name":"Fat","amount":11.0},
                {"name":"Sugar","amount":1.1},
                {"name":"Sodium","amount":124.0}
            ]}}]}
        """.trimIndent()
        val (dataSource, _) = dataSource(spoonacularApiKey = "spoon-key") { isUsda ->
            if (isUsda) HttpStatusCode.InternalServerError to "boom" else HttpStatusCode.OK to spoonacularBody
        }

        val result = dataSource.searchFoods("egg")

        assertEquals(1, result.size)
        val ingredient = result.first()
        assertEquals("Egg", ingredient.name)
        assertEquals(155.0, ingredient.caloriesPer100g, 0.0001)
        assertEquals(0.124, ingredient.saltPer100g, 0.0001) // 124 mg -> 0.124 g
    }

    @Test
    fun fallsBackToSpoonacular_whenUsdaCallThrows() = runTest {
        val engine = MockEngine { request ->
            val isUsda = request.url.encodedPath.contains("fdc/v1/foods/search")
            if (isUsda) throw RuntimeException("network")
            respond(
                """{"results":[{"title":"Rice"}]}""",
                HttpStatusCode.OK,
                jsonHeaders
            )
        }
        val dataSource = KtorFoodApiRemoteDataSource(createHttpClient(engine), "usda-key", "spoon-key")

        val result = dataSource.searchFoods("rice")

        assertEquals(listOf("Rice"), result.map { it.name })
    }

    @Test
    fun returnsEmptyList_whenBothProvidersFail() = runTest {
        val (dataSource, _) = dataSource(spoonacularApiKey = "spoon-key") { HttpStatusCode.InternalServerError to "boom" }

        assertTrue(dataSource.searchFoods("nothing").isEmpty())
    }

    @Test
    fun returnsEmptyList_whenNoApiKeysAreConfigured() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respondError(HttpStatusCode.NotFound)
        }
        val dataSource = KtorFoodApiRemoteDataSource(createHttpClient(engine), usdaApiKey = "", spoonacularApiKey = "")

        assertTrue(dataSource.searchFoods("anything").isEmpty())
        assertEquals(0, callCount)
    }
}
