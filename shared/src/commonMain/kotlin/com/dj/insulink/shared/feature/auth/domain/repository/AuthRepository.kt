package com.dj.insulink.shared.feature.auth.domain.repository

import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

// Platform-agnostičan oblik postojećeg app/auth/data/AuthRepository.kt (Android, Hilt) - vidi
// androidMain/iosMain actual-e. Android actual wrapuje POSTOJEĆI Firebase Auth/Firestore kod
// (GMS SDK, nepromenjen), iOS actual koristi nov Ktor REST klijent (Identity Toolkit +
// Firestore REST API - vidi core/network i core/firestore). Google Sign-In namerno izostavljen
// iz v1 (vidi plan, Faza 1) - samo email/password + reset lozinke + verifikacija emaila.
interface AuthRepository {
    val currentUserFlow: StateFlow<AuthUser?>

    // Pokušava da obnovi prethodnu sesiju (ako postoji sačuvan token/nalog) - poziva se jednom
    // pri pokretanju aplikacije, pre nego što se App() prikaže. Ne baca - vraća null ako nema
    // validne sesije umesto da puca na cold start.
    suspend fun restoreSession(): AuthUser?

    suspend fun login(email: String, password: String): AuthUser
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): AuthUser

    suspend fun sendPasswordResetEmail(email: String)
    suspend fun signOut()
}
