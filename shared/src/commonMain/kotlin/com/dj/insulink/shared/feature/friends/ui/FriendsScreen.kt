package com.dj.insulink.shared.feature.friends.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dj.insulink.shared.core.localization.LocalizationSession
import com.dj.insulink.shared.core.localization.tr
import com.dj.insulink.shared.core.time.dateTimeLabel
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.friends.ui.viewmodel.FriendsViewModel
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

// Deveti deljeni Compose Multiplatform MVP ekran - vidi FriendsViewModel u istom paketu za
// obim/odluke. Isti obim kao Android-ov ekran (traži se prijatelj po friendCode, lista +
// poslednje očitavanje), samo bez ikonica/InsulinkTheme (app-module-specifično) - isti princip
// kao ostatak deljenog UI-ja.
@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val friends by viewModel.allFriendsForUser.collectAsState()
    val usersFriendCode by viewModel.usersFriendCode.collectAsState()
    val showDialog by viewModel.showAddNewFriendDialog.collectAsState()
    val enteredCode by viewModel.enteredCode.collectAsState()
    val unit by viewModel.glucoseUnit.collectAsState()
    val language by LocalizationSession.currentLanguage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Button(
            onClick = { viewModel.setShowAddNewFriendDialog(true) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr(language, "Dodaj prijatelja", "Add friend"))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${tr(language, "Prijatelji", "Friends")} (${friends.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        if (friends.isEmpty()) {
            Text(
                text = tr(language, "Još nemaš prijatelja - unesi njihov kod da ih dodaš.", "You don't have any friends yet - enter their code to add them."),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            friends.forEach { friend ->
                FriendRow(friend, unit, language)
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDialog) {
        AddFriendDialog(
            usersFriendCode = usersFriendCode,
            enteredCode = enteredCode,
            language = language,
            onEnteredCodeChange = viewModel::setEnteredCode,
            onDismiss = { viewModel.setShowAddNewFriendDialog(false) },
            onAdd = viewModel::addFriend
        )
    }
}

@Composable
private fun FriendRow(friend: Friend, unit: GlucoseUnit, language: AppLanguage) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = friend.friendName, fontWeight = FontWeight.Bold)
                Text(
                    text = friend.friendsLastGlucoseReadingTime?.let { "${tr(language, "Poslednje", "Last")}: ${dateTimeLabel(it)}" }
                        ?: tr(language, "Nema očitavanja", "No readings"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = friend.friendLastGlucoseReadingValue?.let { "${unit.formatValue(it)} ${unit.suffix}" }
                    ?: "--",
                fontWeight = FontWeight.Bold
            )
            // Poznat bug (namerno OSTAVLJEN na zahtev korisnika 2026-09-07 - beta testiranje je u
            // toku): nema dugmeta za uklanjanje prijatelja. viewModel.removeFriend(friend) je
            // spreman (zakomentarisan u FriendsViewModel.kt) - samo treba dodati onDelete
            // parametar ovoj funkciji i otkomentarisati dole kad beta period prođe.
            // IconButton(onClick = { onDelete(friend) }) {
            //     Text(text = "✕", color = MaterialTheme.colorScheme.error)
            // }
        }
    }
}

@Composable
private fun AddFriendDialog(
    usersFriendCode: String,
    enteredCode: String,
    language: AppLanguage,
    onEnteredCodeChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAdd: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr(language, "Dodaj prijatelja", "Add friend")) },
        text = {
            Column {
                Text(tr(language, "Tvoj kod za deljenje:", "Your code to share:"))
                Text(text = usersFriendCode, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = onEnteredCodeChange,
                    label = { Text(tr(language, "Unesi kod prijatelja", "Enter friend's code")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd, enabled = enteredCode.isNotBlank()) {
                Text(tr(language, "Dodaj", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr(language, "Otkaži", "Cancel")) }
        }
    )
}
