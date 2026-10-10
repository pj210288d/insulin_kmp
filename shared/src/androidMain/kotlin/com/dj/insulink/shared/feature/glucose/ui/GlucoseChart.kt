package com.dj.insulink.shared.feature.glucose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.glucose.domain.model.HIGH_GLUCOSE_THRESHOLD
import com.dj.insulink.shared.feature.glucose.domain.model.LOWER_GLUCOSE_THRESHOLD
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalBox
import com.patrykandpatrick.vico.core.common.Position
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private const val DAY_VIEW_TIME_FORMAT = "HH:mm"

private const val FIXED_MIN_MMOL = 2.0
private const val FIXED_MAX_MMOL = 25.0
private val Y_AXIS_TICKS_MMOL = listOf(3.0, 6.0, 9.0, 12.0, 15.0, 18.0, 21.0)

private fun axisValueLabel(value: Double, unit: GlucoseUnit): String = when (unit) {
    GlucoseUnit.MG_DL -> value.toInt().toString()
    GlucoseUnit.MMOL_L -> {
        val scaled = kotlin.math.round(value * 10).toLong()
        val whole = scaled / 10
        val fraction = kotlin.math.abs(scaled % 10)
        "$whole.$fraction"
    }
}

private class FixedVerticalAxisItemPlacer(private val values: List<Double>) : VerticalAxis.ItemPlacer {
    override fun getLabelValues(
        context: CartesianDrawingContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: Axis.Position.Vertical
    ): List<Double> = values

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: Axis.Position.Vertical
    ): List<Double> = values

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        position: Axis.Position.Vertical
    ): List<Double> = values

    override fun getTopLayerMargin(
        context: CartesianMeasuringContext,
        verticalLabelPosition: Position.Vertical,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = when (verticalLabelPosition) {
        Position.Vertical.Top -> maxLabelHeight + maxLineThickness / 2f
        Position.Vertical.Center -> (max(maxLabelHeight, maxLineThickness) + maxLineThickness) / 2f
        else -> maxLineThickness
    }

    override fun getBottomLayerMargin(
        context: CartesianMeasuringContext,
        verticalLabelPosition: Position.Vertical,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = when (verticalLabelPosition) {
        Position.Vertical.Top -> maxLineThickness
        Position.Vertical.Center -> (max(maxLabelHeight, maxLineThickness) + maxLineThickness) / 2f
        else -> maxLabelHeight + maxLineThickness / 2f
    }
}

@Composable
actual fun GlucoseChart(readings: List<GlucoseReading>, unit: GlucoseUnit, modifier: Modifier) {
    val ordered = remember(readings) { readings.sortedBy { it.timestamp } }
    val xValues = remember(ordered) { ordered.map { it.timestamp } }
    val yValues = remember(ordered) { ordered.map { it.value } }

    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState(initialScroll = Scroll.Absolute.End)

    val convertedYValues = remember(yValues, unit) {
        if (unit == GlucoseUnit.MMOL_L) {
            yValues.map { GlucoseUnit.convertMgDlToMmolL(it.toDouble()).toFloat() }
        } else {
            yValues.map { it.toFloat() }
        }
    }

    val fixedMinY = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MIN_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MIN_MMOL)
    }
    val fixedMaxY = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) FIXED_MAX_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MAX_MMOL)
    }
    val yAxisTicks = remember(unit) {
        Y_AXIS_TICKS_MMOL.map { tick ->
            if (unit == GlucoseUnit.MMOL_L) tick else GlucoseUnit.convertMmolLToMgDl(tick)
        }
    }
    val yAxisItemPlacer = remember(yAxisTicks) { FixedVerticalAxisItemPlacer(yAxisTicks) }

    val targetRange = remember(unit) {
        if (unit == GlucoseUnit.MMOL_L) {
            GlucoseUnit.convertMgDlToMmolL(LOWER_GLUCOSE_THRESHOLD.toDouble())..
                GlucoseUnit.convertMgDlToMmolL(HIGH_GLUCOSE_THRESHOLD.toDouble())
        } else {
            LOWER_GLUCOSE_THRESHOLD.toDouble()..HIGH_GLUCOSE_THRESHOLD.toDouble()
        }
    }
    val targetRangeBox = rememberShapeComponent(fill = fill(GlucoseNormalGreen))
    val decorations = remember(targetRange, targetRangeBox) {
        listOf(HorizontalBox(y = { targetRange }, box = targetRangeBox))
    }

    LaunchedEffect(xValues, convertedYValues) {
        if (xValues.isNotEmpty() && convertedYValues.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(x = xValues.indices.map { it.toFloat() }, y = convertedYValues)
                }
            }
            scrollState.scroll(Scroll.Absolute.End)
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = remember(fixedMinY, fixedMaxY) {
                    CartesianLayerRangeProvider.fixed(minY = fixedMinY, maxY = fixedMaxY)
                }
            ),
            startAxis = VerticalAxis.rememberStart(
                itemPlacer = yAxisItemPlacer,
                valueFormatter = CartesianValueFormatter { _, value, _ -> axisValueLabel(value, unit) }
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    val index = x.toInt()
                    if (xValues.isNotEmpty() && index >= 0 && index < xValues.size) {
                        val timestamp = xValues[index]
                        SimpleDateFormat(DAY_VIEW_TIME_FORMAT, Locale.getDefault()).format(Date(timestamp))
                    } else {
                        "00:00"
                    }
                },
                guideline = null
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = scrollState,
        zoomState = rememberVicoZoomState(zoomEnabled = true)
    )
}

private val GlucoseNormalGreen = androidx.compose.ui.graphics.Color(0xFF66BB6A).copy(alpha = 0.15f)
