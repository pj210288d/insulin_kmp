package com.dj.insulink.shared.core.crypto

private const val CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

// Deterministički friend code iz email adrese - isti charset/podrazumevana dužina kao
// Android-ov app/core/utils/FriendCodeGenerator.kt (DeterministicCodeGenerator), ali NIJE
// algoritamski identičan (taj koristi java.math.BigInteger, JVM-only, nedostupno u
// Kotlin/Native) - ovde se svaki znak izvodi iz po jednog bajta SHA-256 heša umesto iz jednog
// velikog broja. Nema funkcionalnog uticaja: Friends pretraga (Faza 3) upoređuje tačan string
// već sačuvan u Firestore-u, ne re-generiše kod iz email-a - format (charset/dužina) je isti,
// samo se za isti email NEĆE dobiti identičan kod kao na Android-u, što nije bitno jer se kod
// generiše samo jednom, pri registraciji.
fun generateFriendCodeFromEmail(email: String, length: Int = 6): String {
    val normalizedEmail = email.lowercase().trim()
    val hashHex = sha256Hex(normalizedEmail)
    val hashBytes = IntArray(hashHex.length / 2) { i ->
        hashHex.substring(i * 2, i * 2 + 2).toInt(16)
    }
    return buildString {
        for (i in 0 until length) {
            append(CHAR_SET[hashBytes[i % hashBytes.size] % CHAR_SET.length])
        }
    }
}
