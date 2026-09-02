package com.dj.insulink.shared.feature.statistics.domain

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StatisticsCalculatorTest {

    private fun reading(value: Int) = GlucoseReading(
        id = 0L, userId = "u1", timestamp = 0L, value = value, comment = ""
    )

    @Test
    fun calculateGlucoseStatistics_returnsNullForEmptyReadings() {
        assertNull(StatisticsCalculator.calculateGlucoseStatistics(emptyList()))
    }

    @Test
    fun calculateGlucoseStatistics_computesAverageMinMaxAndCount() {
        val readings = listOf(reading(100), reading(120), reading(140))

        val stats = StatisticsCalculator.calculateGlucoseStatistics(readings)!!

        assertEquals(120.0, stats.average, 0.0001)
        assertEquals(100, stats.min)
        assertEquals(140, stats.max)
        assertEquals(3, stats.readingCount)
    }

    @Test
    fun calculateGlucoseStatistics_computesStandardDeviation() {
        // values 100, 120, 140 -> mean 120, variance ((-20)^2+0^2+20^2)/3 = 266.67 -> stdDev ~16.33
        val readings = listOf(reading(100), reading(120), reading(140))

        val stats = StatisticsCalculator.calculateGlucoseStatistics(readings)!!

        assertEquals(16.3299, stats.standardDeviation, 0.001)
    }

    @Test
    fun calculateGlucoseStatistics_zeroVarianceGivesZeroStandardDeviation() {
        val readings = listOf(reading(110), reading(110), reading(110))

        val stats = StatisticsCalculator.calculateGlucoseStatistics(readings)!!

        assertEquals(0.0, stats.standardDeviation, 0.0001)
    }

    @Test
    fun calculateGlucoseStatistics_bucketsTimeInRangeUsingDefaultThresholds() {
        // default target range is 70-126 mg/dL
        val readings = listOf(
            reading(60),  // below
            reading(100), // in range
            reading(126), // in range (inclusive upper bound)
            reading(150)  // above
        )

        val stats = StatisticsCalculator.calculateGlucoseStatistics(readings)!!

        assertEquals(25.0, stats.timeInRange.belowPercent, 0.0001)
        assertEquals(50.0, stats.timeInRange.inRangePercent, 0.0001)
        assertEquals(25.0, stats.timeInRange.abovePercent, 0.0001)
    }

    @Test
    fun calculateGlucoseStatistics_respectsCustomThresholds() {
        val readings = listOf(reading(80), reading(200))

        val stats = StatisticsCalculator.calculateGlucoseStatistics(
            readings, targetLowMgDl = 90, targetHighMgDl = 180
        )!!

        assertEquals(50.0, stats.timeInRange.belowPercent, 0.0001)
        assertEquals(0.0, stats.timeInRange.inRangePercent, 0.0001)
        assertEquals(50.0, stats.timeInRange.abovePercent, 0.0001)
    }
}
