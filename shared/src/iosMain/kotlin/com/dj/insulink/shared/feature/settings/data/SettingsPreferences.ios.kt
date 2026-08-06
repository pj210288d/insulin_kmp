package com.dj.insulink.shared.feature.settings.data

import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import platform.Foundation.NSUserDefaults

actual class SettingsPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getLanguage(): AppLanguage =
        AppLanguage.fromKey(defaults.stringForKey(KEY_LANGUAGE) ?: AppLanguage.ENGLISH.key)

    actual fun setLanguage(language: AppLanguage) {
        defaults.setObject(language.key, KEY_LANGUAGE)
    }

    actual fun getGlucoseUnit(): GlucoseUnit =
        GlucoseUnit.fromKey(defaults.stringForKey(KEY_GLUCOSE_UNIT) ?: GlucoseUnit.MG_DL.key)

    actual fun setGlucoseUnit(unit: GlucoseUnit) {
        defaults.setObject(unit.key, KEY_GLUCOSE_UNIT)
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_GLUCOSE_UNIT = "glucose_unit"
    }
}
