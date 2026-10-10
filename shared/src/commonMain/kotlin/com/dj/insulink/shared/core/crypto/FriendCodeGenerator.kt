package com.dj.insulink.shared.core.crypto

private const val CHAR_SET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

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
