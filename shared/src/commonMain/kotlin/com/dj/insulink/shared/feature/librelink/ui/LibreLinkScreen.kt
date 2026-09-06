package com.dj.insulink.shared.feature.librelink.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkConnectState
import com.dj.insulink.shared.feature.librelink.ui.viewmodel.LibreLinkViewModel

// Sedmi deljeni Compose Multiplatform MVP ekran - vidi LibreLinkViewModel za obim/odluke.
@Composable
fun LibreLinkScreen(viewModel: LibreLinkViewModel) {
    val state by viewModel.connectState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncMessage by viewModel.lastSyncMessage.collectAsState()
    val language by LocalizationSession.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        when (val current = state) {
            is LibreLinkConnectState.Disconnected -> {
                LoginForm(
                    email = email,
                    onEmailChange = viewModel::setEmail,
                    password = password,
                    onPasswordChange = viewModel::setPassword,
                    onLogin = viewModel::login,
                    language = language
                )
            }
            is LibreLinkConnectState.Connecting -> {
                Text(text = tr(language, "Povezivanje...", "Connecting..."), color = MaterialTheme.colorScheme.onBackground)
            }
            is LibreLinkConnectState.ChoosingConnection -> {
                Text(
                    text = tr(language, "Izaberi konekciju", "Choose connection"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                current.connections.forEach { connection ->
                    ConnectionRow(connection = connection, onClick = { viewModel.selectConnection(connection) })
                    Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = viewModel::cancelChoosingConnection) {
                    Text(tr(language, "Otkaži", "Cancel"))
                }
            }
            is LibreLinkConnectState.Connected -> {
                Text(
                    text = tr(language, "Povezano", "Connected"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InsulinkGreen
                )
                Spacer(Modifier.height(4.dp))
                Text(text = current.session.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Row {
                    TextButton(onClick = viewModel::syncNow, enabled = !isSyncing) {
                        Text(if (isSyncing) tr(language, "Sinhronizacija...", "Syncing...") else tr(language, "Sinhronizuj sada", "Sync now"))
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = viewModel::disconnect) {
                        Text(tr(language, "Prekini vezu", "Disconnect"))
                    }
                }
                lastSyncMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            is LibreLinkConnectState.Error -> {
                Text(text = current.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                LoginForm(
                    email = email,
                    onEmailChange = viewModel::setEmail,
                    password = password,
                    onPasswordChange = viewModel::setPassword,
                    onLogin = viewModel::login,
                    language = language
                )
            }
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
    language: AppLanguage
) {
    Text(
        text = tr(language, "Poveži LibreLinkUp nalog", "Connect your LibreLinkUp account"),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(tr(language, "Lozinka", "Password")) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onLogin, enabled = email.isNotBlank() && password.isNotBlank()) {
        Text(tr(language, "Poveži se", "Connect"))
    }
}

@Composable
private fun ConnectionRow(connection: LibreLinkConnection, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = connection.displayName)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(InsulinkBlue, CircleShape)
            )
        }
    }
}

private val InsulinkBlue = Color(0xFF4A7BF6)
private val InsulinkGreen = Color(0xFF66BB6A)
