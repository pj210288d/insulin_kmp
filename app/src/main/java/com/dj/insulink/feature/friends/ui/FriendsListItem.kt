package com.dj.insulink.feature.friends.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.feature.glucose.ui.GlucoseLevelTag
import com.dj.insulink.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FriendsListItem(
    friend: Friend,
    glucoseUnit: GlucoseUnit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .border(
                width = InsulinkTheme.dimens.commonButtonBorder1,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
            )
            .padding(vertical = InsulinkTheme.dimens.commonPadding12)
    ) {
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
        Column {
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            Row {
                Text(
                    text = friend.friendName,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            }
            val readingTime = friend.friendsLastGlucoseReadingTime
            if (readingTime != null) {
                Text(
                    text = stringResource(
                        R.string.friends_screen_reading_label,
                        SimpleDateFormat(
                            "d/M/yy H:mm",
                            Locale.getDefault()
                        ).format(Date(readingTime))
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            } else {
                Text(
                    text = stringResource(R.string.friends_screen_empty_reading_label),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            }
        }
        Spacer(Modifier.weight(WEIGHT_VALUE))
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End
        ) {
            val glucoseValue = friend.friendLastGlucoseReadingValue
            if (glucoseValue != null) {
                Text(
                    text = stringResource(
                        R.string.glucose_screen_value_display_label,
                        glucoseUnit.formatValue(glucoseValue),
                        glucoseUnit.suffix
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                GlucoseLevelTag(glucoseLevel = glucoseValue)
            } else {
                Text(
                    text = stringResource(R.string.friends_screen_dash_label),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            }
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))
    }
    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
}

private const val WEIGHT_VALUE = 1f