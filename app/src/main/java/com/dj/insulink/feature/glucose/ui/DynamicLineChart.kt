package com.dj.insulink.feature.glucose.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import com.dj.insulink.R
import com.dj.insulink.feature.glucose.ui.viewmodel.GlucoseReadingTimespan
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DynamicLineChart(
    xValues: List<Long>,
    yValues: List<Int>,
    timespan: GlucoseReadingTimespan,
    modifier: Modifier,
    glucoseUnit: GlucoseUnit = GlucoseUnit.MG_DL
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val scrollState = rememberVicoScrollState(
        initialScroll = Scroll.Absolute.End
    )

    val dateFormat = remember(timespan) {
        when (timespan) {
            GlucoseReadingTimespan.LAST_DAY -> "HH:mm"
            GlucoseReadingTimespan.LAST_3_DAYS -> "d MMM HH:mm"
            GlucoseReadingTimespan.LAST_WEEK -> "d MMM"
            GlucoseReadingTimespan.LAST_MONTH -> "d MMM"
            GlucoseReadingTimespan.ALL_READINGS -> {
                if (xValues.isNotEmpty()) {
                    val timeRange = (xValues.maxOrNull() ?: 0L) - (xValues.minOrNull() ?: 0L)
                    when {
                        timeRange <= 7 * 24 * 60 * 60 * 1000L -> "d MMM"
                        timeRange <= 365 * 24 * 60 * 60 * 1000L -> "d MMM"
                        else -> "MMM yyyy"
                    }
                } else {
                    "d MMM"
                }
            }
        }
    }

    val convertedYValues = remember(yValues, glucoseUnit) {
        if (glucoseUnit == GlucoseUnit.MMOL_L) {
            yValues.map { GlucoseUnit.convertMgDlToMmolL(it.toDouble()).toFloat() }
        } else {
            yValues.map { it.toFloat() }
        }
    }

    LaunchedEffect(xValues, convertedYValues, timespan) {
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
            startAxis = VerticalAxis.rememberStart(
                title = stringResource(R.string.new_reading_text_field_label, glucoseUnit.suffix),
                titleComponent = rememberTextComponent(
                    color = MaterialTheme.colorScheme.onBackground
                )
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = CartesianValueFormatter { context, x, _ ->
                    val index = x.toInt()
                    if (xValues.isNotEmpty() && index >= 0 && index < xValues.size) {
                        val timestamp = xValues[index]
                        SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(timestamp))
                    } else {
                        when (timespan) {
                            GlucoseReadingTimespan.LAST_DAY -> "00:00"
                            GlucoseReadingTimespan.LAST_3_DAYS -> "1 Jan 00:00"
                            else -> "1 Jan"
                        }
                    }
                },
                title = getAxisTitle(timespan),
                titleComponent = rememberTextComponent(
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        ),
        modelProducer = modelProducer,
        modifier = modifier,
        scrollState = scrollState,
        zoomState = rememberVicoZoomState(zoomEnabled = true)
    )
}

@Composable
private fun getAxisTitle(timespan: GlucoseReadingTimespan): String {
    return when (timespan) {
        GlucoseReadingTimespan.LAST_DAY -> stringResource(R.string.chart_time_axis_title)
        else -> stringResource(R.string.glucose_screen_date_axis_title)
    }
}