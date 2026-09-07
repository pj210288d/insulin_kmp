package com.dj.insulink.feature.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.statistics.domain.model.GlucoseStatistics
import com.dj.insulink.shared.feature.statistics.domain.model.StatisticsRange
import java.util.Locale

@Composable
fun StatisticsScreen(params: StatisticsScreenParams) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(InsulinkTheme.dimens.commonPadding16)
    ) {
        RangeSelector(
            selectedRange = params.selectedRange.value,
            onRangeSelected = params.onRangeSelected
        )

        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))

        val statistics = params.statistics.value
        if (statistics == null) {
            EmptyState(stringResource(R.string.statistics_no_data))
        } else {
            StatisticsSummary(statistics, params.glucoseUnit.value)
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: StatisticsRange,
    onRangeSelected: (StatisticsRange) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
    ) {
        RANGE_OPTIONS.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = { Text(stringResource(range.labelRes())) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = InsulinkTheme.colors.insulinkBlue,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun StatisticsSummary(statistics: GlucoseStatistics, glucoseUnit: GlucoseUnit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
    ) {
        StatCard(
            stringResource(R.string.statistics_average_label),
            glucoseUnit.formatValue(statistics.average),
            InsulinkTheme.colors.insulinkBlue,
            Modifier.weight(1f)
        )
        StatCard(
            stringResource(R.string.statistics_min_label),
            glucoseUnit.formatValue(statistics.min),
            InsulinkTheme.colors.glucoseLow,
            Modifier.weight(1f)
        )
        StatCard(
            stringResource(R.string.statistics_max_label),
            glucoseUnit.formatValue(statistics.max),
            InsulinkTheme.colors.glucoseHigh,
            Modifier.weight(1f)
        )
    }

    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
    ) {
        StatCard(
            stringResource(R.string.statistics_std_dev_label),
            glucoseUnit.formatValue(statistics.standardDeviation),
            InsulinkTheme.colors.insulinkPurple,
            Modifier.weight(1f)
        )
        StatCard(
            stringResource(R.string.statistics_reading_count_label),
            statistics.readingCount.toString(),
            InsulinkTheme.colors.glucoseNormal,
            Modifier.weight(1f)
        )
    }

    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))

    Text(
        text = stringResource(R.string.statistics_time_in_range_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
    TimeInRangeBar(statistics)
}

@Composable
private fun TimeInRangeBar(statistics: GlucoseStatistics) {
    val timeInRange = statistics.timeInRange
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TIME_IN_RANGE_BAR_HEIGHT)
            .clip(RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8))
    ) {
        if (timeInRange.belowPercent > 0) {
            Box(
                modifier = Modifier
                    .weight(timeInRange.belowPercent.toFloat().coerceAtLeast(MIN_SEGMENT_WEIGHT))
                    .fillMaxSize()
                    .background(InsulinkTheme.colors.glucoseLow)
            )
        }
        if (timeInRange.inRangePercent > 0) {
            Box(
                modifier = Modifier
                    .weight(timeInRange.inRangePercent.toFloat().coerceAtLeast(MIN_SEGMENT_WEIGHT))
                    .fillMaxSize()
                    .background(InsulinkTheme.colors.glucoseNormal)
            )
        }
        if (timeInRange.abovePercent > 0) {
            Box(
                modifier = Modifier
                    .weight(timeInRange.abovePercent.toFloat().coerceAtLeast(MIN_SEGMENT_WEIGHT))
                    .fillMaxSize()
                    .background(InsulinkTheme.colors.glucoseHigh)
            )
        }
    }
    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeInRangeLegendItem(
            InsulinkTheme.colors.glucoseLow,
            stringResource(R.string.statistics_below_label, formatPercent(timeInRange.belowPercent))
        )
        TimeInRangeLegendItem(
            InsulinkTheme.colors.glucoseNormal,
            stringResource(R.string.statistics_in_range_label, formatPercent(timeInRange.inRangePercent))
        )
        TimeInRangeLegendItem(
            InsulinkTheme.colors.glucoseHigh,
            stringResource(R.string.statistics_above_label, formatPercent(timeInRange.abovePercent))
        )
    }
}

@Composable
private fun TimeInRangeLegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(InsulinkTheme.dimens.commonSpacing8)
                .background(color, RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius4))
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(InsulinkTheme.dimens.nutritionCardHeight),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius8)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(InsulinkTheme.dimens.ingredientsListHeight),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.0f%%", value)

private fun StatisticsRange.labelRes(): Int = when (this) {
    StatisticsRange.TODAY -> R.string.statistics_range_today
    StatisticsRange.LAST_7_DAYS -> R.string.statistics_range_7_days
    StatisticsRange.LAST_15_DAYS -> R.string.statistics_range_15_days
    StatisticsRange.LAST_30_DAYS -> R.string.statistics_range_30_days
    StatisticsRange.LAST_90_DAYS -> R.string.statistics_range_90_days
}

data class StatisticsScreenParams(
    val selectedRange: State<StatisticsRange>,
    val onRangeSelected: (StatisticsRange) -> Unit,
    val statistics: State<GlucoseStatistics?>,
    val glucoseUnit: State<GlucoseUnit>
)

private val RANGE_OPTIONS = listOf(
    StatisticsRange.TODAY,
    StatisticsRange.LAST_7_DAYS,
    StatisticsRange.LAST_15_DAYS,
    StatisticsRange.LAST_30_DAYS,
    StatisticsRange.LAST_90_DAYS
)

private const val MIN_SEGMENT_WEIGHT = 0.001f
private val TIME_IN_RANGE_BAR_HEIGHT: Dp = 24.dp
