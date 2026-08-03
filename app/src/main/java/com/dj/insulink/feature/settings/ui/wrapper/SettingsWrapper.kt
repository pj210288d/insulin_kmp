package com.dj.insulink.feature.settings.ui.wrapper

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.feature.settings.ui.SettingsScreen
import com.dj.insulink.feature.settings.ui.SettingsScreenParams
import com.dj.insulink.feature.settings.ui.locale
import com.dj.insulink.feature.settings.ui.viewmodel.SettingsViewModel
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import java.util.Locale

@Composable
fun SettingsWrapper() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val context = LocalContext.current

    val selectedLanguage = viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedGlucoseUnit = viewModel.selectedGlucoseUnit.collectAsStateWithLifecycle()

    SettingsScreen(
        params = SettingsScreenParams(
            selectedLanguage = selectedLanguage.value,
            selectedGlucoseUnit = selectedGlucoseUnit.value,
            onLanguageSelected = { language ->
                viewModel.setLanguage(language)
                applyLocaleAndRecreate(context as Activity, language)
            },
            onGlucoseUnitSelected = { unit ->
                viewModel.setGlucoseUnit(unit)
            }
        )
    )
}

private fun applyLocaleAndRecreate(activity: Activity, language: AppLanguage) {
    val locale = language.locale
    Locale.setDefault(locale)
    val config = activity.resources.configuration
    config.setLocale(locale)
    activity.recreate()
}
