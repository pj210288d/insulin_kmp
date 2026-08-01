package com.dj.insulink.feature.meals.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MealsScreen(
    params: MealsScreenParams
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                DatePickerCard(
                    selectedDate = params.selectedDate.value,
                    onDateSelected = params.setSelectedDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
                )
            }

            item {
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                Card(
                    shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = InsulinkTheme.dimens.commonElevation2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
                ) {
                    DailyNutritionSummary(params.dailyNutrition.value)
                }
            }

            item {
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                Text(
                    text = stringResource(R.string.meals_screen_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(
                        start = InsulinkTheme.dimens.commonPadding16,
                        top = InsulinkTheme.dimens.commonPadding8,
                        bottom = InsulinkTheme.dimens.commonPadding8
                    )
                )
            }

            items(items = params.mealsForSelectedDate.value, key = { it.id }) { meal ->
                MealItem(
                    meal = meal,
                    onSwipeFromStartToEnd = { params.deleteMeal(meal) }
                )
                Spacer(Modifier.size(InsulinkTheme.dimens.commonPadding8))
            }

            item {
                Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing64))
            }
        }

        FloatingActionButton(
            onClick = params.onAddMealClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(InsulinkTheme.dimens.commonPadding16),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = ""
            )
        }
    }
}

data class MealsScreenParams(
    val mealsForSelectedDate: State<List<Meal>>,
    val dailyNutrition: State<DailyNutrition>,
    val selectedDate: State<Long>,
    val setSelectedDate: (Long) -> Unit,
    val deleteMeal: (Meal) -> Unit,
    val onAddMealClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerCard(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val showDatePicker = remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
    )
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
    val isToday = remember(selectedDate) {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { timeInMillis = selectedDate }
        today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
    }

    Card(
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = InsulinkTheme.dimens.commonElevation2),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker.value = true }
                .padding(vertical = InsulinkTheme.dimens.commonPadding8)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(InsulinkTheme.dimens.commonIconSize16)
                )
                Spacer(modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing4))
                Text(
                    text = if (isToday) stringResource(R.string.meals_screen_today_label) else dateFormatter.format(Date(selectedDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = dayFormatter.format(Date(selectedDate)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            onDateSelected(dateMillis)
                        }
                        showDatePicker.value = false
                    }
                ) {
                    Text(stringResource(R.string.new_reading_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker.value = false }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
