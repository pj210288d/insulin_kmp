package com.dj.insulink.shared.feature.librelink.data.mapper

import kotlin.time.ExperimentalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

// LibreLinkUp's FactoryTimestamp isn't ISO-8601 (e.g. "8/2/2025 3:45:00 PM"), so
// kotlinx.datetime.LocalDateTime.parse can't read it directly. Parsed manually here
// instead of using java.time, which isn't available in commonMain.
@OptIn(ExperimentalTime::class)
fun parseLibreLinkTimestamp(raw: String): Long? {
    val parts = raw.trim().split(" ")
    if (parts.size != 3) return null

    val dateParts = parts[0].split("/")
    if (dateParts.size != 3) return null
    val month = dateParts[0].toIntOrNull() ?: return null
    val day = dateParts[1].toIntOrNull() ?: return null
    val year = dateParts[2].toIntOrNull() ?: return null

    val timeParts = parts[1].split(":")
    if (timeParts.size != 3) return null
    var hour = timeParts[0].toIntOrNull() ?: return null
    val minute = timeParts[1].toIntOrNull() ?: return null
    val second = timeParts[2].toIntOrNull() ?: return null

    when (parts[2].uppercase()) {
        "PM" -> if (hour != 12) hour += 12
        "AM" -> if (hour == 12) hour = 0
        else -> return null
    }

    return try {
        LocalDateTime(year, month, day, hour, minute, second)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
    } catch (e: IllegalArgumentException) {
        null
    }
}
