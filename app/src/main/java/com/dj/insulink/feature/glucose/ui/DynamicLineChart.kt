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
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalBox
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// The chart always shows a single calendar day's worth of readings (see GlucoseViewModel's
// selectedDayStartMillis) - the x-axis is always formatted as a time of day, never a date.
private const val DAY_VIEW_TIME_FORMAT = "HH:mm"

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

            scrollState.scroll(Scroll.Absolute.Start)
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(),
            startAxis = VerticalAxis.rememberStart(),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    val index = x.toInt()
                    if (xValues.isNotEmpty() && index >= 0 && index < xValues.size) {
                        val timestamp = xValues[index]
                        SimpleDateFormat(DAY_VIEW_TIME_FORMAT, Locale.getDefault()).format(Date(timestamp))
                    } else {
                        "00:00"
                    }
                }
            ),
            decorations = decorations
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = scrollState,
        zoomState = rememberVicoZoomState(zoomEnabled = true)
    )
}
