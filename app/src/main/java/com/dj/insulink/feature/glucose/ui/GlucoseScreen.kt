package com.dj.insulink.feature.glucose.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.feature.glucose.ui.viewmodel.GlucoseReadingTimespan
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun GlucoseScreen(
    params: GlucoseScreenParams
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(InsulinkTheme.dimens.commonPadding12)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                InsulinkTheme.colors.insulinkBlue,
                                InsulinkTheme.colors.insulinkPurple
                            )
                        ),
                        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = InsulinkTheme.dimens.commonPadding16)
                        .padding(start = InsulinkTheme.dimens.commonPadding24)
                ) {
                    Text(
                        text = stringResource(R.string.glucose_screen_latest_reading_label),
                        color = Color.White
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Text(
                        text = if (params.latestGlucoseReading.value != null) {
                            stringResource(
                                R.string.glucose_screen_value_display_label,
                                params.glucoseUnit.value.formatValue(params.latestGlucoseReading.value!!.value),
                                params.glucoseUnit.value.suffix
                            )
                        } else {
                            stringResource(R.string.glucose_screen_empty_display_label, params.glucoseUnit.value.suffix)
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    Text(
                        text = if (params.latestGlucoseReading.value != null) {
                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(params.latestGlucoseReading.value!!.timestamp))
                        } else {
                            ""
                        },
                        color = Color.White
                    )
                    Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing8))
                    GlucoseLevelIndicator(glucoseLevel = params.latestGlucoseReading.value?.value)
                }
            }

            val timespanLabels = GlucoseReadingTimespan.entries.map { stringResource(it.displayNameRes) }
            GlucoseDropdownMenu(
                items = timespanLabels,
                selectedItem = stringResource(params.selectedTimespan.value.displayNameRes),
                onItemSelected = { selected ->
                    val index = timespanLabels.indexOf(selected)
                    if (index >= 0) {
                        params.setSelectedTimespan(GlucoseReadingTimespan.entries[index])
                    }
                },
                modifier = Modifier.padding(horizontal = InsulinkTheme.dimens.commonPadding12)
            )

            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))

            if (params.allGlucoseReadings.value.isNotEmpty()) {
                DynamicLineChart(
                    xValues = params.allGlucoseReadings.value.map { it.timestamp }.reversed(),
                    yValues = params.allGlucoseReadings.value.map { it.value }.reversed(),
                    modifier = Modifier
                        .padding(horizontal = InsulinkTheme.dimens.commonPadding12)
                        .height(289.dp),
                    timespan = params.selectedTimespan.value,
                    glucoseUnit = params.glucoseUnit.value
                )
            }

            Spacer(Modifier.size(InsulinkTheme.dimens.commonSpacing12))

            Column {
                if (params.allGlucoseReadings.value.isNotEmpty()) {
                    val insulinTypesById = remember(params.allInsulinTypesForUser.value) {
                        params.allInsulinTypesForUser.value.associateBy { it.id }
                    }
                    val mealsById = remember(params.allMealsForUser.value) {
                        params.allMealsForUser.value.associateBy { it.id }
                    }
                    LazyColumn(
                        modifier = Modifier.height(ALLOWED_READINGS_COLUMN_HEIGHT)
                    ) {
                        items(items = params.allGlucoseReadings.value, key = { item -> item.id }) {
                            GlucoseReadingItem(
                                glucoseReading = it,
                                glucoseUnit = params.glucoseUnit.value,
                                insulinTypesById = insulinTypesById,
                                mealsById = mealsById,
                                onSwipeFromStartToEnd = {
                                    params.deleteGlucoseReading(it)
                                },
                                onClick = {
                                    params.startEditingGlucoseReading(it)
                                }
                            )
                            Spacer(Modifier.size(InsulinkTheme.dimens.commonPadding8))
                        }
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.glucose_screen_no_readings_title),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                params.startAddGlucoseReading()
            },
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

    if (params.showAddGlucoseReadingDialog.value) {
        AddGlucoseReadingDialog(
            newGlucoseReadingTimestamp = params.newGlucoseReadingTimestamp,
            setNewGlucoseReadingTimestamp = params.setNewGlucoseReadingTimestamp,
            newGlucoseReadingValue = params.newGlucoseReadingValue,
            setNewGlucoseReadingValue = params.setNewGlucoseReadingValue,
            newGlucoseReadingComment = params.newGlucoseReadingComment,
            setNewGlucoseReadingComment = params.setNewGlucoseReadingComment,
            insulinTypes = params.allInsulinTypesForUser.value,
            selectedInsulinTypeId = params.newGlucoseReadingInsulinTypeId.value,
            setSelectedInsulinTypeId = params.setNewGlucoseReadingInsulinTypeId,
            insulinUnits = params.newGlucoseReadingInsulinUnits.value,
            setInsulinUnits = params.setNewGlucoseReadingInsulinUnits,
            sameDayMeals = params.sameDayMeals.value,
            selectedMealId = params.newGlucoseReadingLinkedMealId.value,
            setSelectedMealId = params.setNewGlucoseReadingLinkedMealId,
            isEditMode = params.editingReadingId.value != null,
            glucoseUnit = params.glucoseUnit.value,
            onDismissRequest = {
                params.setShowAddGlucoseReadingDialog(false)
            },
            onSaveClicked = {
                params.submitNewGlucoseReading()
            }
        )
    }
}

data class GlucoseScreenParams(
    val allGlucoseReadings: State<List<GlucoseReading>>,
    val latestGlucoseReading: State<GlucoseReading?>,
    val selectedTimespan: State<GlucoseReadingTimespan>,
    val setSelectedTimespan: (GlucoseReadingTimespan) -> Unit,
    val newGlucoseReadingTimestamp: State<Long>,
    val setNewGlucoseReadingTimestamp: (Long) -> Unit,
    val newGlucoseReadingValue: State<String>,
    val setNewGlucoseReadingValue: (String) -> Unit,
    val newGlucoseReadingComment: State<String>,
    val setNewGlucoseReadingComment: (String) -> Unit,
    val showAddGlucoseReadingDialog: State<Boolean>,
    val setShowAddGlucoseReadingDialog: (Boolean) -> Unit,
    val submitNewGlucoseReading: () -> Unit,
    val deleteGlucoseReading: (GlucoseReading) -> Unit,
    val glucoseUnit: State<GlucoseUnit>,
    val allInsulinTypesForUser: State<List<InsulinType>>,
    val allMealsForUser: State<List<Meal>>,
    val sameDayMeals: State<List<Meal>>,
    val newGlucoseReadingInsulinTypeId: State<Long?>,
    val setNewGlucoseReadingInsulinTypeId: (Long?) -> Unit,
    val newGlucoseReadingInsulinUnits: State<String>,
    val setNewGlucoseReadingInsulinUnits: (String) -> Unit,
    val newGlucoseReadingLinkedMealId: State<Long?>,
    val setNewGlucoseReadingLinkedMealId: (Long?) -> Unit,
    val editingReadingId: State<Long?>,
    val startAddGlucoseReading: () -> Unit,
    val startEditingGlucoseReading: (GlucoseReading) -> Unit
)

private val ALLOWED_READINGS_COLUMN_HEIGHT = 400.dp