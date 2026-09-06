package com.dj.insulink.shared.feature.reports.data

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

// Faza 6 (Reports PDF, najniži prioritet - Statistics tab već pokriva glavnu vrednost bez PDF
// izvoza). Android-ov PRAVI Reports ekran (app/feature/reports) već ima potpuno ispravan,
// proveren PDF izvoz preko iText7 (GlucoseReportPdfGenerator u :app) - taj kod ostaje netaknut.
// iText7 zavisnost postoji SAMO u :app-ovom build.gradle.kts, ne u :shared-u, i dodavanje je ne
// vredi za deljeni demo ekran - isti princip kao Google Sign-In/Reminders notifikacije: Android
// actual/podrška je namerno izostavljena (isPdfReportSupported = false sakriva dugme na Android
// strani), iOS actual koristi UIGraphicsPDFRenderer (sistemski framework, besplatan preko
// Kotlin/Native ObjC interop-a - isti mehanizam kao NSUserDefaults/NSFileManager).
interface PdfReportGenerator {
    /** Vraća apsolutnu putanju do generisanog PDF fajla (privremeni direktorijum). */
    suspend fun generate(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        unit: GlucoseUnit
    ): String
}

expect val isPdfReportSupported: Boolean
