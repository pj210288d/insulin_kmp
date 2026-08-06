package com.dj.insulink.shared.feature.librelink.domain.model

data class LibreLinkSession(
    val email: String,
    val token: String,
    val regionHost: String,
    val accountIdHash: String,
    val patientId: String
) {
    val auth: LibreLinkAuth
        get() = LibreLinkAuth(token = token, regionHost = regionHost, accountIdHash = accountIdHash)
}
