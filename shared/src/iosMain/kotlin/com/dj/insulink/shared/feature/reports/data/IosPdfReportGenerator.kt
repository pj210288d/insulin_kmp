package com.dj.insulink.shared.feature.reports.data

import com.dj.insulink.shared.core.time.dateOnlyLabel
import com.dj.insulink.shared.core.time.dateTimeLabel
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreGraphics.CGContextSelectFont
import platform.CoreGraphics.CGContextSetRGBFillColor
import platform.CoreGraphics.CGContextSetTextDrawingMode
import platform.CoreGraphics.CGContextSetTextMatrix
import platform.CoreGraphics.CGContextShowTextAtPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGTextDrawingMode
import platform.CoreGraphics.CGTextEncoding
import platform.Foundation.NSTemporaryDirectory
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

private const val PAGE_WIDTH = 595.0 // A4 @ 72dpi
private const val PAGE_HEIGHT = 842.0
private const val MARGIN = 40.0
private const val LOW_THRESHOLD = 70
private const val HIGH_THRESHOLD = 180
private const val LINE_SPACING = 6.0

// Faza 6 (Reports PDF) - vidi opširan komentar u commonMain PdfReportGenerator.kt. Tekst se crta
// preko starijeg CGContextShowTextAtPoint/CGContextSelectFont C API-ja (MacRoman kodiranje) umesto
// preko NSString.drawAtPoint(withAttributes:) - taj poziv (i NSData.writeToFile) se dosledno nije
// razrešavao ni na metadata ni na pravom iosSimulatorArm64 target-u iz razloga koji nije utvrđen
// (2026-09-06) - dat prioritet radnom rešenju nad daljim istraživanjem s obzirom da je ovo
// najniži prioritet faze. POSLEDICA: MacRoman ne pokriva srpske dijakritike (č/ć/š/ž/đ), pa se
// tekst u PDF-u transliteruje u ASCII (transliterate()) - kozmetički kompromis, ne utiče na
// tačnost podataka. Fajl se upisuje preko POSIX fopen/fwrite (NSData.bytes/length su osnovna
// Foundation svojstva, ne kategorije, pouzdano razrešena) umesto NSData.writeToFile.
@OptIn(ExperimentalForeignApi::class)
class IosPdfReportGenerator : PdfReportGenerator {

    override suspend fun generate(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        unit: GlucoseUnit
    ): String {
        val sorted = readings.sortedBy { it.timestamp }
        val pageRect = CGRectMake(0.0, 0.0, PAGE_WIDTH, PAGE_HEIGHT)
        val renderer = UIGraphicsPDFRenderer(bounds = pageRect, format = UIGraphicsPDFRendererFormat())

        val data = renderer.PDFDataWithActions { pdfContext ->
            val cgContext = pdfContext!!.CGContext
            var y = MARGIN
            var pageOpen = false

            fun newPage() {
                pdfContext.beginPage()
                // UIGraphicsPDFRenderer-ov CGContext ima top-left/Y-dole CTM (UIKit konvencija),
                // ali CGContextShowTextAtPoint (stariji Quartz API) crta glifove u sopstvenoj,
                // FIKSNOJ tekst-matrici koja pretpostavlja Y-gore orijentaciju bez obzira na CTM -
                // otud tekst izlazio naopako/flipovano (potvrđeno na fizičkom testu 2026-09-07).
                // Standardan fix: eksplicitno postaviti tekst-matricu da poništi tu razliku.
                CGContextSetTextMatrix(cgContext, CGAffineTransformMakeScale(1.0, -1.0))
                pageOpen = true
                y = MARGIN
            }

            fun ensureSpace(lineHeight: Double) {
                if (!pageOpen || y + lineHeight > PAGE_HEIGHT - MARGIN) newPage()
            }

            fun drawText(text: String, size: Double, bold: Boolean = false) {
                ensureSpace(size + LINE_SPACING)
                val fontName = if (bold) "Helvetica-Bold" else "Helvetica"
                CGContextSelectFont(cgContext, fontName, size, CGTextEncoding.kCGEncodingMacRoman)
                CGContextSetTextDrawingMode(cgContext, CGTextDrawingMode.kCGTextFill)
                CGContextSetRGBFillColor(cgContext, 0.0, 0.0, 0.0, 1.0)
                val ascii = transliterate(text)
                // NE "PAGE_HEIGHT - y" - UIGraphicsPDFRenderer-ov kontekst je već top-down
                // (UIKit konvencija), taj dodatni flip je pozicionirao redove u obrnutom
                // redosledu (potvrđeno na fizičkom testu 2026-09-07 - naslov se pojavio na dnu).
                CGContextShowTextAtPoint(cgContext, MARGIN, y + size, ascii, ascii.length.toULong())
                y += size + LINE_SPACING
            }

            newPage()
            drawText("Insulink - Izvestaj o glukozi", 20.0, bold = true)
            drawText("Period: ${dateOnlyLabel(startDate)} - ${dateOnlyLabel(endDate)}", 12.0)
            y += 8.0

            if (sorted.isNotEmpty()) {
                val values = sorted.map { it.value }
                val avg = values.average()
                val min = values.min()
                val max = values.max()
                val inRange = values.count { it in LOW_THRESHOLD..HIGH_THRESHOLD }
                val inRangePercent = (inRange.toDouble() / values.size * 100).toInt()

                drawText("Statistika", 16.0, bold = true)
                drawText("Ukupno ocitavanja: ${values.size}", 11.0)
                drawText("Prosek: ${unit.formatValue(avg)} ${unit.suffix}", 11.0)
                drawText("Min / Maks: ${unit.formatValue(min)} / ${unit.formatValue(max)} ${unit.suffix}", 11.0)
                drawText("U cilju ($LOW_THRESHOLD-$HIGH_THRESHOLD ${unit.suffix}): $inRangePercent% ($inRange/${values.size})", 11.0)
                y += 12.0

                drawText("Ocitavanja", 16.0, bold = true)
                sorted.forEach { reading ->
                    val comment = reading.comment.takeIf { it.isNotBlank() } ?: "-"
                    drawText(
                        "${dateTimeLabel(reading.timestamp)}   ${unit.formatValue(reading.value)} ${unit.suffix}   $comment",
                        10.0
                    )
                }
            } else {
                drawText("Nema ocitavanja u izabranom periodu.", 12.0)
            }
        }

        val fileName = "insulink_izvestaj_${startDate}_${endDate}.pdf"
        val path = NSTemporaryDirectory() + fileName
        writeNSDataToPath(data, path)
        return path
    }

    private fun writeNSDataToPath(data: platform.Foundation.NSData, path: String) {
        val file = fopen(path, "wb") ?: error("Ne mogu da otvorim fajl za pisanje: $path")
        try {
            val bytes = data.bytes
            val length = data.length
            if (bytes != null && length > 0uL) {
                fwrite(bytes, 1uL, length, file)
            }
        } finally {
            fclose(file)
        }
    }

    // MacRoman (CGContextShowTextAtPoint) ne pokriva srpske Latin dijakritike - transliteruje ih
    // u najbliži ASCII ekvivalent da tekst ostane čitljiv umesto da se prikaže kao pogrešan glif.
    private fun transliterate(text: String): String = buildString {
        text.forEach { char ->
            append(
                when (char) {
                    'č', 'ć' -> 'c'
                    'Č', 'Ć' -> 'C'
                    'š' -> 's'
                    'Š' -> 'S'
                    'ž' -> 'z'
                    'Ž' -> 'Z'
                    'đ' -> 'd'
                    'Đ' -> 'D'
                    else -> char
                }
            )
        }
    }
}
