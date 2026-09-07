package com.dj.insulink.feature.glucose.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
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

// The chart always shows a single calendar day's worth of readings (see GlucoseViewModel's
// selectedDayStartMillis) - the x-axis is always formatted as a time of day, never a date.
private const val DAY_VIEW_TIME_FORMAT = "HH:mm"

// 2026-09-07: korisnik tražio da Y osa ima fiksne vrednosti/opseg umesto auto-skaliranja
// - isti pravi klinički opseg nezavisno od dnevnih vrednosti (poređenje grafika različitih dana),
// isto rešenje kao shared/commonMain GlucoseScreen.kt (SimpleLineChart) - vidi Y_AXIS_TICKS_MMOL
// tamo, iste vrednosti, iz istog razloga.
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

// Vico-ov ugrađeni VerticalAxis.ItemPlacer.step()/count() ne garantuje TAČNO ove vrednosti (step
// mod automatski bira "lep" korak na osnovu opsega podataka) - ovaj custom ItemPlacer uvek vraća
// baš Y_AXIS_TICKS_MMOL (konvertovano u jedinicu), bez obzira na visinu/opseg. Margine gore/dole
// prate istu formulu kao Vico-ov Default (Center pozicija labela, shiftTopLines = true - vidi
// DefaultVerticalAxisItemPlacer u vico-core izvoru).
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DynamicLineChart(
    xValues: List<Long>,
    yValues: List<Int>,
    modifier: Modifier,
    glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End
    )

    val convertedYValues = remember(yValues, glucoseUnit) {
        if (glucoseUnit == GlucoseUnit.MMOL_L) {
            yValues.map { GlucoseUnit.convertMgDlToMmolL(it.toDouble()).toFloat() }
        } else {
            yValues.map { it.toFloat() }
        }
    }

    val fixedMinY = remember(glucoseUnit) {
        if (glucoseUnit == GlucoseUnit.MMOL_L) FIXED_MIN_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MIN_MMOL)
    }
    val fixedMaxY = remember(glucoseUnit) {
        if (glucoseUnit == GlucoseUnit.MMOL_L) FIXED_MAX_MMOL else GlucoseUnit.convertMmolLToMgDl(FIXED_MAX_MMOL)
    }
    val yAxisTicks = remember(glucoseUnit) {
        Y_AXIS_TICKS_MMOL.map { tick ->
            if (glucoseUnit == GlucoseUnit.MMOL_L) tick else GlucoseUnit.convertMmolLToMgDl(tick)
        }
    }
    val yAxisItemPlacer = remember(yAxisTicks) { FixedVerticalAxisItemPlacer(yAxisTicks) }

    val targetRange = remember(glucoseUnit) {
        if (glucoseUnit == GlucoseUnit.MMOL_L) {
            GlucoseUnit.convertMgDlToMmolL(LOWER_GLUCOSE_THRESHOLD.toDouble())..
                GlucoseUnit.convertMgDlToMmolL(HIGH_GLUCOSE_THRESHOLD.toDouble())
        } else {
            LOWER_GLUCOSE_THRESHOLD.toDouble()..HIGH_GLUCOSE_THRESHOLD.toDouble()
        }
    }
    val targetRangeBox = rememberShapeComponent(fill = fill(InsulinkTheme.colors.glucoseNormal.copy(alpha = 0.15f)))
    val decorations = remember(targetRange, targetRangeBox) {
        listOf(HorizontalBox(y = { targetRange }, box = targetRangeBox))
    }

    LaunchedEffect(xValues, convertedYValues) {
        if (xValues.isNotEmpty() && convertedYValues.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(
                        x = xValues.indices.map { it.toFloat() },
                        y = convertedYValues
                    )
                }
            }

            // Osa sad ide hronološki levo->desno (najstarije->najnovije, vidi fix u
            // GlucoseScreen.kt gde je uklonjen .reversed() koji je gurao najnovije očitavanje na
            // levu stranu). Da bi najnovije očitavanje i dalje bilo odmah vidljivo bez skrolovanja,
            // pomeramo na End (desno), ne Start.
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
                valueFormatter = CartesianValueFormatter { _, value, _ -> axisValueLabel(value, glucoseUnit) }
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
                // Korisnik tražio da se uklone vertikalne isprekidane guideline linije koje Vico
                // podrazumevano crta na svakoj X-osa oznaci.
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
