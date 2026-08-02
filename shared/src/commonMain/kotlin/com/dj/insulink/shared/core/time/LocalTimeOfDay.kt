package com.dj.insulink.shared.core.time

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
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
