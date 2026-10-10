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

@OptIn(ExperimentalTime::class)
fun startOfDayMillis(epochMillis: Long): Long {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    return LocalDateTime(date, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun shiftedDayStartMillis(epochMillis: Long, days: Int): Long {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val shiftedDate = date.plus(DatePeriod(days = days))
    return LocalDateTime(shiftedDate, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()
}

fun daysAgoMillis(days: Int): Long {
    return currentTimeMillis() - days * MILLIS_PER_DAY
}

@OptIn(ExperimentalTime::class)
fun combineDateAndTime(dateEpochMillis: Long, existingTimestamp: Long): Long {
    val zone = TimeZone.currentSystemDefault()
    val newDate = Instant.fromEpochMilliseconds(dateEpochMillis).toLocalDateTime(zone).date
    val existingTime = localTimeOfDay(existingTimestamp)
    return LocalDateTime(newDate, existingTime).toInstant(zone).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun combineTimeWithDate(hour: Int, minute: Int, existingTimestamp: Long): Long {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(existingTimestamp).toLocalDateTime(zone).date
    return LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()
}

@OptIn(ExperimentalTime::class)
fun timeOfDayLabel(epochMillis: Long): String {
    val time = localTimeOfDay(epochMillis)
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

@OptIn(ExperimentalTime::class)
fun dateOnlyLabel(epochMillis: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    @Suppress("DEPRECATION")
    val day = date.dayOfMonth.toString().padStart(2, '0')
    @Suppress("DEPRECATION")
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year}"
}

@OptIn(ExperimentalTime::class)
fun dateTimeLabel(epochMillis: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    @Suppress("DEPRECATION")
    val day = date.dayOfMonth.toString().padStart(2, '0')
    @Suppress("DEPRECATION")
    val month = date.monthNumber.toString().padStart(2, '0')
    return "$day/$month/${date.year} ${timeOfDayLabel(epochMillis)}"
}

@OptIn(ExperimentalTime::class)
fun shortWeekdayDateLabel(epochMillis: Long): String {
    val zone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
    val weekday = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    @Suppress("DEPRECATION")
    val day = date.dayOfMonth
    return "$weekday, $day $month ${date.year}"
}

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
