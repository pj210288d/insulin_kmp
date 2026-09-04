package com.dj.insulink.shared.feature.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.statistics.domain.model.GlucoseStatistics
import com.dj.insulink.shared.feature.statistics.domain.model.StatisticsRange
import com.dj.insulink.shared.feature.statistics.ui.viewmodel.StatisticsViewModel
import kotlin.math.round

// Drugi deljeni Compose Multiplatform MVP ekran - vidi StatisticsViewModel za kontekst. Prati
// isti stil kao GlucoseScreen u istom modulu (bez ikonica, bez string resursa/teme, ista brend
// paleta) namerno, radi doslednosti između dva ekrana u istoj demo aplikaciji.
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel) {
    val selectedRange by viewModel.selectedRange.collectAsState()
    val statistics by viewModel.statistics.collectAsState()
    val unit by viewModel.glucoseUnit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 16.dp)
    ) {
        RangeSelector(selectedRange = selectedRange, onSelect = viewModel::setRange)
        Spacer(Modifier.height(16.dp))

        if (statistics == null) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nema očitavanja u izabranom periodu",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            StatisticsSummary(statistics = statistics!!, unit = unit)
            Spacer(Modifier.height(16.dp))
            TimeInRangeBar(statistics = statistics!!)
        }
    }
}

@Composable
private fun RangeSelector(selectedRange: StatisticsRange, onSelect: (StatisticsRange) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatisticsRange.entries.forEach { range ->
            val selected = range == selectedRange
            Box(
                modifier = Modifier
                    .background(
                        color = if (selected) InsulinkBlue else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(range) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = rangeLabel(range),
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun rangeLabel(range: StatisticsRange): String = when (range) {
    StatisticsRange.TODAY -> "Danas"
    StatisticsRange.LAST_7_DAYS -> "7 dana"
    StatisticsRange.LAST_15_DAYS -> "15 dana"
    StatisticsRange.LAST_30_DAYS -> "30 dana"
    StatisticsRange.LAST_90_DAYS -> "90 dana"
}

@Composable
private fun StatisticsSummary(statistics: GlucoseStatistics, unit: GlucoseUnit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Prosek",
                value = "${unit.formatValue(statistics.average)} ${unit.suffix}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Broj očitavanja",
                value = statistics.readingCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Min",
                value = "${unit.formatValue(statistics.min)} ${unit.suffix}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Max",
                value = "${unit.formatValue(statistics.max)} ${unit.suffix}",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        StatCard(
            label = "Standardna devijacija",
            value = oneDecimal(statistics.standardDeviation),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimeInRangeBar(statistics: GlucoseStatistics) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(text = "Vreme u ciljnom opsegu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
        ) {
            TimeInRangeSegment(statistics.timeInRange.belowPercent, GlucoseLow)
            TimeInRangeSegment(statistics.timeInRange.inRangePercent, GlucoseNormal)
            TimeInRangeSegment(statistics.timeInRange.abovePercent, GlucoseHigh)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(color = GlucoseLow, label = "Ispod (${oneDecimal(statistics.timeInRange.belowPercent)}%)")
            LegendItem(color = GlucoseNormal, label = "U opsegu (${oneDecimal(statistics.timeInRange.inRangePercent)}%)")
            LegendItem(color = GlucoseHigh, label = "Iznad (${oneDecimal(statistics.timeInRange.abovePercent)}%)")
        }
    }
}

@Composable
private fun RowScope.TimeInRangeSegment(percent: Double, color: Color) {
    if (percent <= 0.0) return
    Box(
        modifier = Modifier
            .weight(percent.toFloat().coerceAtLeast(0.001f))
            .fillMaxSize()
            .background(color)
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(10.dp).height(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun oneDecimal(value: Double): String {
    val scaled = round(value * 10).toLong()
    val whole = scaled / 10
    val fraction = kotlin.math.abs(scaled % 10)
    return "$whole.$fraction"
}

private val InsulinkBlue = Color(0xFF4A7BF6)
private val GlucoseLow = Color(0xFFEF5350)
private val GlucoseNormal = Color(0xFF66BB6A)
private val GlucoseHigh = Color(0xFFFFEE58)
