package com.dj.insulink.shared.feature.auth.data.remote

import com.dj.insulink.shared.core.config.FIREBASE_PROJECT_ID
import com.dj.insulink.shared.core.config.FIREBASE_WEB_API_KEY
import com.dj.insulink.shared.core.network.createCoreHttpClient
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val IDENTITY_TOOLKIT_BASE = "https://identitytoolkit.googleapis.com/v1"
private const val SECURE_TOKEN_BASE = "https://securetoken.googleapis.com/v1"

/** Dovoljno da TokenStorage (iOS actual) sačuva sesiju i kasnije je obnovi. */
data class FirebaseAuthTokens(
    val idToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long,
    val uid: String
)

/** Rezultat accounts:signInWithIdp - tokeni + Google profil polja za popunjavanje AuthUser-a. */
data class GoogleSignInResult(
    val tokens: FirebaseAuthTokens,
    val email: String,
    val firstName: String,
    val lastName: String,
    val isNewUser: Boolean
)

// Ručno pisan Ktor klijent za Firebase Identity Toolkit REST API (login/registracija/reset
// lozinke/verifikacija emaila/refresh tokena) - koristi ga SAMO iOS Auth actual (Faza 1 plan,
// arhitektonska odluka: REST umesto GitLive Firebase KMP, da se izbegne CocoaPods i Kotlin/
// Native ABI rizik - vidi CLAUDE.md gotcha #5). Android i dalje ide preko pravog Firebase GMS
// SDK-a, nepromenjeno.
//
// Odgovori se parsiraju RUČNO (JsonObject + traženo polje po polje), NE preko strogog
// @Serializable data class-a - otkriveno na fizičkom testu (2026-09-06) da signInWithPassword
// odgovor na iOS simulatoru ume da stigne bez refreshToken/expiresIn polja iako je HTTP status
// 200 i iako identičan curl poziv (signUp→update→sendOobCode→signIn sekvenca) protiv istog
// Identity Toolkit endpoint-a UVEK vraća sva polja - uzrok nije do kraja utvrđen (moguć Ktor
// Darwin-engine edge slučaj sa čitanjem tela odgovora), ali strogo dekodiranje je tu bacalo
// kriptičnu kotlinx.serialization grešku umesto da otkaže glatko. Ako se ponovi, poruka će
// navesti TAČNO koji ključevi jesu prisutni (bez vrednosti - ništa osetljivo se ne loguje).
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

    /**
     * Razmenjuje Google OAuth id_token (dobijen preko ASWebAuthenticationSession - vidi
     * GoogleSignInCoordinator, iOS-only) za Firebase sesiju. Isti krajnji rezultat kao
     * Android-ov firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(...)), samo
     * preko REST-a: accounts:signInWithIdp prihvata bilo koji spoljni id_token i ili poveže
     * postojeći Firebase nalog (isti email) ili napravi nov, i vraća Google profil polja
     * (firstName/lastName/email) pored uobičajenih idToken/refreshToken/localId.
     */
    suspend fun signInWithGoogleIdToken(googleIdToken: String): GoogleSignInResult {
        val response = httpClient.post("$IDENTITY_TOOLKIT_BASE/accounts:signInWithIdp") {
            parameter("key", FIREBASE_WEB_API_KEY)
            contentType(ContentType.Application.Json)
            setBody(
                SignInWithIdpRequest(
                    postBody = "id_token=$googleIdToken&providerId=google.com",
                    requestUri = "https://$FIREBASE_PROJECT_ID.firebaseapp.com"
                )
            )
        }
        val json = parseJsonObject(response)
        return GoogleSignInResult(
            tokens = FirebaseAuthTokens(
                idToken = requireField(json, "idToken", response),
                refreshToken = requireField(json, "refreshToken", response),
                expiresInSeconds = json["expiresIn"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
                uid = requireField(json, "localId", response)
            ),
            email = json["email"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            firstName = json["firstName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            lastName = json["lastName"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            isNewUser = json["isNewUser"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
        )
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
        val json = parseJsonObject(response)
        return FirebaseAuthTokens(
            idToken = requireField(json, "id_token", response),
            refreshToken = requireField(json, "refresh_token", response),
            expiresInSeconds = json["expires_in"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
            uid = requireField(json, "user_id", response)
        )
    }

    private suspend fun parseAuthResponse(response: HttpResponse): FirebaseAuthTokens {
        val json = parseJsonObject(response)
        return FirebaseAuthTokens(
            idToken = requireField(json, "idToken", response),
            refreshToken = requireField(json, "refreshToken", response),
            expiresInSeconds = json["expiresIn"]?.jsonPrimitive?.contentOrNull?.toLongOrNull() ?: 3600L,
            uid = requireField(json, "localId", response)
        )
    }

    /** Parsira telo odgovora kao JsonObject; ako status nije uspešan, baca AuthException sa Google-ovom porukom. */
    private suspend fun parseJsonObject(response: HttpResponse): JsonObject {
        val rawBody = response.bodyAsText()
        val json = runCatching { Json.parseToJsonElement(rawBody).jsonObject }.getOrNull()
        if (!response.status.isSuccess()) {
            val message = json?.get("error")?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
            throw AuthException(message ?: "Firebase Auth zahtev nije uspeo (${response.status})", code = message)
        }
        return json ?: throw AuthException("Firebase Auth odgovor nije validan JSON (status ${response.status})")
    }

    private fun requireField(json: JsonObject, key: String, response: HttpResponse): String =
        json[key]?.jsonPrimitive?.contentOrNull ?: throw AuthException(
            "Firebase Auth odgovor nema očekivano polje '$key' (status ${response.status}, " +
                "prisutni ključevi: ${json.keys.sorted()})"
        )

    private suspend fun requireSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        parseJsonObject(response) // baca AuthException sa porukom iz tela odgovora
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
private data class SignInWithIdpRequest(
    val postBody: String,
    val requestUri: String,
    val returnIdpCredential: Boolean = true,
    val returnSecureToken: Boolean = true
)
