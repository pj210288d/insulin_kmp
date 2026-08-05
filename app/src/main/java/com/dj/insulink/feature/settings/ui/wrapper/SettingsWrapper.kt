package com.dj.insulink.feature.settings.ui.wrapper

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.R
import com.dj.insulink.feature.librelink.ui.LibreLinkSectionParams
import com.dj.insulink.feature.librelink.ui.viewmodel.LibreLinkViewModel
import com.dj.insulink.feature.settings.ui.SettingsScreen
import com.dj.insulink.feature.settings.ui.SettingsScreenParams
import com.dj.insulink.feature.settings.ui.locale
import com.dj.insulink.feature.settings.ui.viewmodel.SettingsViewModel
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsWrapper() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val libreLinkViewModel: LibreLinkViewModel = hiltViewModel()
    val context = LocalContext.current

    val selectedLanguage = viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val selectedGlucoseUnit = viewModel.selectedGlucoseUnit.collectAsStateWithLifecycle()

    val libreLinkSession = libreLinkViewModel.session.collectAsStateWithLifecycle()
    val libreLinkLastSynced = libreLinkViewModel.lastSyncedTimestamp.collectAsStateWithLifecycle()
    val libreLinkLastSyncError = libreLinkViewModel.lastSyncError.collectAsStateWithLifecycle()
    val libreLinkConnectState = libreLinkViewModel.connectState.collectAsStateWithLifecycle()
    val libreLinkEmail = libreLinkViewModel.email.collectAsStateWithLifecycle()
    val libreLinkPassword = libreLinkViewModel.password.collectAsStateWithLifecycle()
    val libreLinkCurrentUserId = libreLinkViewModel.currentUserId.collectAsStateWithLifecycle()

    // Re-reads LibreLinkUp status whenever the signed-in Insulink user changes (not just on
    // first composition) - otherwise a second Google account signed into the same app
    // install would keep showing the first account's LibreLinkUp connection.
    LaunchedEffect(libreLinkCurrentUserId.value) {
        libreLinkViewModel.refreshStatus()
    }

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
            },
            libreLink = LibreLinkSectionParams(
                connectedEmail = libreLinkSession.value?.email,
                lastSyncedLabel = formatLastSynced(context, libreLinkLastSynced.value),
                lastSyncError = libreLinkLastSyncError.value,
                connectState = libreLinkConnectState.value,
                email = libreLinkEmail.value,
                password = libreLinkPassword.value,
                onEmailChanged = libreLinkViewModel::setEmail,
                onPasswordChanged = libreLinkViewModel::setPassword,
                onConnect = libreLinkViewModel::connect,
                onDisconnect = libreLinkViewModel::disconnect
            )
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

private fun formatLastSynced(context: android.content.Context, timestamp: Long?): String {
    if (timestamp == null) return context.getString(R.string.librelink_never_synced)
    val formatter = SimpleDateFormat("d/M/yy H:mm", Locale.getDefault())
    return context.getString(R.string.librelink_last_synced, formatter.format(Date(timestamp)))
}
