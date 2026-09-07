package com.dj.insulink.shared.feature.reports.di

import com.dj.insulink.shared.feature.reports.data.IosPdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.IosPdfShareCoordinator
import com.dj.insulink.shared.feature.reports.data.PdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.PdfShareCoordinator
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformReportsModule(): Module = module {
    single<PdfReportGenerator> { IosPdfReportGenerator() }
    single<PdfShareCoordinator> { IosPdfShareCoordinator() }
}
