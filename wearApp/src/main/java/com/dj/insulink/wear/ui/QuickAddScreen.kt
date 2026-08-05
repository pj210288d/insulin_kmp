package com.dj.insulink.wear.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.dj.insulink.wear.R

// A plain +/- stepper rather than a rotary Picker: the Picker composable's API has shifted
// across Wear Compose versions, while Text/Button/Chip have been stable for years - a safer
// choice for something this simple. Tap-and-hold isn't wired up; short taps are enough for a
// "quick add" flow.
@Composable
fun QuickAddScreen(
    initialValueMgDl: Int,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableIntStateOf(initialValueMgDl.coerceIn(MIN_VALUE, MAX_VALUE)) }

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.quick_add_title), style = MaterialTheme.typography.caption1)

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { value = (value - STEP).coerceIn(MIN_VALUE, MAX_VALUE) }) {
                Text(text = "-")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(text = value.toString(), style = MaterialTheme.typography.display3)

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = { value = (value + STEP).coerceIn(MIN_VALUE, MAX_VALUE) }) {
                Text(text = "+")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Chip(
            onClick = { onConfirm(value) },
            label = { Text(text = stringResource(R.string.confirm_button)) }
        )
    }
}

private const val MIN_VALUE = 40
private const val MAX_VALUE = 400
private const val STEP = 5
