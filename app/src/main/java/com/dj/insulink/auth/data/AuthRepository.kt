package com.dj.insulink.auth.data

import android.content.Context
import android.util.Log
import com.dj.insulink.R
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.auth.domain.models.UserLogin
import com.dj.insulink.auth.domain.models.UserRegistration
import com.dj.insulink.core.utils.DeterministicCodeGenerator
import com.dj.insulink.feature.fitness.domain.model.Exercise
import com.dj.insulink.feature.friends.domain.models.Friend
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.dj.insulink.feature.reminders.domain.models.Reminder
import com.google.firebase.Timestamp
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun loginUser(userLogin: UserLogin) {

        val authResult = firebaseAuth.signInWithEmailAndPassword(
            userLogin.email,
            userLogin.password
        ).await()

        val user = authResult.user
            ?: throw Exception("User login failed")
        Log.d("authrepo", "loginUser: $user")
    }

    suspend fun registerUser(userRegistration: UserRegistration): User {
        val friendCode = DeterministicCodeGenerator.generateCodeFromEmail(userRegistration.email)

        val authResult = firebaseAuth.createUserWithEmailAndPassword(
            userRegistration.email,
            userRegistration.password
        ).await()

        val user = authResult.user
            ?: throw Exception("User creation failed")

        val displayName = "${userRegistration.firstName} ${userRegistration.lastName}"
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName)
            .build()

        user.updateProfile(profileUpdates).await()

        val userData = hashMapOf(
            "firstName" to userRegistration.firstName,
            "lastName" to userRegistration.lastName,
            "email" to userRegistration.email,
            "createdAt" to Timestamp.now(),
            "userId" to user.uid,
            "readings" to emptyList<GlucoseReading>(),
            "friendCode" to friendCode,
            "friends" to emptyList<Friend>(),
            "reminders" to emptyList<Reminder>(),
            "exercises" to emptyList<Exercise>()
        )

        firestore.collection("users")
            .document(user.uid)
            .set(userData)
            .await()

        user.sendEmailVerification().await()

        return User(
            uid = user.uid,
            firstName = userRegistration.firstName,
            lastName = userRegistration.lastName,
            email = userRegistration.email,
            friendCode = friendCode,
            isEmailVerified = user.isEmailVerified
        )
    }

    fun getCurrentUserFlow(): Flow<String?> = flow {
        emit(getCurrentUser()?.uid)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        firebaseAuth.sendPasswordResetEmail(email).await()
    }

    suspend fun signInWithGoogle(credential: AuthCredential): User {
        try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser =
                authResult.user ?: throw Exception("Google Sign-In failed: User is null.")

            // Check if user already exists in Firestore
            val userDocRef = firestore.collection("users").document(firebaseUser.uid)
            val userDoc = userDocRef.get().await()

            if (!userDoc.exists()) {
                // User is new, save their details to Firestore
                val displayName = firebaseUser.displayName ?: "N/A"
                val nameParts = displayName.split(" ")
                val firstName = nameParts.getOrNull(0) ?: ""
                val lastName = nameParts.drop(1).joinToString(" ")
                val friendCode =
                    DeterministicCodeGenerator.generateCodeFromEmail(firebaseUser.email!!)

                val newUser = hashMapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to firebaseUser.email!!,
                    "friendCode" to friendCode,
                    "createdAt" to Timestamp.now(),
                    "userId" to firebaseUser.uid
                )
                userDocRef.set(newUser).await()

                return User(
                    uid = firebaseUser.uid,
                    firstName = firstName,
                    lastName = lastName,
                    email = firebaseUser.email!!,
                    friendCode = friendCode,
                    isEmailVerified = firebaseUser.isEmailVerified
                )
            } else {
                // User already exists, just return their data
                return User(
                    uid = firebaseUser.uid,
                    firstName = userDoc.getString("firstName") ?: "",
                    lastName = userDoc.getString("lastName") ?: "",
                    email = firebaseUser.email ?: "",
                    friendCode = userDoc.getString("friendCode") ?: "",
                    isEmailVerified = firebaseUser.isEmailVerified
                )
            }
        } catch (e: Exception) {
            // Re-throw to be handled by ViewModel
            throw e
        }
    }

    suspend fun getCurrentUser(): User? {
        val firebaseUser = firebaseAuth.currentUser ?: return null

        val userDoc = firestore.collection("users")
            .document(firebaseUser.uid)
            .get()
            .await()

        return User(
            uid = firebaseUser.uid,
            firstName = userDoc.getString("firstName") ?: "",
            lastName = userDoc.getString("lastName") ?: "",
            email = firebaseUser.email ?: "",
            friendCode = userDoc.getString("friendCode") ?: "",
            isEmailVerified = firebaseUser.isEmailVerified
        )
    }

    fun signOut(context: Context) {
        firebaseAuth.signOut()
        val googleSignInClient = GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        googleSignInClient.signOut().addOnCompleteListener {
            Log.d("Auth", "Signed out successfully from Google and Firebase")
        }
    }

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

}