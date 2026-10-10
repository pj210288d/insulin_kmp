package com.dj.insulink.shared.feature.auth.domain.model

class AuthException(
    message: String,
    val code: String? = null
) : Exception(message)
