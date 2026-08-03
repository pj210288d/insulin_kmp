package com.dj.insulink.feature.friends.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.friends.domain.model.Friend

@Composable
fun FriendsScreen(
    params: FriendsScreenParams
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
    ) {
        Button(
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            onClick = {
                params.setShowAddNewFriendDialog(true)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = InsulinkTheme.dimens.commonPadding12,
                    vertical = InsulinkTheme.dimens.commonPadding24
                )
                .shadow(
                    elevation = InsulinkTheme.dimens.commonPadding8,
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    clip = false
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = InsulinkTheme.dimens.commonPadding12)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_new_friends),
                    tint = Color.Unspecified,
                    contentDescription = ""
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                Text(
                    text = stringResource(R.string.friends_screen_add_new_friend_button_label),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Text(
            text = stringResource(
                R.string.friends_screen_friends_list_label,
                params.friendsList.size
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = InsulinkTheme.dimens.commonPadding12)
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
        ) {
            params.friendsList.forEach {
                FriendsListItem(friend = it, glucoseUnit = params.glucoseUnit)
            }
        }
    }
    if (params.showAddNewFriendDialog) {
        AddNewFriendDialog(
            onDismissRequest = {
                params.setShowAddNewFriendDialog(false)
            },
            usersFriendCode = params.usersFriendCode,
            enteredCode = params.enteredCode,
            setEnteredCode = params.setEnteredCode,
            onAddFriendClick = params.onAddFriendClick
        )
    }
}

@Composable
private fun AddNewFriendDialog(
    onDismissRequest: () -> Unit,
    usersFriendCode: String,
    enteredCode: String,
    setEnteredCode: (String) -> Unit,
    onAddFriendClick: () -> Unit
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Card(
            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(InsulinkTheme.dimens.commonPadding24),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.friends_screen_friend_code_label),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                Text(
                    text = usersFriendCode,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                OutlinedTextField(
                    value = enteredCode,
                    onValueChange = {
                        setEnteredCode(it)
                    },
                    label = { Text(stringResource(R.string.friends_screen_enter_code_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorTextColor = MaterialTheme.colorScheme.error,

                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorLabelColor = MaterialTheme.colorScheme.error,

                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = OPACITY_VALUE),
                        errorBorderColor = MaterialTheme.colorScheme.error,

                        cursorColor = MaterialTheme.colorScheme.primary,
                        errorCursorColor = MaterialTheme.colorScheme.error,

                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        errorContainerColor = Color.Transparent,

                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = OPACITY_VALUE),
                        errorPlaceholderColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(
                            text = stringResource(R.string.new_reading_cancel),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Button(
                        onClick = {
                            onAddFriendClick()
                        },
                        modifier = Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    InsulinkTheme.colors.insulinkBlue,
                                    InsulinkTheme.colors.insulinkPurple
                                )
                            ),
                            shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.friends_screen_add_friend_label),
                            color = Color.White
                        )
                    }
                }
            }
        }

    }
}

private const val OPACITY_VALUE = 0.6f

data class FriendsScreenParams(
    val friendsList: List<Friend>,
    val usersFriendCode: String,
    val showAddNewFriendDialog: Boolean,
    val enteredCode: String,
    val setShowAddNewFriendDialog: (Boolean) -> Unit,
    val setEnteredCode: (String) -> Unit,
    val onAddFriendClick: () -> Unit,
    val glucoseUnit: GlucoseUnit
)