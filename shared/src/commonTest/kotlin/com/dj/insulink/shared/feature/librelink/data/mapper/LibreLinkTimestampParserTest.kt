package com.dj.insulink.shared.feature.librelink.data.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@OptIn(ExperimentalTime::class)
class LibreLinkTimestampParserTest {

    // FactoryTimestamp is UTC (see LibreLinkTimestampParser's doc comment), so the
    // expected epoch must be computed against UTC, not the test runner's local zone.
    private fun epochMillisOf(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        return LocalDateTime(year, month, day, hour, minute, second)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
    }

    @Test
    fun parsesAfternoonTimestamp() {
        val expected = epochMillisOf(2025, 8, 2, 15, 45, 0)

        assertEquals(expected, parseLibreLinkTimestamp("8/2/2025 3:45:00 PM"))
    }

    @Test
    fun parsesNoonAsTwelveHundredHours() {
        val expected = epochMillisOf(2025, 8, 2, 12, 0, 0)

        assertEquals(expected, parseLibreLinkTimestamp("8/2/2025 12:00:00 PM"))
    }

    @Test
    fun parsesMidnightAsZeroHundredHours() {
        val expected = epochMillisOf(2025, 8, 2, 0, 0, 0)

        assertEquals(expected, parseLibreLinkTimestamp("8/2/2025 12:00:00 AM"))
    }

    @Test
    fun returnsNullForUnparsableInput() {
        assertNull(parseLibreLinkTimestamp("not a timestamp"))
        assertNull(parseLibreLinkTimestamp(""))
        assertNull(parseLibreLinkTimestamp("8/2/2025 3:45:00"))
    }
}
