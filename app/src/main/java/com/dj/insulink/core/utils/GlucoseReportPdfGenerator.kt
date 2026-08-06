package com.dj.insulink.feature.dataREMOVE.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.dj.insulink.R
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GlucoseReportPdfGenerator(private val context: Context) {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val dateOnlyFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    suspend fun generatePdf(
        readings: List<GlucoseReading>,
        startDate: Long,
        endDate: Long,
        outputFile: File,
        glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            addHeader(document)

            addReportPeriod(document, startDate, endDate)

            addSummaryStatistics(document, readings, glucoseUnit)

            addReadingsTable(document, readings, glucoseUnit)

            addFooter(document)

            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun addHeader(document: Document) {
        try {
            val logoResourceId = R.drawable.ic_insulink_logo
            if (logoResourceId != 0) {
                val logoBitmap = BitmapFactory.decodeResource(context.resources, logoResourceId)
                val logoByteArray = bitmapToByteArray(logoBitmap)
                val logoImage = Image(ImageDataFactory.create(logoByteArray))
                    .setWidth(50f)
                    .setHeight(50f)
                document.add(logoImage)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        document.add(
            Paragraph(context.getString(R.string.pdf_title))
                .setFontSize(24f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20f)
        )
    }

    private fun addReportPeriod(document: Document, startDate: Long, endDate: Long) {
        val startDateStr = dateOnlyFormatter.format(Date(startDate))
        val endDateStr = dateOnlyFormatter.format(Date(endDate))

        document.add(
            Paragraph(context.getString(R.string.pdf_report_period, startDateStr, endDateStr))
                .setFontSize(14f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15f)
        )

        document.add(
            Paragraph(context.getString(R.string.pdf_generated_on, dateFormatter.format(Date())))
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

        val timeInRange = readings.count { it.value in 70..180 }
        val timeInRangePercent = (timeInRange.toDouble() / totalReadings * 100)

        val unit = glucoseUnit.suffix

        document.add(
            Paragraph(context.getString(R.string.pdf_summary_title))
                .setFontSize(16f)
                .setMarginBottom(10f)
        )

        val summaryTable = Table(2)
            .setWidth(UnitValue.createPercentValue(100f))
            .setMarginBottom(20f)

        summaryTable.addCell(createStatsCell(context.getString(R.string.pdf_total_readings), totalReadings.toString()))
        summaryTable.addCell(createStatsCell(context.getString(R.string.pdf_average_glucose), "${glucoseUnit.formatValue(avgGlucose)} $unit"))
        summaryTable.addCell(createStatsCell(context.getString(R.string.pdf_minimum), "${glucoseUnit.formatValue(minGlucose)} $unit"))
        summaryTable.addCell(createStatsCell(context.getString(R.string.pdf_maximum), "${glucoseUnit.formatValue(maxGlucose)} $unit"))
        summaryTable.addCell(createStatsCell(
            context.getString(R.string.pdf_time_in_range, glucoseUnit.formatValue(LOW_THRESHOLD), glucoseUnit.formatValue(HIGH_THRESHOLD), unit),
            "${timeInRangePercent.toInt()}%"
        ))
        summaryTable.addCell(createStatsCell(context.getString(R.string.pdf_readings_in_range), "$timeInRange/$totalReadings"))

        document.add(summaryTable)
    }

    private fun addReadingsTable(document: Document, readings: List<GlucoseReading>, glucoseUnit: GlucoseUnit) {
        document.add(
            Paragraph(context.getString(R.string.pdf_detailed_readings))
                .setFontSize(16f)
                .simulateBold()
                .setMarginBottom(10f)
        )

        val table = Table(3)
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell(context.getString(R.string.pdf_date_time_header)))
        table.addHeaderCell(createHeaderCell(context.getString(R.string.pdf_glucose_header, glucoseUnit.suffix)))
        table.addHeaderCell(createHeaderCell(context.getString(R.string.pdf_comment_header)))

        readings.sortedBy { it.timestamp }.forEach { reading ->
            table.addCell(createDataCell(dateFormatter.format(Date(reading.timestamp))))
            table.addCell(createDataCell(glucoseUnit.formatValue(reading.value)))
            table.addCell(createDataCell(reading.comment.takeIf { it.isNotBlank() } ?: "-"))
        }

        document.add(table)
    }

    private fun addFooter(document: Document) {
        document.add(
            Paragraph("\n${context.getString(R.string.pdf_footer)}")
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

    companion object {
        private const val LOW_THRESHOLD = 70
        private const val HIGH_THRESHOLD = 180
    }
}