package com.dj.insulink.shared.feature.reports.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidPdfReportGenerator(
    private val context: Context
) : PdfReportGenerator {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val fileNameFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun generate(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        unit: GlucoseUnit
    ): String {
        val fileName = "glucose_report_${fileNameFormatter.format(Date(startDate))}_to_" +
            "${fileNameFormatter.format(Date(endDate))}.pdf"
        val outputFile = File(context.cacheDir, fileName)

        val pdfWriter = PdfWriter(outputFile)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        addHeader(document)
        addReportPeriod(document, startDate, endDate)
        addSummaryStatistics(document, readings, unit)
        addReadingsTable(document, readings, unit)
        addFooter(document)

        document.close()
        return outputFile.absolutePath
    }

    private fun addHeader(document: Document) {
        try {
            val logoResourceId = context.applicationInfo.icon
            if (logoResourceId != 0) {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResourceId)
                val logoImage = Image(ImageDataFactory.create(bitmapToByteArray(logoBitmap)))
                    .setWidth(50f)
                    .setHeight(50f)
                document.add(logoImage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        document.add(
            Paragraph("InsuLink - Glucose Report")
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )
    }

    private fun addReportPeriod(document: Document, startDate: Long, endDate: Long) {
        val startDateStr = dateOnlyFormatter.format(Date(startDate))
        val endDateStr = dateOnlyFormatter.format(Date(endDate))

        document.add(
            Paragraph("Report Period: $startDateStr - $endDateStr")
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15f)
        )

        document.add(
            Paragraph("Generated on: ${dateFormatter.format(Date())}")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )
    }

    private fun addSummaryStatistics(document: Document, readings: List<GlucoseReading>, glucoseUnit: GlucoseUnit) {
        if (readings.isEmpty()) return

        val avgGlucose = readings.map { it.value }.average()
        val minGlucose = readings.minOf { it.value }
        val maxGlucose = readings.maxOf { it.value }
        val totalReadings = readings.size

        val timeInRange = readings.count { it.value in LOW_THRESHOLD..HIGH_THRESHOLD }
        val timeInRangePercent = (timeInRange.toDouble() / totalReadings * 100)

        val unit = glucoseUnit.suffix

        document.add(
            Paragraph("Summary Statistics")
                .setFontSize(16f)
                .setMarginBottom(10f)
        )

        val summaryTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)

        summaryTable.addCell(createStatsCell("Total Readings:", totalReadings.toString()))
        summaryTable.addCell(createStatsCell("Average Glucose:", "${glucoseUnit.formatValue(avgGlucose)} $unit"))
        summaryTable.addCell(createStatsCell("Minimum:", "${glucoseUnit.formatValue(minGlucose)} $unit"))
        summaryTable.addCell(createStatsCell("Maximum:", "${glucoseUnit.formatValue(maxGlucose)} $unit"))
        summaryTable.addCell(
            createStatsCell(
                "Time in Range (${glucoseUnit.formatValue(LOW_THRESHOLD.toDouble())}–" +
                    "${glucoseUnit.formatValue(HIGH_THRESHOLD.toDouble())} $unit):",
                "${timeInRangePercent.toInt()}%"
            )
        )
        summaryTable.addCell(createStatsCell("Readings in Range:", "$timeInRange/$totalReadings"))

        document.add(summaryTable)
    }

    private fun addReadingsTable(document: Document, readings: List<GlucoseReading>, glucoseUnit: GlucoseUnit) {
        document.add(
            Paragraph("Detailed Readings")
                .setFontSize(16f)
                .simulateBold()
                .setMarginBottom(10f)
        )

        val table = Table(3)
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Date & Time"))
        table.addHeaderCell(createHeaderCell("Glucose (${glucoseUnit.suffix})"))
        table.addHeaderCell(createHeaderCell("Comment"))

        readings.sortedBy { it.timestamp }.forEach { reading ->
            table.addCell(createDataCell(dateFormatter.format(Date(reading.timestamp))))
            table.addCell(createDataCell(glucoseUnit.formatValue(reading.value)))
            table.addCell(createDataCell(reading.comment.takeIf { it.isNotBlank() } ?: "-"))
        }

        document.add(table)
    }

    private fun addFooter(document: Document) {
        document.add(
            Paragraph(
                "\nThis report was generated by InsuLink app. Please consult with your " +
                    "healthcare provider for medical advice."
            )
                .setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20f)
                .simulateItalic()
        )
    }

    private fun createStatsCell(label: String, value: String): Cell {
        val cell = Cell()
        cell.add(Paragraph("$label $value").setFontSize(11f))
        cell.setPadding(5f)
        return cell
    }

    private fun createHeaderCell(text: String): Cell {
        val cell = Cell()
        cell.add(Paragraph(text).simulateBold().setFontSize(12f))
        cell.setBackgroundColor(ColorConstants.LIGHT_GRAY)
        cell.setPadding(8f)
        cell.setTextAlignment(TextAlignment.CENTER)
        return cell
    }

    private fun createDataCell(text: String): Cell {
        val cell = Cell()
        cell.add(Paragraph(text).setFontSize(10f))
        cell.setPadding(5f)
        return cell
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private companion object {
        const val LOW_THRESHOLD = 70
        const val HIGH_THRESHOLD = 180
    }
}
