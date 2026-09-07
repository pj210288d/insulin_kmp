package com.dj.insulink.shared.core.auth

import com.dj.insulink.shared.feature.auth.data.local.AuthTokenStorage
import com.dj.insulink.shared.feature.auth.data.local.StoredAuthSession
import com.dj.insulink.shared.feature.auth.data.remote.FirebaseAuthRestClient

// Zajednička "daj mi važeći idToken" logika za SVE iOS Firestore pozive (Faza 2
// FirestoreRestXRemoteDataSource actual-i, ne samo Auth ekran) - izdvojeno iz
// RestAuthRepository da bi ostatak koda mogao da ga koristi bez zavisnosti od cele Auth
// ViewModel/UI priče. Osvežava token preko refresh_token-a kad je blizu isteka (isti
// TOKEN_EXPIRY_BUFFER_SECONDS obrazac kao ranije u RestAuthRepository).
class IosAuthTokenProvider(
    private val authClient: FirebaseAuthRestClient,
    private val tokenStorage: AuthTokenStorage
) {
    /** Baca ako nema aktivne sesije - pozivalac (feature repository) treba to da prosledi kao grešku, ne da tiho ignoriše. */
    suspend fun currentIdToken(): String {
        val stored = tokenStorage.load() ?: error("Nema aktivne sesije - korisnik nije prijavljen")
        return ensureValidTokens(stored).idToken
    }

    suspend fun currentUserId(): String {
        val stored = tokenStorage.load() ?: error("Nema aktivne sesije - korisnik nije prijavljen")
        return stored.uid
    }

    suspend fun ensureValidTokens(stored: StoredAuthSession): StoredAuthSession {
        val nowPlusBuffer = AuthTokenStorage.nowEpochSeconds() + TOKEN_EXPIRY_BUFFER_SECONDS
        if (stored.expiresAtEpochSeconds > nowPlusBuffer) return stored
        val refreshed = authClient.refreshToken(stored.refreshToken)
        return stored.copy(
            idToken = refreshed.idToken,
            refreshToken = refreshed.refreshToken,
            expiresAtEpochSeconds = AuthTokenStorage.nowEpochSeconds() + refreshed.expiresInSeconds
        ).also(tokenStorage::save)
    }

    companion object {
        private const val TOKEN_EXPIRY_BUFFER_SECONDS = 60.0
    }
}
