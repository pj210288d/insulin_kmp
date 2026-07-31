package com.dj.insulink.feature.meals.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MealItem(meal: Meal, onSwipeFromStartToEnd: () -> Unit) {
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onSwipeFromStartToEnd()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * POSITIONAL_MODIFIER }
    )

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        enableDismissFromEndToStart = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = InsulinkTheme.dimens.commonPadding12),
        backgroundContent = {}
    ) {
        MealItemContent(meal = meal)
    }
}

@Composable
private fun MealItemContent(meal: Meal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        elevation = CardDefaults.cardElevation(defaultElevation = InsulinkTheme.dimens.commonElevation2),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InsulinkTheme.dimens.commonPadding16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(InsulinkTheme.dimens.commonSpacing4))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(meal.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (meal.calories != null || meal.protein != null || meal.carbs != null) {
                Column(horizontalAlignment = Alignment.End) {
                    val calories = meal.calories
                    if (calories != null) {
                        Text(
                            text = stringResource(R.string.meals_screen_cal_value, calories),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (meal.protein != null) {
                        Text(
                            text = stringResource(R.string.meals_screen_protein_value, String.format("%.1f", meal.protein)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (meal.carbs != null) {
                        Text(
                            text = stringResource(R.string.meals_screen_carbs_value, String.format("%.1f", meal.carbs)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private const val POSITIONAL_MODIFIER = 0.25f
