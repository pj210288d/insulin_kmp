package com.dj.insulink.shared.feature.auth.data.remote

import com.dj.insulink.shared.core.network.createCoreHttpClient
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val GOOGLE_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"

/**
 * Razmenjuje Google OAuth authorization code (+ PKCE code_verifier - vidi GoogleSignInCoordinator,
 * iOS-only) za Google-ov id_token preko Google-ovog SOPSTVENOG token endpoint-a (ne
 * Firebase-ovog - taj id_token se tek ODATLE prosleđuje Firebase-u preko
 * FirebaseAuthRestClient.signInWithGoogleIdToken). "Public" klijent (iOS app, bez client
 * secret-a) - PKCE je dovoljan dokaz vlasništva nad authorization code-om umesto secret-a.
 */
class GoogleTokenExchangeClient(
    private val httpClient: HttpClient = createCoreHttpClient()
) {
    suspend fun exchangeCodeForIdToken(
        code: String,
        codeVerifier: String,
        clientId: String,
        redirectUri: String
    ): String {
        val response = httpClient.submitForm(
            url = GOOGLE_TOKEN_ENDPOINT,
            formParameters = Parameters.build {
                append("code", code)
                append("client_id", clientId)
                append("redirect_uri", redirectUri)
                append("grant_type", "authorization_code")
                append("code_verifier", codeVerifier)
            }
        )
        val rawBody = response.bodyAsText()
        val json = runCatching { Json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
        if (!response.status.isSuccess()) {
            val message = json?.get("error_description")?.jsonPrimitive?.contentOrNull
                ?: json?.get("error")?.jsonPrimitive?.contentOrNull
            throw AuthException(message ?: "Google token exchange nije uspeo (${response.status})")
        }
        return json?.get("id_token")?.jsonPrimitive?.contentOrNull
            ?: throw AuthException("Google token odgovor ne sadrži id_token")
    }
}
