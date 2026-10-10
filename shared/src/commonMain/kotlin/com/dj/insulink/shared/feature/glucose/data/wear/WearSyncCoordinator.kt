package com.dj.insulink.shared.feature.glucose.data.wear

import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

interface WearSyncCoordinator {
    suspend fun pushLatestReading(reading: GlucoseReading?, unit: GlucoseUnit)
}
