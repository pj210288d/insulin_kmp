package com.dj.insulink.shared.feature.librelink.domain.model

data class LibreLinkAuth(
    val token: String,
    val regionHost: String,
    val accountIdHash: String
)
