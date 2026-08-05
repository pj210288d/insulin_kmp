package com.dj.insulink.feature.glucose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.wear.WearSyncManager
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlucoseViewModel @Inject constructor(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val authRepository: AuthRepository,
    private val settingsPreferences: SettingsPreferences,
    private val insulinTypeRepository: InsulinTypeRepository,
    private val mealRepository: MealRepository,
    private val wearSyncManager: WearSyncManager
) : ViewModel() {

    private val _glucoseUnit = MutableStateFlow(settingsPreferences.getGlucoseUnit())
    val glucoseUnit: StateFlow<GlucoseUnit> = _glucoseUnit.asStateFlow()

    fun refreshGlucoseUnit() {
        _glucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }

    private val _newGlucoseReadingTimestamp = MutableStateFlow(System.currentTimeMillis())
    val newGlucoseReadingTimestamp = _newGlucoseReadingTimestamp.asStateFlow()

    private val _newGlucoseReadingValue = MutableStateFlow("")
    val newGlucoseReadingValue = _newGlucoseReadingValue.asStateFlow()

    private val _newGlucoseReadingComment = MutableStateFlow("")
    val newGlucoseReadingComment = _newGlucoseReadingComment.asStateFlow()

    private val _showAddGlucoseReadingDialog = MutableStateFlow(false)
    val showAddGlucoseReadingDialog = _showAddGlucoseReadingDialog.asStateFlow()

    private val _selectedTimespan = MutableStateFlow(GlucoseReadingTimespan.ALL_READINGS)
    val selectedTimespan = _selectedTimespan.asStateFlow()

    private val _editingReadingId = MutableStateFlow<Long?>(null)
    val editingReadingId = _editingReadingId.asStateFlow()

    private val _newGlucoseReadingInsulinTypeId = MutableStateFlow<Long?>(null)
    val newGlucoseReadingInsulinTypeId = _newGlucoseReadingInsulinTypeId.asStateFlow()

    private val _newGlucoseReadingInsulinUnits = MutableStateFlow("")
    val newGlucoseReadingInsulinUnits = _newGlucoseReadingInsulinUnits.asStateFlow()

    private val _newGlucoseReadingLinkedMealId = MutableStateFlow<Long?>(null)
    val newGlucoseReadingLinkedMealId = _newGlucoseReadingLinkedMealId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val allInsulinTypesForUser: StateFlow<List<InsulinType>> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                insulinTypeRepository.getAllInsulinTypesForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // All of the user's meals, unfiltered by date - used to resolve a reading's linkedMealId
    // to a display name in the list (unlike sameDayMealsForNewReading below, which is scoped
    // to the dialog's currently selected date and only useful for the meal picker).
    @OptIn(ExperimentalCoroutinesApi::class)
    val allMealsForUser: StateFlow<List<Meal>> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                mealRepository.getAllMealsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val sameDayMealsForNewReading: StateFlow<List<Meal>> = combine(
        authRepository.getCurrentUserFlow(),
        _newGlucoseReadingTimestamp
    ) { userId, timestamp ->
        userId to timestamp
    }.flatMapLatest { (userId, timestamp) ->
        if (userId != null) {
            mealRepository.getMealsByDateForUser(userId, timestamp)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val allGlucoseReadings: StateFlow<List<GlucoseReading>> = combine(
        authRepository.getCurrentUserFlow(),
        _selectedTimespan
    ) { userId, timespan ->
        userId to timespan
    }.flatMapLatest { (userId, timespan) ->
        if (userId != null) {
            glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
                .map { readings -> filterGlucoseReadingsByTimespan(readings, timespan) }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val latestGlucoseReading: StateFlow<GlucoseReading?> = allGlucoseReadings
        .map { it.firstOrNull() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId: String?) {
        viewModelScope.launch {
            userId?.let {
                glucoseReadingRepository.fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId)
            }
        }
    }

    fun submitNewGlucoseReading(userId: String?) {
        viewModelScope.launch {
            userId?.let {
                val enteredValue = newGlucoseReadingValue.value.toDoubleOrNull() ?: return@launch
                val storedValue = if (_glucoseUnit.value == GlucoseUnit.MMOL_L) {
                    GlucoseUnit.convertMmolLToMgDl(enteredValue).toInt()
                } else {
                    enteredValue.toInt()
                }
                val reading = GlucoseReading(
                    id = _editingReadingId.value ?: 0,
                    userId = userId,
                    timestamp = newGlucoseReadingTimestamp.value,
                    value = storedValue,
                    comment = newGlucoseReadingComment.value,
                    insulinTypeId = _newGlucoseReadingInsulinTypeId.value,
                    insulinUnits = _newGlucoseReadingInsulinUnits.value.toDoubleOrNull(),
                    linkedMealId = _newGlucoseReadingLinkedMealId.value
                )

                if (_editingReadingId.value == null) {
                    glucoseReadingRepository.insert(userId = userId, reading = reading)
                } else {
                    glucoseReadingRepository.update(userId = userId, reading = reading)
                }
                pushLatestReadingToWear(userId)
                resetNewReadingDialogFields()
            }
        }
    }

    fun startAddGlucoseReading() {
        resetNewReadingDialogFields()
        _showAddGlucoseReadingDialog.value = true
    }

    fun startEditingGlucoseReading(glucoseReading: GlucoseReading) {
        _editingReadingId.value = glucoseReading.id
        _newGlucoseReadingTimestamp.value = glucoseReading.timestamp
        _newGlucoseReadingValue.value = _glucoseUnit.value.formatValue(glucoseReading.value)
        _newGlucoseReadingComment.value = glucoseReading.comment
        _newGlucoseReadingInsulinTypeId.value = glucoseReading.insulinTypeId
        _newGlucoseReadingInsulinUnits.value = glucoseReading.insulinUnits?.toString() ?: ""
        _newGlucoseReadingLinkedMealId.value = glucoseReading.linkedMealId
        _showAddGlucoseReadingDialog.value = true
    }

    fun deleteGlucoseReading(userId: String?, glucoseReading: GlucoseReading) {
        viewModelScope.launch {
            userId?.let {
                glucoseReadingRepository.delete(
                    userId = userId,
                    reading = glucoseReading
                )
            }
        }
    }

    fun setNewGlucoseReadingTimestamp(newTimestamp: Long) {
        _newGlucoseReadingTimestamp.value = newTimestamp
    }

    fun setNewGlucoseReadingValue(newValue: String) {
        _newGlucoseReadingValue.value = newValue
    }

    fun setNewGlucoseReadingComment(comment: String) {
        if (comment.length <= COMMENT_MAXIMUM_LENGTH) {
            _newGlucoseReadingComment.value = comment
        }
    }

    fun setShowAddGlucoseReadingDialog(isVisible: Boolean) {
        _showAddGlucoseReadingDialog.value = isVisible
    }

    fun setSelectedTimespan(newTimespan: GlucoseReadingTimespan) {
        _selectedTimespan.value = newTimespan
    }

    fun setNewGlucoseReadingInsulinTypeId(insulinTypeId: Long?) {
        _newGlucoseReadingInsulinTypeId.value = insulinTypeId
    }

    fun setNewGlucoseReadingInsulinUnits(units: String) {
        _newGlucoseReadingInsulinUnits.value = units.filter { it.isDigit() || it == '.' }
    }

    fun setNewGlucoseReadingLinkedMealId(mealId: Long?) {
        _newGlucoseReadingLinkedMealId.value = mealId
    }

    private fun filterGlucoseReadingsByTimespan(
        allGlucoseReadings: List<GlucoseReading>,
        timespan: GlucoseReadingTimespan
    ): List<GlucoseReading> {
        if (timespan == GlucoseReadingTimespan.ALL_READINGS) {
            return allGlucoseReadings
        }

        val cutoffTime = System.currentTimeMillis() - timespan.milliseconds
        return allGlucoseReadings.filter { it.timestamp >= cutoffTime }
    }

    // Recomputes the true latest reading (unfiltered by the timespan selector - see
    // allGlucoseReadings above) and pushes it to the paired Wear OS watch. Always recomputes
    // rather than assuming the just-submitted reading is newest, since editing an older entry
    // must not overwrite the watch's displayed value with stale data.
    private fun pushLatestReadingToWear(userId: String) {
        viewModelScope.launch {
            val latest = glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
                .first()
                .maxByOrNull { it.timestamp }
            wearSyncManager.pushLatestReading(latest, _glucoseUnit.value)
        }
    }

    private fun resetNewReadingDialogFields() {
        _newGlucoseReadingTimestamp.value = System.currentTimeMillis()
        _newGlucoseReadingValue.value = ""
        _newGlucoseReadingComment.value = ""
        _newGlucoseReadingInsulinTypeId.value = null
        _newGlucoseReadingInsulinUnits.value = ""
        _newGlucoseReadingLinkedMealId.value = null
        _editingReadingId.value = null
    }
}

enum class GlucoseReadingTimespan(val displayNameRes: Int, val milliseconds: Long) {
    ALL_READINGS(com.dj.insulink.R.string.timespan_all_readings, Long.MAX_VALUE),
    LAST_DAY(com.dj.insulink.R.string.timespan_last_24_hours, 24 * 60 * 60 * 1000L),
    LAST_3_DAYS(com.dj.insulink.R.string.timespan_last_3_days, 72 * 60 * 60 * 1000L),
    LAST_WEEK(com.dj.insulink.R.string.timespan_last_week, 7 * 24 * 60 * 60 * 1000L),
    LAST_MONTH(com.dj.insulink.R.string.timespan_last_month, 30 * 24 * 60 * 60 * 1000L);
}

private const val COMMENT_MAXIMUM_LENGTH = 20