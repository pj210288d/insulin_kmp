package com.dj.insulink.shared.feature.meals.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClientEngine(): HttpClientEngine

fun createHttpClient(engine: HttpClientEngine = createHttpClientEngine()): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
