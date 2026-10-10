package com.dj.insulink.shared.feature.auth.domain.model

data class AuthUser(
    val uid: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val friendCode: String,
    val isEmailVerified: Boolean = false
)
