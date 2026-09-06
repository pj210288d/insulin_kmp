package com.dj.insulink.shared.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

// Namerno odvojen od feature/librelink/data/remote/HttpClientFactory.kt (koji ima gzip
// content-encoding specifičan za LibreLinkUp-ov backend) - ovaj je opšti core klijent za
// Firebase Identity Toolkit/Firestore REST pozive (core/firestore, feature/auth), da se
// librelink kod uopšte ne dira.
expect fun createCoreHttpClientEngine(): HttpClientEngine

fun createCoreHttpClient(engine: HttpClientEngine = createCoreHttpClientEngine()): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
