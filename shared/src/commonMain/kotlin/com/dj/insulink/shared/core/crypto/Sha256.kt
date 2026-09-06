package com.dj.insulink.shared.core.crypto

// Namerno odvojeno od feature/librelink/data/remote/Sha256.kt (isti algoritam, ali taj fajl
// je librelink-specifičan po lokaciji - ne dirati radeći na core/auth). Koristi se za
// deterministički friend code (vidi FriendCodeGenerator) - isti algoritam kao Android-ov
// app/core/utils/FriendCodeGenerator.kt, koji koristi java.security.MessageDigest (JVM-only,
// ne postoji u Kotlin/Native), pa ovde mora expect/actual.
expect fun sha256Hex(input: String): String

/** Sirovi SHA-256 bajtovi (za PKCE code_challenge - Base64URL enkodiranje treba bajtove, ne hex). */
expect fun sha256Bytes(input: String): ByteArray
