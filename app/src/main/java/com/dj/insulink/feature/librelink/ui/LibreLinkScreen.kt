package com.dj.insulink.feature.librelink.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme

// Izdvojeno iz Settings-a u sopstveni sidebar ekran (isto kao na iOS-u - shared App.kt već ima
// LibreLinkUp kao poseban DRAWER_DESTINATIONS unos, ne deo Settings-a) - vidi SideDrawer.kt.
@Composable
fun LibreLinkScreen(params: LibreLinkScreenParams) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(InsulinkTheme.dimens.commonPadding16)
    ) {
        OutlinedButton(
            onClick = params.onHelpClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "")
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
            Text(stringResource(R.string.librelink_help_button))
        }

        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing16))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12))
                .border(
                    BorderStroke(InsulinkTheme.dimens.commonButtonBorder1, MaterialTheme.colorScheme.outline),
                    RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(InsulinkTheme.dimens.commonPadding16)
        ) {
            Text(
                text = stringResource(R.string.settings_librelink_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
            Text(
                text = stringResource(R.string.settings_librelink_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
            LibreLinkSection(params = params.libreLink)
        }
    }
}

data class LibreLinkScreenParams(
    val libreLink: LibreLinkSectionParams,
    val onHelpClick: () -> Unit
)
