package com.dj.insulink.shared.feature.librelink.data.remote

// LibreLinkUp requires an "Account-Id" header set to the SHA-256 hex digest of the
// LibreLinkUp user id. Implemented per-platform (java.security.MessageDigest on Android)
// rather than hand-rolled, since a wrong hash would produce a hard-to-diagnose auth failure.
expect fun sha256Hex(input: String): String
