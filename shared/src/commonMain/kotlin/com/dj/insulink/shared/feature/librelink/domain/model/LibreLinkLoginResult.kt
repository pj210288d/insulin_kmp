package com.dj.insulink.shared.feature.librelink.domain.model

data class LibreLinkLoginResult(
    val email: String,
    val auth: LibreLinkAuth,
    val connections: List<LibreLinkConnection>
)
