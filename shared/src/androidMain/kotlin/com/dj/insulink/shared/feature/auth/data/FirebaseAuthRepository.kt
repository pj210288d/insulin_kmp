package com.dj.insulink.shared.feature.auth.data

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.dj.insulink.shared.core.auth.AuthSession
import com.dj.insulink.shared.core.crypto.generateFriendCodeFromEmail
import com.dj.insulink.shared.feature.auth.domain.model.AuthException
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

private const val USERS_COLLECTION = "users"

private const val WEB_CLIENT_ID = "846271429991-s1fudad0obbn2am6a9virtqrfbmhcn7o.apps.googleusercontent.com"

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val context: Context
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
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credential = runCatching { credentialManager.getCredential(context, request) }
            .getOrElse { throw AuthException(it.message ?: "Google prijava nije uspela") }
            .credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw AuthException("Google prijava nije uspela - neočekivan tip kredencijala")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)

        val result = runCatching { firebaseAuth.signInWithCredential(firebaseCredential).await() }
            .getOrElse { throw AuthException(it.message ?: "Google prijava nije uspela") }
        val firebaseUser = result.user ?: throw AuthException("Google prijava nije uspela")

        val userDocRef = firestore.collection(USERS_COLLECTION).document(firebaseUser.uid)
        val existingDoc = userDocRef.get().await()

        val user = if (existingDoc.exists()) {
            AuthUser(
                uid = firebaseUser.uid,
                firstName = existingDoc.getString("firstName").orEmpty(),
                lastName = existingDoc.getString("lastName").orEmpty(),
                email = firebaseUser.email.orEmpty(),
                friendCode = existingDoc.getString("friendCode").orEmpty(),
                isEmailVerified = firebaseUser.isEmailVerified
            )
        } else {
            val displayName = firebaseUser.displayName.orEmpty()
            val nameParts = displayName.split(" ")
            val firstName = nameParts.getOrNull(0).orEmpty()
            val lastName = nameParts.drop(1).joinToString(" ")
            val friendCode = generateFriendCodeFromEmail(firebaseUser.email.orEmpty())
            val newUserData = hashMapOf(
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to firebaseUser.email.orEmpty(),
                "createdAt" to Timestamp.now(),
                "userId" to firebaseUser.uid,
                "readings" to emptyList<Any>(),
                "friendCode" to friendCode,
                "friends" to emptyList<Any>(),
                "reminders" to emptyList<Any>(),
                "exercises" to emptyList<Any>()
            )
            userDocRef.set(newUserData).await()
            AuthUser(
                uid = firebaseUser.uid,
                firstName = firstName,
                lastName = lastName,
                email = firebaseUser.email.orEmpty(),
                friendCode = friendCode,
                isEmailVerified = firebaseUser.isEmailVerified
            )
        }
        publish(user)
        return user
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        runCatching { firebaseAuth.sendPasswordResetEmail(email).await() }
            .getOrElse { throw AuthException(it.message ?: "Slanje emaila nije uspelo") }
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
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
