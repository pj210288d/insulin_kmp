package com.dj.insulink.shared.feature.settings.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GlucoseUnitTest {

    @Test
    fun formatValueIntForMgDlReturnsPlainIntegerString() {
        assertEquals("120", GlucoseUnit.MG_DL.formatValue(120))
    }

    @Test
    fun formatValueIntForMmolLConvertsAndFormatsWithOneDecimal() {
        // 180 / 18.0182 = 9.9888 -> "10.0"
        assertEquals("10.0", GlucoseUnit.MMOL_L.formatValue(180))
    }

    @Test
    fun formatValueDoubleForMgDlTruncatesToInteger() {
        assertEquals("120", GlucoseUnit.MG_DL.formatValue(120.9))
    }

    @Test
    fun formatValueDoubleForMmolLConvertsAndFormatsWithOneDecimal() {
        // 90.091 / 18.0182 = 5.0 -> "5.0"
        assertEquals("5.0", GlucoseUnit.MMOL_L.formatValue(90.091))
    }

    @Test
    fun fromKeyReturnsMatchingUnitForKnownKeys() {
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromKey("mg_dl"))
        assertEquals(GlucoseUnit.MMOL_L, GlucoseUnit.fromKey("mmol_l"))
    }

    @Test
    fun fromKeyFallsBackToMgDlForUnknownKey() {
        assertEquals(GlucoseUnit.MG_DL, GlucoseUnit.fromKey("unknown"))
    }

    @Test
    fun convertMgDlToMmolLDividesByTheConversionFactor() {
        assertEquals(10.0, GlucoseUnit.convertMgDlToMmolL(180.182), 0.0001)
    }

    @Test
    fun convertMmolLToMgDlMultipliesByTheConversionFactor() {
        assertEquals(180.182, GlucoseUnit.convertMmolLToMgDl(10.0), 0.0001)
    }

    @Test
    fun conversionRoundTripsBackToTheOriginalValue() {
        val original = 137.0
        val roundTripped = GlucoseUnit.convertMmolLToMgDl(GlucoseUnit.convertMgDlToMmolL(original))
        assertEquals(original, roundTripped, 0.0001)
    }

    @Test
    fun everyUnitHasANonBlankKeyLabelAndSuffix() {
        GlucoseUnit.entries.forEach { unit ->
            assertTrue(unit.key.isNotBlank())
            assertTrue(unit.label.isNotBlank())
            assertTrue(unit.suffix.isNotBlank())
        }
    }
}
