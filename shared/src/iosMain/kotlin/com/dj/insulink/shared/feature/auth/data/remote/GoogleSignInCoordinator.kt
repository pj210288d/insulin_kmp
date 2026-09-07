package com.dj.insulink.shared.feature.auth.data.remote

import com.dj.insulink.shared.core.crypto.sha256Bytes
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

private const val GOOGLE_AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth"
private const val CODE_VERIFIER_CHARS =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

/** Authorization code + PKCE verifier (potreban za token exchange - vidi FirebaseAuthRestClient nije, ovo ide direktno Google-u preko GoogleTokenExchangeClient). */
data class GoogleAuthorizationResult(val code: String, val codeVerifier: String)

// Nema GoogleSignIn SDK-a (namerno - vidi CLAUDE.md, izbegava CocoaPods) - "ručni" OAuth preko
// ASWebAuthenticationSession (sistemski framework, besplatan preko Kotlin/Native ObjC interop-a).
// Authorization Code + PKCE flow (NE implicit id_token flow) - Google vraća
// "Error 400: unsupported_response_type" za response_type=id_token na ovom klijentu (potvrđeno
// uživo na fizičkom testu 2026-09-06, savremeni Google OAuth klijenti generalno više ne
// podržavaju implicit flow). PKCE (code_challenge/code_verifier) je obavezan jer je ovo "public"
// klijent (iOS app, bez client secret-a) - bez njega bi authorization code mogao da se presretne.
@OptIn(ExperimentalForeignApi::class)
class GoogleSignInCoordinator {

    private var activeSession: ASWebAuthenticationSession? = null
    private var activePresentationContextProvider: NSObject? = null

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun signIn(): GoogleAuthorizationResult = suspendCancellableCoroutine { continuation ->
        val codeVerifier = (1..64).map { CODE_VERIFIER_CHARS.random(Random) }.joinToString("")
        val codeChallenge = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
            .encode(sha256Bytes(codeVerifier))

        val authUrlString = buildString {
            append(GOOGLE_AUTH_ENDPOINT)
            append("?client_id=").append(GOOGLE_IOS_CLIENT_ID)
            append("&redirect_uri=").append(GOOGLE_IOS_REVERSED_CLIENT_ID).append(":/oauth2redirect")
            append("&response_type=code")
            append("&scope=openid%20email%20profile")
            append("&code_challenge=").append(codeChallenge)
            append("&code_challenge_method=S256")
            append("&prompt=select_account")
        }
        val authUrl = NSURL.URLWithString(authUrlString)
        if (authUrl == null) {
            continuation.resumeWithException(AuthException("Nevažeći URL za Google prijavu"))
            return@suspendCancellableCoroutine
        }

        val presentationContextProvider = object : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
            override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow {
                return UIApplication.sharedApplication.keyWindow ?: UIWindow()
            }
        }

        val session = ASWebAuthenticationSession(
            uRL = authUrl,
            callbackURLScheme = GOOGLE_IOS_REVERSED_CLIENT_ID
        ) { callbackUrl, error ->
            activeSession = null
            activePresentationContextProvider = null
            if (error != null) {
                continuation.resumeWithException(
                    AuthException(error.localizedDescription ?: "Google prijava otkazana")
                )
                return@ASWebAuthenticationSession
            }
            val code = callbackUrl?.query
                ?.split("&")
                ?.map { it.split("=", limit = 2) }
                ?.firstOrNull { it.getOrNull(0) == "code" }
                ?.getOrNull(1)
            if (code.isNullOrEmpty()) {
                continuation.resumeWithException(AuthException("Google odgovor ne sadrži authorization code"))
            } else {
                continuation.resume(GoogleAuthorizationResult(code, codeVerifier))
            }
        }
        session.presentationContextProvider = presentationContextProvider
        activeSession = session
        activePresentationContextProvider = presentationContextProvider

        continuation.invokeOnCancellation {
            session.cancel()
            activeSession = null
            activePresentationContextProvider = null
        }

        session.start()
    }
}
