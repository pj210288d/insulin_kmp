package com.dj.insulink.shared.feature.librelink.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createHttpClientEngine(): HttpClientEngine

fun createHttpClient(engine: HttpClientEngine = createHttpClientEngine()): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        // LibreLinkUp's backend appears to require an explicit "accept-encoding: gzip" header
        // (see KtorLibreLinkApiClient.baseHeaders). Setting that header manually disables
        // OkHttp's own transparent gzip decompression (it only auto-decompresses when it adds
        // the header itself), so Ktor's own decompression plugin handles it instead.
        install(ContentEncoding) {
            gzip()
        }
    }
}
