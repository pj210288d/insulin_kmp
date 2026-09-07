package com.dj.insulink.shared.feature.reports.di

import com.dj.insulink.shared.feature.reports.data.AndroidPdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.AndroidPdfShareCoordinator
import com.dj.insulink.shared.feature.reports.data.PdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.PdfShareCoordinator
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformReportsModule(): Module = module {
    single<PdfReportGenerator> { AndroidPdfReportGenerator() }
    single<PdfShareCoordinator> { AndroidPdfShareCoordinator() }
}
