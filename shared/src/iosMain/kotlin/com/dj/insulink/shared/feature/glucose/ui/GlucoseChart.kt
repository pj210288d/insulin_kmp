package com.dj.insulink.shared.feature.glucose.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.time.timeOfDayLabel
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@Composable
actual fun GlucoseChart(readings: List<GlucoseReading>, unit: GlucoseUnit, modifier: Modifier) {
    val ordered = remember(readings) { readings.sortedBy { it.timestamp } }
    val values = remember(ordered, unit) {
        ordered.map { reading ->
            if (unit == GlucoseUnit.MMOL_L) {
                GlucoseUnit.convertMgDlToMmolL(reading.value.toDouble()).toFloat()
            } else {
                reading.value.toFloat()
            }
        }
    }
    val fixedMin = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MIN_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MIN_MMOL.toDouble()).toFloat()
    }
    val fixedMax = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MAX_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MAX_MMOL.toDouble()).toFloat()
    }
    val yAxisTicks = remember(unit) {
        Y_AXIS_TICKS_MMOL.map { tick ->
            if (unit == GlucoseUnit.MMOL_L) tick else GlucoseUnit.convertMmolLToMgDl(tick.toDouble()).toFloat()
        }
    }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = axisColor)

    Canvas(modifier = modifier) {
        val leftAxisWidth = 34.dp.toPx()
        val bottomAxisHeight = 18.dp.toPx()
        val plotLeft = leftAxisWidth
        val plotWidth = (size.width - leftAxisWidth).coerceAtLeast(0f)
        val plotHeight = (size.height - bottomAxisHeight).coerceAtLeast(0f)
        val range = (fixedMax - fixedMin).coerceAtLeast(1f)

        yAxisTicks.forEach { value ->
            val y = plotHeight - ((value - fixedMin) / range) * plotHeight
            drawLine(
                color = axisColor.copy(alpha = 0.15f),
                start = Offset(plotLeft, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            val layout = textMeasurer.measure(axisValueLabel(value, unit), style = labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(0f, (y - layout.size.height / 2f).coerceIn(0f, plotHeight - layout.size.height))
            )
        }

        if (values.isNotEmpty()) {
            val stepX = if (values.size > 1) plotWidth / (values.size - 1) else 0f
            val points = values.mapIndexed { index, value ->
                val clamped = value.coerceIn(fixedMin, fixedMax)
                Offset(
                    x = plotLeft + index * stepX,
                    y = plotHeight - ((clamped - fixedMin) / range) * plotHeight
                )
            }
            for (index in 0 until points.size - 1) {
                drawLine(color = InsulinkBlue, start = points[index], end = points[index + 1], strokeWidth = 6f)
            }
            points.forEach { point ->
                drawCircle(color = InsulinkBlue, radius = 8f, center = point)
            }

            val labelIndices = listOf(0, points.size / 2, points.size - 1).distinct()
            labelIndices.forEach { index ->
                val layout = textMeasurer.measure(timeOfDayLabel(ordered[index].timestamp), style = labelStyle)
                val x = (points[index].x - layout.size.width / 2f)
                    .coerceIn(plotLeft, size.width - layout.size.width)
                drawText(textLayoutResult = layout, topLeft = Offset(x, plotHeight + 2.dp.toPx()))
            }
        }
    }
}

private fun axisValueLabel(value: Float, unit: GlucoseUnit): String {
    return when (unit) {
        GlucoseUnit.MG_DL -> value.toInt().toString()
        GlucoseUnit.MMOL_L -> {
            val scaled = kotlin.math.round(value * 10).toLong()
            val whole = scaled / 10
            val fraction = kotlin.math.abs(scaled % 10)
            "$whole.$fraction"
        }
    }
}

private const val FIXED_MIN_MMOL = 2f
private const val FIXED_MAX_MMOL = 25f
private val Y_AXIS_TICKS_MMOL = listOf(3f, 6f, 9f, 12f, 15f, 18f, 21f)
private val InsulinkBlue = androidx.compose.ui.graphics.Color(0xFF4A7BF6)
