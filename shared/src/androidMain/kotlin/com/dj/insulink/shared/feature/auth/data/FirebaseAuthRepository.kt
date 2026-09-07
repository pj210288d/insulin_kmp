package com.dj.insulink.shared.feature.auth.data

import com.dj.insulink.shared.core.auth.AuthSession
import com.dj.insulink.shared.core.crypto.generateFriendCodeFromEmail
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

// Nova, samostalna klasa - NE app/auth/data/AuthRepository.kt (taj ostaje netaknut, i dalje ga
// koristi pravi Android Login/Registration ekran preko Hilt-a, guardrail #4 iz plana). Ova
// wrapuje ISTI, već dokazani Firebase GMS SDK (FirebaseAuth+FirebaseFirestore - vidi
// core/di/FirebaseModule.kt) samo da bi shared App() površina (iOS root + Android "Shared UI"
// demo) imala svoj commonMain AuthRepository interfejs zadovoljen i na Android-u. U praksi
// restoreSession() na Android-u skoro uvek odmah uspe - do ove rute se stiže tek posle pravog
// login-a u glavnoj app (AppNavigation.kt startDestination logika), pa je firebaseAuth.currentUser
// već postavljen - shared Login/Registration ekran se praktično nikad ne prikazuje na Android-u,
// što je očekivano (postoji radi dokazivanja da isti kod radi na oba OS-a, ne kao stvarni
// dvostruki login tok).
class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    private val _currentUserFlow = MutableStateFlow<AuthUser?>(null)
    override val currentUserFlow: StateFlow<AuthUser?> = _currentUserFlow

    override suspend fun restoreSession(): AuthUser? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return runCatching {
            fetchUser(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.isEmailVerified)
        }.onSuccess { publish(it) }.getOrNull()
    }

    override suspend fun login(email: String, password: String): AuthUser {
        val result = runCatching { firebaseAuth.signInWithEmailAndPassword(email, password).await() }
            .getOrElse { throw AuthException(it.message ?: "Prijava nije uspela") }
        val firebaseUser = result.user ?: throw AuthException("Prijava nije uspela")
        val user = fetchUser(firebaseUser.uid, firebaseUser.email.orEmpty(), firebaseUser.isEmailVerified)
        publish(user)
        return user
    }

    override suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthUser {
        val result = runCatching { firebaseAuth.createUserWithEmailAndPassword(email, password).await() }
            .getOrElse { throw AuthException(it.message ?: "Registracija nije uspela") }
        val firebaseUser = result.user ?: throw AuthException("Registracija nije uspela")

        val displayName = "$firstName $lastName"
        firebaseUser.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
        ).await()

        val friendCode = generateFriendCodeFromEmail(email)
        val userData = hashMapOf(
            "firstName" to firstName,
            "lastName" to lastName,
            "email" to email,
            "createdAt" to Timestamp.now(),
            "userId" to firebaseUser.uid,
            "readings" to emptyList<Any>(),
            "friendCode" to friendCode,
            "friends" to emptyList<Any>(),
            "reminders" to emptyList<Any>(),
            "exercises" to emptyList<Any>()
        )
        firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).set(userData).await()
        firebaseUser.sendEmailVerification().await()

        val user = AuthUser(
            uid = firebaseUser.uid,
            firstName = firstName,
            lastName = lastName,
            email = email,
            friendCode = friendCode,
            isEmailVerified = firebaseUser.isEmailVerified
        )
        publish(user)
        return user
    }

    override suspend fun signInWithGoogle(): AuthUser {
        // Deljeni demo ekran u praksi se ne prikazuje na Android-u (do njega se stiže tek posle
        // pravog login-a u glavnoj app - vidi restoreSession() gore), a Android-ov PRAVI Google
        // Sign-In ekran (app/auth/ui/LoginScreen.kt) već postoji, sa potpuno drugačijim OS
        // mehanizmom (Activity Result Contract, ne ASWebAuthenticationSession) - namerno se ne
        // duplira ovde. Vidi isGoogleSignInSupported.
        throw UnsupportedOperationException(
            "Google Sign-In u deljenom demo ekranu nije podržan na Android-u - koristi glavni Login ekran"
        )
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        runCatching { firebaseAuth.sendPasswordResetEmail(email).await() }
            .getOrElse { throw AuthException(it.message ?: "Slanje emaila nije uspelo") }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        publish(null)
    }

    private suspend fun fetchUser(uid: String, email: String, isEmailVerified: Boolean): AuthUser {
        val snapshot = firestore.collection(USERS_COLLECTION).document(uid).get().await()
        return AuthUser(
            uid = uid,
            firstName = snapshot.getString("firstName").orEmpty(),
            lastName = snapshot.getString("lastName").orEmpty(),
            email = email,
            friendCode = snapshot.getString("friendCode").orEmpty(),
            isEmailVerified = isEmailVerified
        )
    }

    private fun publish(user: AuthUser?) {
        _currentUserFlow.value = user
        AuthSession.setCurrentUser(user)
    }
}
