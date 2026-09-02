package com.dj.insulink.shared.feature.statistics.domain.model

import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.daysAgoMillis
import com.dj.insulink.shared.core.time.startOfDayMillis

enum class StatisticsRange {
    TODAY,
    LAST_7_DAYS,
    LAST_15_DAYS,
    LAST_30_DAYS,
    LAST_90_DAYS
}

fun StatisticsRange.startMillis(): Long = when (this) {
    StatisticsRange.TODAY -> startOfDayMillis(currentTimeMillis())
    StatisticsRange.LAST_7_DAYS -> daysAgoMillis(7)
    StatisticsRange.LAST_15_DAYS -> daysAgoMillis(15)
    StatisticsRange.LAST_30_DAYS -> daysAgoMillis(30)
    StatisticsRange.LAST_90_DAYS -> daysAgoMillis(90)
}
