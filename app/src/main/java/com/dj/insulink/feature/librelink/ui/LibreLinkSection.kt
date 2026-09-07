package com.dj.insulink.feature.librelink.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.feature.librelink.ui.viewmodel.LibreLinkConnectState
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkConnection

@Composable
fun LibreLinkSection(params: LibreLinkSectionParams) {
    val connectState = params.connectState
    when {
        params.connectedEmail != null -> LibreLinkConnectedContent(params)
        connectState is LibreLinkConnectState.ChoosingConnection ->
            LibreLinkChooseConnectionContent(connectState.connections, params.onSelectConnection, params.onCancelSelectingConnection)
        else -> LibreLinkConnectForm(params)
    }
}

@Composable
private fun LibreLinkChooseConnectionContent(
    connections: List<LibreLinkConnection>,
    onSelectConnection: (LibreLinkConnection) -> Unit,
    onCancel: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.librelink_choose_connection_title),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
        connections.forEach { connection ->
            Card(
                onClick = { onSelectConnection(connection) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = InsulinkTheme.dimens.commonSpacing4),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectConnection(connection) }
                        .padding(InsulinkTheme.dimens.commonPadding12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = false, onClick = { onSelectConnection(connection) })
                    Text(
                        text = connection.displayName,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.librelink_choose_connection_cancel))
        }
    }
}

@Composable
private fun LibreLinkConnectedContent(params: LibreLinkSectionParams) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.librelink_connected_as, params.connectedEmail.orEmpty()),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
        Text(
            text = params.lastSyncedLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        params.lastSyncError?.let { error ->
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
            Text(
                text = stringResource(R.string.librelink_sync_error, error),
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Button(
            onClick = params.onSyncNow,
            enabled = !params.isSyncing,
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (params.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing24),
                    color = Color.White
                )
            } else {
                Text(stringResource(R.string.librelink_sync_now_button))
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
        OutlinedButton(
            onClick = params.onDisconnect,
            enabled = !params.isSyncing,
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.librelink_disconnect_button))
        }
    }
}

@Composable
private fun LibreLinkConnectForm(params: LibreLinkSectionParams) {
    val isConnecting = params.connectState is LibreLinkConnectState.Connecting

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = params.email,
            onValueChange = params.onEmailChanged,
            label = { Text(stringResource(R.string.librelink_email_label)) },
            singleLine = true,
            enabled = !isConnecting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = InsulinkTheme.dimens.commonSpacing12),
            colors = libreLinkTextFieldColors()
        )
        OutlinedTextField(
            value = params.password,
            onValueChange = params.onPasswordChanged,
            label = { Text(stringResource(R.string.librelink_password_label)) },
            singleLine = true,
            enabled = !isConnecting,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = InsulinkTheme.dimens.commonSpacing12),
            colors = libreLinkTextFieldColors()
        )
        (params.connectState as? LibreLinkConnectState.Error)?.let { error ->
            Text(
                text = stringResource(R.string.librelink_connect_error, error.message),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = InsulinkTheme.dimens.commonSpacing12)
            )
        }
        Button(
            onClick = params.onConnect,
            enabled = !isConnecting && params.email.isNotBlank() && params.password.isNotBlank(),
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing24),
                    color = Color.White
                )
            } else {
                Text(stringResource(R.string.librelink_connect_button))
            }
        }
    }
}

@Composable
private fun libreLinkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.primary
)

data class LibreLinkSectionParams(
    val connectedEmail: String?,
    val lastSyncedLabel: String,
    val lastSyncError: String?,
    val connectState: LibreLinkConnectState,
    val isSyncing: Boolean,
    val email: String,
    val password: String,
    val onEmailChanged: (String) -> Unit,
    val onPasswordChanged: (String) -> Unit,
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
    val onSyncNow: () -> Unit,
    val onSelectConnection: (LibreLinkConnection) -> Unit,
    val onCancelSelectingConnection: () -> Unit
)
