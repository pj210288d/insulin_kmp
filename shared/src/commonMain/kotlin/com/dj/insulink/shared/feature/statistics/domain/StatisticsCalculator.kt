package com.dj.insulink.shared.feature.statistics.domain

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.statistics.domain.model.GlucoseStatistics
import com.dj.insulink.shared.feature.statistics.domain.model.TimeInRangeBreakdown
import kotlin.math.sqrt

// Mirrors GlucoseLevelTag/DynamicLineChart's target range on the app side - duplicated rather
// than shared because those live in Android-only UI code, not commonMain. Keep in sync if the
// target range ever changes.
private const val DEFAULT_TARGET_LOW_MG_DL = 70
private const val DEFAULT_TARGET_HIGH_MG_DL = 126

/**
 * Pure statistics calculations over an already-loaded [GlucoseReading] list - no persistence, no
 * I/O. Callers (ViewModels) are responsible for fetching the relevant date range first.
 */
object StatisticsCalculator {

    fun calculateGlucoseStatistics(
        readings: List<GlucoseReading>,
        targetLowMgDl: Int = DEFAULT_TARGET_LOW_MG_DL,
        targetHighMgDl: Int = DEFAULT_TARGET_HIGH_MG_DL
    ): GlucoseStatistics? {
        if (readings.isEmpty()) return null

        val values = readings.map { it.value }
        val average = values.average()
        val variance = values.sumOf { value -> (value - average) * (value - average) } / values.size
        val total = values.size.toDouble()
        val belowCount = values.count { it < targetLowMgDl }
        val aboveCount = values.count { it > targetHighMgDl }
        val inRangeCount = values.size - belowCount - aboveCount

        return GlucoseStatistics(
            average = average,
            min = values.min(),
            max = values.max(),
            standardDeviation = sqrt(variance),
            readingCount = values.size,
            timeInRange = TimeInRangeBreakdown(
                belowPercent = belowCount / total * 100,
                inRangePercent = inRangeCount / total * 100,
                abovePercent = aboveCount / total * 100
            )
        )
    }
}
