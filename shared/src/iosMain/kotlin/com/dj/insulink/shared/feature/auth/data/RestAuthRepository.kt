package com.dj.insulink.shared.feature.auth.data

import com.dj.insulink.shared.core.auth.AuthSession
import com.dj.insulink.shared.core.auth.IosAuthTokenProvider
import com.dj.insulink.shared.core.crypto.generateFriendCodeFromEmail
import com.dj.insulink.shared.core.firestore.FirestoreRestClient
import com.dj.insulink.shared.core.firestore.FirestoreValue
import com.dj.insulink.shared.feature.auth.data.local.AuthTokenStorage
import com.dj.insulink.shared.feature.auth.data.local.StoredAuthSession
import com.dj.insulink.shared.feature.auth.data.remote.FirebaseAuthRestClient
import com.dj.insulink.shared.feature.auth.data.remote.FirebaseAuthTokens
import com.dj.insulink.shared.feature.auth.data.remote.GOOGLE_IOS_CLIENT_ID
import com.dj.insulink.shared.feature.auth.data.remote.GOOGLE_IOS_REVERSED_CLIENT_ID
import com.dj.insulink.shared.feature.auth.data.remote.GoogleSignInCoordinator
import com.dj.insulink.shared.feature.auth.data.remote.GoogleTokenExchangeClient
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val USERS_COLLECTION = "users"

// Isto ponašanje kao Android-ov app/auth/data/AuthRepository.kt, ali preko Ktor REST klijenata
// (FirebaseAuthRestClient + FirestoreRestClient) umesto pravog Firebase GMS SDK-a - arhitektonska
// odluka iz Faze 1 plana (izbegava CocoaPods/ABI rizik).
// Token refresh logika (ensureValidTokens) živi u IosAuthTokenProvider - deljena sa Faza 2
// FirestoreRestXRemoteDataSource actual-ima, ne duplirana ovde.
class RestAuthRepository(
    private val authClient: FirebaseAuthRestClient,
    private val firestoreClient: FirestoreRestClient,
    private val tokenStorage: AuthTokenStorage,
    private val tokenProvider: IosAuthTokenProvider,
    private val googleSignInCoordinator: GoogleSignInCoordinator,
    private val googleTokenExchangeClient: GoogleTokenExchangeClient
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<AuthUser?>(null)
    override val currentUserFlow: StateFlow<AuthUser?> = _currentUserFlow

    override suspend fun restoreSession(): AuthUser? {
        val stored = tokenStorage.load() ?: return null
        val valid = runCatching { tokenProvider.ensureValidTokens(stored) }.getOrNull() ?: run {
            tokenStorage.clear()
            return null
        }
        val user = AuthUser(
            uid = valid.uid,
            firstName = valid.firstName,
            lastName = valid.lastName,
            email = valid.email,
            friendCode = valid.friendCode,
            isEmailVerified = valid.isEmailVerified
        )
        publish(user)
        return user
    }

    override suspend fun login(email: String, password: String): AuthUser {
        val tokens = authClient.signIn(email, password)
        val fields = firestoreClient.getDocumentFields(USERS_COLLECTION, tokens.uid, tokens.idToken)
            ?: throw AuthException("Korisnički profil nije pronađen u bazi.")
        val user = AuthUser(
            uid = tokens.uid,
            firstName = FirestoreValue.stringOrNull(fields, "firstName").orEmpty(),
            lastName = FirestoreValue.stringOrNull(fields, "lastName").orEmpty(),
            email = email,
            friendCode = FirestoreValue.stringOrNull(fields, "friendCode").orEmpty(),
            // Identity Toolkit signInWithPassword ne vraća emailVerified direktno (trebao bi
            // dodatan accounts:lookup poziv) - v1 pojednostavljenje, dovoljno za MVP.
            isEmailVerified = false
        )
        persistSession(tokens, user)
        publish(user)
        return user
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthUser {
        val tokens = authClient.signUp(email, password)
        authClient.updateProfile(tokens.idToken, "$firstName $lastName")

        val friendCode = generateFriendCodeFromEmail(email)
        firestoreClient.createDocument(
            collection = USERS_COLLECTION,
            documentId = tokens.uid,
            idToken = tokens.idToken,
            fields = mapOf(
                "firstName" to FirestoreValue.Str(firstName),
                "lastName" to FirestoreValue.Str(lastName),
                "email" to FirestoreValue.Str(email),
                "createdAt" to FirestoreValue.Timestamp(Clock.System.now().toString()),
                "userId" to FirestoreValue.Str(tokens.uid),
                "readings" to FirestoreValue.Arr(),
                "friendCode" to FirestoreValue.Str(friendCode),
                "friends" to FirestoreValue.Arr(),
                "reminders" to FirestoreValue.Arr(),
                "exercises" to FirestoreValue.Arr()
            )
        )
        authClient.sendEmailVerification(tokens.idToken)

        val user = AuthUser(
            uid = tokens.uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            friendCode = friendCode,
            isEmailVerified = false
        )
        persistSession(tokens, user)
        publish(user)
        return user
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun signInWithGoogle(): AuthUser {
        val authResult = googleSignInCoordinator.signIn()
        val googleIdToken = googleTokenExchangeClient.exchangeCodeForIdToken(
            code = authResult.code,
            codeVerifier = authResult.codeVerifier,
            clientId = GOOGLE_IOS_CLIENT_ID,
            redirectUri = "$GOOGLE_IOS_REVERSED_CLIENT_ID:/oauth2redirect"
        )
        val result = authClient.signInWithGoogleIdToken(googleIdToken)

        val existingFields = firestoreClient.getDocumentFields(
            USERS_COLLECTION, result.tokens.uid, result.tokens.idToken
        )
        val user = if (existingFields != null) {
            // Postojeći nalog (npr. isti Google nalog kojim je korisnik već registrovan na
            // Android-u) - samo pročitaj postojeći profil, ne diraj ga.
            AuthUser(
                uid = result.tokens.uid,
                firstName = FirestoreValue.stringOrNull(existingFields, "firstName").orEmpty(),
                lastName = FirestoreValue.stringOrNull(existingFields, "lastName").orEmpty(),
                email = result.email,
                friendCode = FirestoreValue.stringOrNull(existingFields, "friendCode").orEmpty(),
                isEmailVerified = true
            )
        } else {
            // Nov nalog - isti obrazac kao register(), friendCode iz email-a.
            val friendCode = generateFriendCodeFromEmail(result.email)
            firestoreClient.createDocument(
                collection = USERS_COLLECTION,
                documentId = result.tokens.uid,
                idToken = result.tokens.idToken,
                fields = mapOf(
                    "firstName" to FirestoreValue.Str(result.firstName),
                    "lastName" to FirestoreValue.Str(result.lastName),
                    "email" to FirestoreValue.Str(result.email),
                    "createdAt" to FirestoreValue.Timestamp(Clock.System.now().toString()),
                    "userId" to FirestoreValue.Str(result.tokens.uid),
                    "readings" to FirestoreValue.Arr(),
                    "friendCode" to FirestoreValue.Str(friendCode),
                    "friends" to FirestoreValue.Arr(),
                    "reminders" to FirestoreValue.Arr(),
                    "exercises" to FirestoreValue.Arr()
                )
            )
            AuthUser(
                uid = result.tokens.uid,
                firstName = result.firstName,
                lastName = result.lastName,
                email = result.email,
                friendCode = friendCode,
                isEmailVerified = true
            )
        }
        persistSession(result.tokens, user)
        publish(user)
        return user
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        authClient.sendPasswordResetEmail(email)
    }

    override suspend fun signOut() {
        tokenStorage.clear()
        publish(null)
    }

    private fun persistSession(tokens: FirebaseAuthTokens, user: AuthUser) {
        tokenStorage.save(
            StoredAuthSession(
                idToken = tokens.idToken,
                refreshToken = tokens.refreshToken,
                expiresAtEpochSeconds = AuthTokenStorage.nowEpochSeconds() + tokens.expiresInSeconds,
                uid = user.uid,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                friendCode = user.friendCode,
                isEmailVerified = user.isEmailVerified
            )
        )
    }

    private fun publish(user: AuthUser?) {
        _currentUserFlow.value = user
        AuthSession.setCurrentUser(user)
    }
}
