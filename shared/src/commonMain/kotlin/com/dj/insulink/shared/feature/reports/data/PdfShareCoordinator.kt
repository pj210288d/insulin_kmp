package com.dj.insulink.shared.feature.reports.data

// Vidi PdfReportGenerator.kt za kontekst - Android actual je no-op (isPdfReportSupported sakriva
// ceo Reports tab na Android demo ekranu), iOS actual prezentuje UIActivityViewController (isti
// sistemski share sheet koji koristi bilo koja iOS app).
interface PdfShareCoordinator {
    fun share(filePath: String)
}
