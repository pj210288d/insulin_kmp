package com.dj.insulink.shared.core.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(input: String): String {
    val digest = sha256Bytes(input)
    return digest.joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
}

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Bytes(input: String): ByteArray {
    val bytes = input.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { pinnedInput ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(pinnedInput.addressOf(0), bytes.size.toUInt(), pinnedDigest.addressOf(0))
        }
    }
    return digest.toByteArray()
}
