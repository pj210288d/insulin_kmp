package com.dj.insulink.shared.feature.auth.domain.model

// Isti oblik kao Android-ov app/auth/domain/models/User.kt (namerno - Android actual mapira
// 1:1 iz tog modela, iOS actual ga popunjava iz Identity Toolkit + Firestore REST odgovora).
data class AuthUser(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val friendCode: String,
    val isEmailVerified: Boolean = false
)
