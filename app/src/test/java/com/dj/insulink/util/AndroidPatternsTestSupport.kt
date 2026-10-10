package com.dj.insulink.util

import android.util.Patterns
import java.lang.reflect.Field
import java.util.regex.Pattern

object AndroidPatternsTestSupport {

    private val EMAIL_REGEX: Pattern = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    )

    fun installEmailPattern() {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafe = unsafeClass.getDeclaredField("theUnsafe")
            .apply { isAccessible = true }
            .get(null)

        val field: Field = Patterns::class.java.getField("EMAIL_ADDRESS")
        val base = unsafeClass.getMethod("staticFieldBase", Field::class.java)
            .invoke(theUnsafe, field)
        val offset = unsafeClass.getMethod("staticFieldOffset", Field::class.java)
            .invoke(theUnsafe, field) as Long
        unsafeClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
            .invoke(theUnsafe, base, offset, EMAIL_REGEX)
    }
}
