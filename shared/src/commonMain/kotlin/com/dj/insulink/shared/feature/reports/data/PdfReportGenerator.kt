package com.dj.insulink.shared.feature.reports.data

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

interface PdfReportGenerator {
    suspend fun generate(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        unit: GlucoseUnit
    ): String
}

expect val isPdfReportSupported: Boolean
