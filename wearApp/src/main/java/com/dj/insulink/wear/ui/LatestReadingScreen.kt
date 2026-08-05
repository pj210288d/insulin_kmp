package com.dj.insulink.wear.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.dj.insulink.wear.R
import com.dj.insulink.wear.data.LatestReading
import com.dj.insulink.wear.data.WearDataLayerContract

@Composable
fun LatestReadingScreen(
    latestReading: LatestReading?,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (latestReading == null) {
                Text(text = stringResource(R.string.no_reading_yet), style = MaterialTheme.typography.body2)
            } else {
                Text(
                    text = latestReading.formattedValue,
                    style = MaterialTheme.typography.display2,
                    color = rangeStatusColor(latestReading.rangeStatus)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = minutesAgoLabel(latestReading.timestampMillis), style = MaterialTheme.typography.caption2)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Chip(
                onClick = onAddClick,
                label = { Text(text = stringResource(R.string.add_reading_button)) }
            )
        }
    }
}

@Composable
private fun minutesAgoLabel(timestampMillis: Long): String {
    val minutes = (System.currentTimeMillis() - timestampMillis) / 60_000L
    return if (minutes < 1) {
        stringResource(R.string.just_now)
    } else {
        stringResource(R.string.minutes_ago_format, minutes)
    }
}

@Composable
private fun rangeStatusColor(rangeStatus: String) = when (rangeStatus) {
    WearDataLayerContract.RANGE_LOW -> WearColors.Low
    WearDataLayerContract.RANGE_HIGH -> WearColors.High
    else -> WearColors.Normal
}
