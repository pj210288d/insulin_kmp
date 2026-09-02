package com.dj.insulink.shared.feature.librelink.domain.model

// Intermediate result of a LibreLinkUp login, before a specific connection (patient) has been
// chosen and persisted as a session. A LibreLinkUp account can be shared-with-follow multiple
// patients (e.g. a caregiver following a family member's sensor in addition to - or instead of -
// their own), so the caller must not assume the first connection returned is the right one.
data class LibreLinkLoginResult(
    val email: String,
    val auth: LibreLinkAuth,
    val connections: List<LibreLinkConnection>
)
