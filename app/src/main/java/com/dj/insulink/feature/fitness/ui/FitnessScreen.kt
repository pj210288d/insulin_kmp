package com.dj.insulink.feature.fitness.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dj.insulink.R
import com.dj.insulink.core.ui.theme.InsulinkTheme
import com.dj.insulink.feature.fitness.domain.model.Sport
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit

@Composable
fun FitnessScreen(
    params: FitnessScreenParams
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(InsulinkTheme.dimens.commonPadding16)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = InsulinkTheme.dimens.commonPadding16),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.fitness_screen_sports_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
            ) {
                items(params.sports.value) { sport ->
                    SportActivityItem(sport = sport, glucoseUnit = params.glucoseUnit.value)
                }
            }
        }

        FloatingActionButton(
            onClick = {
                params.setShowSportsActivityDialog(true)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(InsulinkTheme.dimens.commonPadding16),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = ""
            )
        }
    }
    Spacer(modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing16))
    if(params.showAddSportsActivityDialog.value){
        AddSportsActivityDialog(
            setShowAddExerciseDialog = params.setShowSportsActivityDialog,
            sportsList = params.sports.value.map { it.name },
            sportName = params.sportName,
            setSportName = params.setSportName,
            durationHours = params.durationHours,
            setDurationHours = params.setDurationHours,
            durationMinutes = params.durationMinutes,
            setDurationMinutes = params.setDurationMinutes,
            glucoseBefore = params.glucoseBefore,
            setGlucoseBefore = params.setGlucoseBefore,
            glucoseAfter = params.glucoseAfter,
            setGlucoseAfter = params.setGlucoseAfter,
            onAddExerciseClick = params.onAddExerciseClick,
            glucoseUnit = params.glucoseUnit.value
        )
    }
}

@Composable
private fun SportActivityItem(
    sport: Sport,
    glucoseUnit: GlucoseUnit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = InsulinkTheme.dimens.commonElevation2)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InsulinkTheme.dimens.commonPadding16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = sport.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.size(InsulinkTheme.dimens.commonSpacing12))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(InsulinkTheme.dimens.commonSpacing8)
                ) {
                    MetricCard(
                        label = stringResource(R.string.fitness_screen_average_drop_label),
                        value = glucoseUnit.formatValue(sport.avgDropPerHour),
                        labelColor = InsulinkTheme.colors.averageDropTitle,
                        valueColor = InsulinkTheme.colors.averageDropLabel,
                        backgroundColor = InsulinkTheme.colors.averageDropBackground,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        label = stringResource(R.string.fitness_screen_last_drop_label),
                        value = glucoseUnit.formatValue(sport.lastDropPerHour),
                        labelColor = InsulinkTheme.colors.lastDropTitle,
                        valueColor = InsulinkTheme.colors.lastDropLabel,
                        backgroundColor = InsulinkTheme.colors.lastDropBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(InsulinkTheme.dimens.commonButtonRadius12),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = InsulinkTheme.dimens.commonElevation0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(InsulinkTheme.dimens.commonPadding16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class FitnessScreenParams(
    val sports: State<List<Sport>>,
    val showAddSportsActivityDialog: State<Boolean>,
    val setShowSportsActivityDialog: (Boolean) -> Unit,
    val sportName: State<String>,
    val setSportName: (String) -> Unit,
    val durationHours: State<String>,
    val setDurationHours: (String) -> Unit,
    val durationMinutes: State<String>,
    val setDurationMinutes: (String) -> Unit,
    val glucoseBefore: State<String>,
    val setGlucoseBefore: (String) -> Unit,
    val glucoseAfter: State<String>,
    val setGlucoseAfter: (String) -> Unit,
    val onAddExerciseClick: () -> Unit,
    val glucoseUnit: State<GlucoseUnit>
)