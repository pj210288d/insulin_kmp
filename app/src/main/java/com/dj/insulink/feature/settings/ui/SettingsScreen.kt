package com.dj.insulink.feature.settings.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.settings.domain.model.AppLanguage
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@Composable
fun SettingsScreen(
    params: SettingsScreenParams
) {
    val languageItems = remember {
        AppLanguage.entries.map { DropdownItemWithIcon(it.displayName, it.flagIcon) }
    }
    val glucoseUnitItems = remember {
        GlucoseUnit.entries.map { DropdownItemWithIcon(it.label, it.flagIcon) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = InsulinkTheme.dimens.commonPadding16)
    ) {
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))

        SettingsSection(
            title = stringResource(R.string.settings_language_title),
            description = stringResource(R.string.settings_language_description)
        ) {
            SettingsDropdownMenu(
                items = languageItems,
                selectedItem = DropdownItemWithIcon(
                    params.selectedLanguage.displayName,
                    params.selectedLanguage.flagIcon
                ),
                onItemSelected = { selected ->
                    val language = AppLanguage.entries.find { it.displayName == selected.label }
                        ?: AppLanguage.ENGLISH
                    params.onLanguageSelected(language)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing24))

        SettingsSection(
            title = stringResource(R.string.settings_glucose_unit_title),
            description = stringResource(R.string.settings_glucose_unit_description)
        ) {
            SettingsDropdownMenu(
                items = glucoseUnitItems,
                selectedItem = DropdownItemWithIcon(
                    params.selectedGlucoseUnit.label,
                    params.selectedGlucoseUnit.flagIcon
                ),
                onItemSelected = { selected ->
                    val unit = GlucoseUnit.entries.find { it.label == selected.label }
                        ?: GlucoseUnit.MG_DL
                    params.onGlucoseUnitSelected(unit)
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12))
            .border(
                BorderStroke(
                    InsulinkTheme.dimens.commonButtonBorder1,
                    MaterialTheme.colorScheme.outline
                ),
                RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
            )
            .background(MaterialTheme.colorScheme.surface)
            .padding(InsulinkTheme.dimens.commonPadding16)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing4))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
        content()
    }
}

data class SettingsScreenParams(
    val selectedLanguage: AppLanguage,
    val selectedGlucoseUnit: GlucoseUnit,
    val onLanguageSelected: (AppLanguage) -> Unit,
    val onGlucoseUnitSelected: (GlucoseUnit) -> Unit,
)
