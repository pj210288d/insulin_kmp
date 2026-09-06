package com.dj.insulink.shared.feature.reports.data

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

// Vidi opširan komentar u commonMain PdfReportGenerator.kt - namerno no-op na Android-u
// (isPdfReportSupported = false sakriva Reports tab na deljenom demo ekranu pre nego što se
// ovo ikad pozove).
class AndroidPdfReportGenerator : PdfReportGenerator {
    override suspend fun generate(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        unit: GlucoseUnit
    ): String {
        throw UnsupportedOperationException("PDF izveštaj u deljenom demo ekranu nije podržan na Android-u")
    }
}

class AndroidPdfShareCoordinator : PdfShareCoordinator {
    override fun share(filePath: String) {
        // no-op - vidi komentar iznad
    }
}
