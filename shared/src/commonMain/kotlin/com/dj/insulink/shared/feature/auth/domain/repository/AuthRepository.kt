package com.dj.insulink.shared.feature.auth.domain.repository

import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUserFlow: StateFlow<AuthUser?>

    suspend fun restoreSession(): AuthUser?

    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthUser

    suspend fun signInWithGoogle(): AuthUser

    suspend fun sendPasswordResetEmail(email: String)
    suspend fun signOut()
}

expect val isGoogleSignInSupported: Boolean
