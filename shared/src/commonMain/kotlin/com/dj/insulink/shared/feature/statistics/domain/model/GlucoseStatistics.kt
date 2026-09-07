package com.dj.insulink.shared.feature.statistics.domain.model

data class GlucoseStatistics(
    val average: Double,
    val min: Int,
    val max: Int,
    val standardDeviation: Double,
    val readingCount: Int,
    val timeInRange: TimeInRangeBreakdown
)

// Percent of readings below / within / above the target range (70-126 mg/dL, matching the
// thresholds already used by GlucoseLevelTag/DynamicLineChart on the app side). Adds up to 100
// (within floating point rounding) unless there are zero readings.
data class TimeInRangeBreakdown(
    val belowPercent: Double,
    val inRangePercent: Double,
    val abovePercent: Double
)
