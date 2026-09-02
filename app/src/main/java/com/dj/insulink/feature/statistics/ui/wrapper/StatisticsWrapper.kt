package com.dj.insulink.feature.statistics.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.feature.statistics.ui.StatisticsScreen
import com.dj.insulink.feature.statistics.ui.StatisticsScreenParams
import com.dj.insulink.feature.statistics.ui.viewmodel.StatisticsViewModel

@Composable
fun StatisticsWrapper() {
    val viewModel: StatisticsViewModel = hiltViewModel()

    val selectedRange = viewModel.selectedRange.collectAsStateWithLifecycle()
    val statistics = viewModel.statistics.collectAsStateWithLifecycle()
    val glucoseUnit = viewModel.glucoseUnit.collectAsStateWithLifecycle()

    // The unit can change on the Settings screen while this one stays alive in the back stack -
    // refresh it whenever Statistics comes back into view, same pattern used elsewhere for
    // settings that can be changed from another screen.
    LaunchedEffect(Unit) {
        viewModel.refreshGlucoseUnit()
    }

    StatisticsScreen(
        params = StatisticsScreenParams(
            selectedRange = selectedRange,
            onRangeSelected = viewModel::setRange,
            statistics = statistics,
            glucoseUnit = glucoseUnit
        )
    )
}
