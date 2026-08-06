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
import com.dj.insulink.wear.data.WearDataLayerContract

// A plain +/- stepper rather than a rotary Picker: the Picker composable's API has shifted
// across Wear Compose versions, while Text/Button/Chip have been stable for years - a safer
// choice for something this simple. Tap-and-hold isn't wired up; short taps are enough for a
// "quick add" flow.
//
// Steps/displays in whichever unit the phone is currently using (see WearSyncManager.KEY_UNIT),
// so a mmol/L user isn't stuck entering raw mg/dL numbers. The value passed to onConfirm is
// always mg/dL, matching GlucoseReading.value on the phone.
@Composable
fun QuickAddScreen(
    initialValueMgDl: Int,
    unit: String,
    onConfirm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isMmol = unit == WearDataLayerContract.UNIT_MMOL_L
    val min = if (isMmol) MIN_MMOL_TENTHS else MIN_MG_DL
    val max = if (isMmol) MAX_MMOL_TENTHS else MAX_MG_DL
    val step = if (isMmol) STEP_MMOL_TENTHS else STEP_MG_DL

    var displayValue by remember {
        val initial = if (isMmol) mgDlToMmolTenths(initialValueMgDl) else initialValueMgDl
        mutableIntStateOf(initial.coerceIn(min, max))
    }

    Column(
        modifier = modifier.fillMaxSize().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.quick_add_title), style = MaterialTheme.typography.caption1)

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { displayValue = (displayValue - step).coerceIn(min, max) }) {
                Text(text = "-")
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isMmol) formatMmolTenths(displayValue) else displayValue.toString(),
                    style = MaterialTheme.typography.display3
                )
                Text(
                    text = if (isMmol) "mmol/L" else "mg/dL",
                    style = MaterialTheme.typography.caption2
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(onClick = { displayValue = (displayValue + step).coerceIn(min, max) }) {
                Text(text = "+")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Chip(
            onClick = {
                val valueMgDl = if (isMmol) mmolTenthsToMgDl(displayValue) else displayValue
                onConfirm(valueMgDl)
            },
            label = { Text(text = stringResource(R.string.confirm_button)) }
        )
    }
}

// Tenths of mmol/L, kept as an Int to avoid float drift (same reasoning as GlucoseUnit's own
// formatOneDecimal on the phone). MG_DL_PER_MMOL_L must match GlucoseUnit.CONVERSION_FACTOR.
private fun mgDlToMmolTenths(mgDl: Int): Int = Math.round(mgDl / MG_DL_PER_MMOL_L * 10).toInt()

private fun mmolTenthsToMgDl(tenths: Int): Int = Math.round(tenths / 10.0 * MG_DL_PER_MMOL_L).toInt()

private fun formatMmolTenths(tenths: Int): String {
    val whole = tenths / 10
    val fraction = tenths % 10
    return "$whole.$fraction"
}

private const val MG_DL_PER_MMOL_L = 18.0182

private const val MIN_MG_DL = 40
private const val MAX_MG_DL = 400
private const val STEP_MG_DL = 5

private const val MIN_MMOL_TENTHS = 22 // 2.2 mmol/L
private const val MAX_MMOL_TENTHS = 222 // 22.2 mmol/L
private const val STEP_MMOL_TENTHS = 1 // 0.1 mmol/L per tap
