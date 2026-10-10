package com.dj.insulink.shared.core.crypto

expect fun sha256Hex(input: String): String

expect fun sha256Bytes(input: String): ByteArray
