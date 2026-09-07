package com.dj.insulink.feature.librelink.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.R
import com.dj.insulink.feature.librelink.ui.LibreLinkScreen
import com.dj.insulink.feature.librelink.ui.LibreLinkScreenParams
import com.dj.insulink.feature.librelink.ui.LibreLinkSectionParams
import com.dj.insulink.feature.librelink.ui.viewmodel.LibreLinkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Isti sadržaj/wiring koji je ranije živeo u SettingsWrapper.kt - izdvojen zajedno sa
// LibreLinkScreen.kt kad je LibreLinkUp dobio sopstveni sidebar unos (vidi SideDrawer.kt).
@Composable
fun LibreLinkWrapper(navigateToHelp: () -> Unit) {
    val libreLinkViewModel: LibreLinkViewModel = hiltViewModel()
    val context = LocalContext.current

    val libreLinkSession = libreLinkViewModel.session.collectAsStateWithLifecycle()
    val libreLinkLastSynced = libreLinkViewModel.lastSyncedTimestamp.collectAsStateWithLifecycle()
    val libreLinkLastSyncError = libreLinkViewModel.lastSyncError.collectAsStateWithLifecycle()
    val libreLinkConnectState = libreLinkViewModel.connectState.collectAsStateWithLifecycle()
    val libreLinkIsSyncing = libreLinkViewModel.isSyncing.collectAsStateWithLifecycle()
    val libreLinkEmail = libreLinkViewModel.email.collectAsStateWithLifecycle()
    val libreLinkPassword = libreLinkViewModel.password.collectAsStateWithLifecycle()
    val libreLinkCurrentUserId = libreLinkViewModel.currentUserId.collectAsStateWithLifecycle()

    // Re-reads LibreLinkUp status whenever the signed-in Insulink user changes (not just on
    // first composition) - otherwise a second Google account signed into the same app
    // install would keep showing the first account's LibreLinkUp connection.
    LaunchedEffect(libreLinkCurrentUserId.value) {
        libreLinkViewModel.refreshStatus()
    }

    LibreLinkScreen(
        params = LibreLinkScreenParams(
            libreLink = LibreLinkSectionParams(
                connectedEmail = libreLinkSession.value?.email,
                lastSyncedLabel = formatLastSynced(context, libreLinkLastSynced.value),
                lastSyncError = libreLinkLastSyncError.value,
                connectState = libreLinkConnectState.value,
                isSyncing = libreLinkIsSyncing.value,
                email = libreLinkEmail.value,
                password = libreLinkPassword.value,
                onEmailChanged = libreLinkViewModel::setEmail,
                onPasswordChanged = libreLinkViewModel::setPassword,
                onConnect = libreLinkViewModel::connect,
                onDisconnect = libreLinkViewModel::disconnect,
                onSyncNow = libreLinkViewModel::syncNow,
                onSelectConnection = libreLinkViewModel::selectConnection,
                onCancelSelectingConnection = libreLinkViewModel::cancelSelectingConnection
            ),
            onHelpClick = navigateToHelp
        )
    )
}

private fun formatLastSynced(context: android.content.Context, timestamp: Long?): String {
    if (timestamp == null) return context.getString(R.string.librelink_never_synced)
    val formatter = SimpleDateFormat("d/M/yy H:mm", Locale.getDefault())
    return context.getString(R.string.librelink_last_synced, formatter.format(Date(timestamp)))
}
