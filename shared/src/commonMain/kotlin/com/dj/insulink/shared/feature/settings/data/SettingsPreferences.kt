package com.dj.insulink.shared.feature.settings.data

import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

expect class SettingsPreferences {
    fun getLanguage(): AppLanguage
    fun setLanguage(language: AppLanguage)
    fun getGlucoseUnit(): GlucoseUnit
    fun setGlucoseUnit(unit: GlucoseUnit)
}
