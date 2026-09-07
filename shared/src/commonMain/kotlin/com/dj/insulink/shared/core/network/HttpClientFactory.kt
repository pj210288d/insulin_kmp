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
            // encodeDefaults = true je BITNO ovde: request telima (npr. EmailPasswordRequest u
            // FirebaseAuthRestClient) je "returnSecureToken: Boolean = true" default vrednost -
            // bez ovoga kotlinx.serialization to polje TIHO IZOSTAVLJA sa žice (ne šalje ga
            // uopšte), a Firebase Identity Toolkit's signInWithPassword bez eksplicitnog
            // returnSecureToken:true vraća odgovor BEZ refreshToken/expiresIn (200 OK, ali
            // "krnji" - potvrđeno curl testom 2026-09-06, pravi uzrok "Fields [refreshToken,
            // expiresIn] required" pucanja na loginu). signUp se slučajno nije lomio jer novi
            // nalog uvek dobija refresh token bez obzira na to polje - zato se bug primetio tek
            // na login, ne na registraciju.
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }
}
