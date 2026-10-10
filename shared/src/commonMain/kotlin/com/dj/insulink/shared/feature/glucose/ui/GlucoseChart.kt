package com.dj.insulink.shared.feature.glucose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@Composable
expect fun GlucoseChart(readings: List<GlucoseReading>, unit: GlucoseUnit, modifier: Modifier = Modifier)
