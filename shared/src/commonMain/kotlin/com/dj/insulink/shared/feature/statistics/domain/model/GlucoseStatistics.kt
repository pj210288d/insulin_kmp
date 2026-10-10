package com.dj.insulink.shared.feature.statistics.domain.model

data class GlucoseStatistics(
    val average: Double,
    val min: Int,
    val max: Int,
    val standardDeviation: Double,
    val readingCount: Int,
    val timeInRange: TimeInRangeBreakdown
)

data class TimeInRangeBreakdown(
    val belowPercent: Double,
    val inRangePercent: Double,
    val abovePercent: Double
)
