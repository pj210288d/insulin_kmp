package com.dj.insulink.feature.meals.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition

@Composable
fun DailyNutritionSummary(nutrition: DailyNutrition) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(InsulinkTheme.dimens.commonPadding12)
    ) {
        Text(
            text = stringResource(R.string.meals_screen_daily_nutrition_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = InsulinkTheme.dimens.commonSpacing12)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
        ) {
            NutritionItem(
                label = stringResource(R.string.meals_screen_calories_label),
                value = "${nutrition.calories}",
                unit = "",
                color = InsulinkTheme.colors.insulinkBlue.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            )
            NutritionItem(
                label = stringResource(R.string.meals_screen_carbs_label),
                value = stringResource(R.string.meals_screen_grams_value, nutrition.carbs),
                unit = "",
                color = InsulinkTheme.colors.glucoseNormal.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
        ) {
            NutritionItem(
                label = stringResource(R.string.meals_screen_protein_label),
                value = stringResource(R.string.meals_screen_grams_value, nutrition.protein),
                unit = "",
                color = InsulinkTheme.colors.insulinkPurple.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            )
            NutritionItem(
                label = stringResource(R.string.meals_screen_fat_label),
                value = stringResource(R.string.meals_screen_grams_value, nutrition.fat),
                unit = "",
                color = InsulinkTheme.colors.lastDropLabel.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
        ) {
            NutritionItem(
                label = stringResource(R.string.meals_screen_sugar_label),
                value = stringResource(R.string.meals_screen_grams_value, nutrition.sugar),
                unit = "",
                color = InsulinkTheme.colors.glucoseLow.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f)
            )
            NutritionItem(
                label = stringResource(R.string.meals_screen_salt_label),
                value = stringResource(R.string.meals_screen_salt_value, nutrition.salt),
                unit = "",
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
