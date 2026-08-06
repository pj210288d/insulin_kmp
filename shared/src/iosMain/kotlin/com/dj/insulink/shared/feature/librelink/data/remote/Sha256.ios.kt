package com.dj.insulink.shared.feature.librelink.data.remote

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(input: String): String {
    val bytes = input.encodeToByteArray()
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { pinnedInput ->
        digest.usePinned { pinnedDigest ->
            CC_SHA256(pinnedInput.addressOf(0), bytes.size.toUInt(), pinnedDigest.addressOf(0))
        }
    }
    return digest.joinToString("") { byte -> byte.toString(16).padStart(2, '0') }
}
