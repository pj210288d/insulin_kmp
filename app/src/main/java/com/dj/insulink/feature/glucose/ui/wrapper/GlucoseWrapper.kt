package com.dj.insulink.feature.glucose.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.glucose.ui.GlucoseScreen
import com.dj.insulink.feature.glucose.ui.GlucoseScreenParams
import com.dj.insulink.feature.glucose.ui.viewmodel.GlucoseViewModel

@Composable
fun GlucoseWrapper(
    currentUser: User?
) {
    val viewModel: GlucoseViewModel = hiltViewModel()

    val allGlucoseReadings = viewModel.allGlucoseReadings.collectAsStateWithLifecycle()
    val latestGlucoseReading = viewModel.latestGlucoseReading.collectAsStateWithLifecycle()
    val newGlucoseReadingTimestamp = viewModel.newGlucoseReadingTimestamp.collectAsStateWithLifecycle()
    val newGlucoseReadingValue = viewModel.newGlucoseReadingValue.collectAsStateWithLifecycle()
    val newGlucoseReadingComment = viewModel.newGlucoseReadingComment.collectAsStateWithLifecycle()
    val showAddGlucoseReadingDialog = viewModel.showAddGlucoseReadingDialog.collectAsStateWithLifecycle()
    val selectedTimespan = viewModel.selectedTimespan.collectAsStateWithLifecycle()
    val glucoseUnit = viewModel.glucoseUnit.collectAsStateWithLifecycle()
    val allInsulinTypesForUser = viewModel.allInsulinTypesForUser.collectAsStateWithLifecycle()
    val allMealsForUser = viewModel.allMealsForUser.collectAsStateWithLifecycle()
    val sameDayMealsForNewReading = viewModel.sameDayMealsForNewReading.collectAsStateWithLifecycle()
    val newGlucoseReadingInsulinTypeId = viewModel.newGlucoseReadingInsulinTypeId.collectAsStateWithLifecycle()
    val newGlucoseReadingInsulinUnits = viewModel.newGlucoseReadingInsulinUnits.collectAsStateWithLifecycle()
    val newGlucoseReadingLinkedMealId = viewModel.newGlucoseReadingLinkedMealId.collectAsStateWithLifecycle()
    val editingReadingId = viewModel.editingReadingId.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        viewModel.fetchAllGlucoseReadingsForUserAndUpdateDatabase(currentUser?.uid)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGlucoseUnit()
    }

    GlucoseScreen(
        params = GlucoseScreenParams(
            allGlucoseReadings = allGlucoseReadings,
            latestGlucoseReading = latestGlucoseReading,
            selectedTimespan = selectedTimespan,
            setSelectedTimespan = viewModel::setSelectedTimespan,
            newGlucoseReadingTimestamp = newGlucoseReadingTimestamp,
            setNewGlucoseReadingTimestamp = viewModel::setNewGlucoseReadingTimestamp,
            newGlucoseReadingValue = newGlucoseReadingValue,
            setNewGlucoseReadingValue = viewModel::setNewGlucoseReadingValue,
            newGlucoseReadingComment = newGlucoseReadingComment,
            setNewGlucoseReadingComment = viewModel::setNewGlucoseReadingComment,
            showAddGlucoseReadingDialog = showAddGlucoseReadingDialog,
            setShowAddGlucoseReadingDialog = viewModel::setShowAddGlucoseReadingDialog,
            submitNewGlucoseReading = {
                viewModel.submitNewGlucoseReading(currentUser?.uid)
            },
            deleteGlucoseReading = {
                viewModel.deleteGlucoseReading(currentUser?.uid, it)
            },
            glucoseUnit = glucoseUnit,
            allInsulinTypesForUser = allInsulinTypesForUser,
            allMealsForUser = allMealsForUser,
            sameDayMeals = sameDayMealsForNewReading,
            newGlucoseReadingInsulinTypeId = newGlucoseReadingInsulinTypeId,
            setNewGlucoseReadingInsulinTypeId = viewModel::setNewGlucoseReadingInsulinTypeId,
            newGlucoseReadingInsulinUnits = newGlucoseReadingInsulinUnits,
            setNewGlucoseReadingInsulinUnits = viewModel::setNewGlucoseReadingInsulinUnits,
            newGlucoseReadingLinkedMealId = newGlucoseReadingLinkedMealId,
            setNewGlucoseReadingLinkedMealId = viewModel::setNewGlucoseReadingLinkedMealId,
            editingReadingId = editingReadingId,
            startAddGlucoseReading = viewModel::startAddGlucoseReading,
            startEditingGlucoseReading = viewModel::startEditingGlucoseReading
        )
    )
}