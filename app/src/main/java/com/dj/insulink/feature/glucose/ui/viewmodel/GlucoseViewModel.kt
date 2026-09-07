package com.dj.insulink.feature.glucose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.wear.WearSyncManager
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.startOfDayMillis
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

    // The day currently shown on the main screen (defaults to today). Navigated via
    // goToPreviousDay()/goToNextDay(), including in response to a horizontal swipe on the
    // screen - see GlucoseScreen.
    private val _selectedDayStartMillis = MutableStateFlow(startOfDayMillis(currentTimeMillis()))
    val selectedDayStartMillis: StateFlow<Long> = _selectedDayStartMillis.asStateFlow()

    // Disabled once the selected day IS today - there's no data to show for a future day.
    val canGoToNextDay: StateFlow<Boolean> = _selectedDayStartMillis
        .map { it < startOfDayMillis(currentTimeMillis()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun goToPreviousDay() {
        _selectedDayStartMillis.value = shiftedDayStartMillis(_selectedDayStartMillis.value, -1)
    }

    fun goToNextDay() {
        val next = shiftedDayStartMillis(_selectedDayStartMillis.value, 1)
        if (next <= startOfDayMillis(currentTimeMillis())) {
            _selectedDayStartMillis.value = next
        }
    }

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

    // Readings for the day currently selected via goToPreviousDay()/goToNextDay() (see
    // selectedDayStartMillis above) - this is what's shown in the chart and the list below it.
    @OptIn(ExperimentalCoroutinesApi::class)
    val glucoseReadingsForSelectedDay: StateFlow<List<GlucoseReading>> = combine(
        authRepository.getCurrentUserFlow(),
        _selectedDayStartMillis
    ) { userId, dayStart ->
        userId to dayStart
    }.flatMapLatest { (userId, dayStart) ->
        if (userId != null) {
            val dayEnd = shiftedDayStartMillis(dayStart, 1) - 1
            glucoseReadingRepository.getGlucoseReadingsByDateRange(userId, dayStart, dayEnd)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // The true latest reading ever recorded - deliberately independent of the day being browsed
    // above, so the status card at the top of the screen always reflects the user's current
    // glucose even while they're looking back at an earlier day's history.
    @OptIn(ExperimentalCoroutinesApi::class)
    val latestGlucoseReading: StateFlow<GlucoseReading?> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .map { it.firstOrNull() } // DAO already orders by timestamp DESC
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId: String?) {
        viewModelScope.launch {
            userId?.let {
                glucoseReadingRepository.fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId)
                pushLatestReadingToWear(userId)
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
                // If the deleted reading was the latest one shown on the watch, this pushes
                // whatever is now the true latest (or "no reading" if the list is empty).
                pushLatestReadingToWear(userId)
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

    fun setNewGlucoseReadingInsulinTypeId(insulinTypeId: Long?) {
        _newGlucoseReadingInsulinTypeId.value = insulinTypeId
    }

    fun setNewGlucoseReadingInsulinUnits(units: String) {
        _newGlucoseReadingInsulinUnits.value = units.filter { it.isDigit() || it == '.' }
    }

    fun setNewGlucoseReadingLinkedMealId(mealId: Long?) {
        _newGlucoseReadingLinkedMealId.value = mealId
    }

    // Recomputes the true latest reading and pushes it to the paired Wear OS watch. Always
    // recomputes rather than assuming the just-submitted reading is newest, since editing an
    // older entry must not overwrite the watch's displayed value with stale data.
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

private const val COMMENT_MAXIMUM_LENGTH = 20
