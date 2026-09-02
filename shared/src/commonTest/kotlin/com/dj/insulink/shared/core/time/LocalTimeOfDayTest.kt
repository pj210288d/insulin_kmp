package com.dj.insulink.shared.core.time

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class LocalTimeOfDayTest {

    @OptIn(ExperimentalTime::class)
    private fun millisAt(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        return LocalDateTime(year, month, day, hour, minute)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    }

    @Test
    fun startOfDayMillis_returnsMidnightOfTheSameCalendarDay() {
        val noon = millisAt(2026, 6, 15, 14, 30)

        assertEquals(millisAt(2026, 6, 15, 0, 0), startOfDayMillis(noon))
    }

    @Test
    fun shiftedDayStartMillis_withZeroDays_matchesStartOfDay() {
        val noon = millisAt(2026, 6, 15, 14, 30)

        assertEquals(startOfDayMillis(noon), shiftedDayStartMillis(noon, 0))
    }

    @Test
    fun shiftedDayStartMillis_withPositiveDays_movesForward() {
        val noon = millisAt(2026, 6, 15, 14, 30)

        assertEquals(millisAt(2026, 6, 16, 0, 0), shiftedDayStartMillis(noon, 1))
    }

    @Test
    fun shiftedDayStartMillis_withNegativeDays_movesBackward() {
        val noon = millisAt(2026, 6, 15, 14, 30)

        assertEquals(millisAt(2026, 6, 14, 0, 0), shiftedDayStartMillis(noon, -1))
    }

    @Test
    fun shiftedDayStartMillis_crossesAMonthBoundary() {
        val lastDayOfMonth = millisAt(2026, 6, 30, 10, 0)

        assertEquals(millisAt(2026, 7, 1, 0, 0), shiftedDayStartMillis(lastDayOfMonth, 1))
    }
}
