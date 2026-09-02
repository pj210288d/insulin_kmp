package com.dj.insulink.shared.core.time

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)
fun localTimeOfDay(epochMillis: Long): LocalTime {
    return Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .time
}

@OptIn(ExperimentalTime::class)
fun currentLocalTimeOfDay(): LocalTime {
    return Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .time
}

// Midnight (device local timezone) of the calendar day containing [epochMillis].
@OptIn(ExperimentalTime::class)
fun startOfDayMillis(epochMillis: Long): Long {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    return LocalDateTime(date, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()
}

// Midnight of the calendar day [days] days away from the one containing [epochMillis] (negative
// for earlier days). Goes through LocalDate arithmetic rather than raw millis math so it stays
// correct across DST transitions, where a "day" isn't always exactly 24h.
@OptIn(ExperimentalTime::class)
fun shiftedDayStartMillis(epochMillis: Long, days: Int): Long {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val shiftedDate = date.plus(DatePeriod(days = days))
    return LocalDateTime(shiftedDate, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()
}

// [days] days before now, as epoch millis - a rolling window ending "now" (not day-aligned),
// used for "last N days" range queries.
fun daysAgoMillis(days: Int): Long {
    return currentTimeMillis() - days * MILLIS_PER_DAY
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
