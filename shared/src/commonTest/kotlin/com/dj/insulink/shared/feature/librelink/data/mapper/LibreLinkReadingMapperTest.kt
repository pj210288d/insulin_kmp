package com.dj.insulink.shared.feature.librelink.data.mapper

import com.dj.insulink.shared.feature.librelink.data.remote.LibreLinkGlucoseReading
import kotlin.test.Test
import kotlin.test.assertEquals

class LibreLinkReadingMapperTest {

    @Test
    fun mapsToGlucoseReadingTaggedWithTheLibreLinkComment() {
        val reading = LibreLinkGlucoseReading(timestamp = 1000L, valueMgDl = 120)

        val result = reading.toGlucoseReading("u1")

        assertEquals(0L, result.id)
        assertEquals("u1", result.userId)
        assertEquals(1000L, result.timestamp)
        assertEquals(120, result.value)
        assertEquals(LIBRELINK_READING_COMMENT, result.comment)
    }
}
