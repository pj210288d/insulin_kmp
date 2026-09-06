package com.dj.insulink.shared.feature.auth.data.remote

import com.dj.insulink.shared.core.config.FIREBASE_WEB_API_KEY
import com.dj.insulink.shared.core.network.createCoreHttpClient
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val IDENTITY_TOOLKIT_BASE = "https://identitytoolkit.googleapis.com/v1"
private const val SECURE_TOKEN_BASE = "https://securetoken.googleapis.com/v1"

/** Dovoljno da TokenStorage (iOS actual) sačuva sesiju i kasnije je obnovi. */
data class FirebaseAuthTokens(
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val uid: String
)

// Ručno pisan Ktor klijent za Firebase Identity Toolkit REST API (login/registracija/reset
// lozinke/verifikacija emaila/refresh tokena) - koristi ga SAMO iOS Auth actual (Faza 1 plan,
// arhitektonska odluka: REST umesto GitLive Firebase KMP, da se izbegne CocoaPods i Kotlin/
// Native ABI rizik - vidi CLAUDE.md gotcha #5). Android i dalje ide preko pravog Firebase GMS
// SDK-a, nepromenjeno.
class FirebaseAuthRestClient(
    private val httpClient: HttpClient = createCoreHttpClient()
) {
    suspend fun signIn(email: String, password: String): FirebaseAuthTokens {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:signInWithPassword") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(EmailPasswordRequest(email, password))
        }
        return parseAuthResponse(response)
    }

    suspend fun signUp(email: String, password: String): FirebaseAuthTokens {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:signUp") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(EmailPasswordRequest(email, password))
        }
        return parseAuthResponse(response)
    }

    suspend fun updateProfile(idToken: String, displayName: String) {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:update") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(idToken = idToken, displayName = displayName))
        }
        requireSuccess(response)
    }

    suspend fun sendEmailVerification(idToken: String) {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:sendOobCode") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(OobCodeRequest(requestType = "VERIFY_EMAIL", idToken = idToken))
        }
        requireSuccess(response)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:sendOobCode") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(OobCodeRequest(requestType = "PASSWORD_RESET", email = email))
        }
        requireSuccess(response)
    }

    suspend fun refreshToken(refreshToken: String): FirebaseAuthTokens {
        val response = httpClient.submitForm(
            url = "$SECURE_TOKEN_BASE/token",
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }
        ) {
            parameter("key", FIREBASE_WEB_API_KEY)
        }
        requireSuccess(response)
        val body: RefreshTokenResponse = response.body()
        return FirebaseAuthTokens(
            idToken = body.idToken,
            refreshToken = body.refreshToken,
            expiresInSeconds = body.expiresIn.toLongOrNull() ?: 3600L,
            uid = body.userId
        )
    }

    private suspend fun parseAuthResponse(response: HttpResponse): FirebaseAuthTokens {
        requireSuccess(response)
        val body: IdentityToolkitAuthResponse = response.body()
        return FirebaseAuthTokens(
            idToken = body.idToken,
            refreshToken = body.refreshToken,
            expiresInSeconds = body.expiresIn.toLongOrNull() ?: 3600L,
            uid = body.localId
        )
    }

    private suspend fun requireSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val errorBody = runCatching { response.body<IdentityToolkitErrorResponse>() }.getOrNull()
        val code = errorBody?.error?.message
        throw AuthException(
            message = code ?: "Firebase Auth zahtev nije uspeo (${response.status})",
            code = code
        )
    }
}

@Serializable
private data class EmailPasswordRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true
)

@Serializable
private data class UpdateProfileRequest(
    val idToken: String,
    val displayName: String,
    val returnSecureToken: Boolean = true
)

@Serializable
private data class OobCodeRequest(
    val requestType: String,
    val idToken: String? = null,
    val email: String? = null
)

@Serializable
private data class IdentityToolkitAuthResponse(
    val idToken: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String
)

@Serializable
private data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class IdentityToolkitErrorResponse(val error: IdentityToolkitErrorBody)

@Serializable
private data class IdentityToolkitErrorBody(val code: Int, val message: String)
