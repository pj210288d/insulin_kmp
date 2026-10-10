package com.dj.insulink.shared.feature.glucose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.data.wear.WearSyncCoordinator
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
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

class GlucoseViewModel(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val settingsPreferences: SettingsPreferences,
    private val insulinTypeRepository: InsulinTypeRepository,
    private val mealRepository: MealRepository,
    private val wearSyncCoordinator: WearSyncCoordinator
) : ViewModel() {

    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching {
                        glucoseReadingRepository.fetchAllGlucoseReadingsForUserAndUpdateDatabase(userId)
                    }
                }
            }
        }
    }

    val glucoseUnit: StateFlow<GlucoseUnit> = SettingsSession.currentGlucoseUnit

    private val _selectedDayStartMillis = MutableStateFlow(startOfDayMillis(currentTimeMillis()))
    val selectedDayStartMillis: StateFlow<Long> = _selectedDayStartMillis.asStateFlow()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val glucoseReadingsForSelectedDay: StateFlow<List<GlucoseReading>> = combine(
        UserSession.currentUserId, _selectedDayStartMillis
    ) { userId, dayStart -> userId to dayStart }
        .flatMapLatest { (userId, dayStart) ->
            if (userId != null) {
                val dayEnd = shiftedDayStartMillis(dayStart, 1) - 1
                glucoseReadingRepository.getGlucoseReadingsByDateRange(userId, dayStart, dayEnd)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val latestGlucoseReading: StateFlow<GlucoseReading?> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allInsulinTypesForUser: StateFlow<List<InsulinType>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                insulinTypeRepository.getAllInsulinTypesForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _newTimestamp = MutableStateFlow(currentTimeMillis())
    val newTimestamp: StateFlow<Long> = _newTimestamp.asStateFlow()

    private val _newValue = MutableStateFlow("")
    val newValue: StateFlow<String> = _newValue.asStateFlow()

    private val _newComment = MutableStateFlow("")
    val newComment: StateFlow<String> = _newComment.asStateFlow()

    private val _newInsulinTypeId = MutableStateFlow<Long?>(null)
    val newInsulinTypeId: StateFlow<Long?> = _newInsulinTypeId.asStateFlow()

    private val _newInsulinUnits = MutableStateFlow("")
    val newInsulinUnits: StateFlow<String> = _newInsulinUnits.asStateFlow()

    private val _newLinkedMealId = MutableStateFlow<Long?>(null)
    val newLinkedMealId: StateFlow<Long?> = _newLinkedMealId.asStateFlow()

    private val _editingReading = MutableStateFlow<GlucoseReading?>(null)
    val editingReading: StateFlow<GlucoseReading?> = _editingReading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val sameDayMealsForNewReading: StateFlow<List<Meal>> = combine(
        UserSession.currentUserId, _newTimestamp
    ) { userId, timestamp -> userId to timestamp }
        .flatMapLatest { (userId, timestamp) ->
            if (userId != null) {
                mealRepository.getMealsByDateForUser(userId, timestamp)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNewTimestamp(timestamp: Long) {
        _newTimestamp.value = timestamp
    }

    fun setNewValue(value: String) {
        _newValue.value = value
    }

    fun setNewComment(comment: String) {
        if (comment.length <= COMMENT_MAX_LENGTH) {
            _newComment.value = comment
        }
    }

    fun setNewInsulinTypeId(insulinTypeId: Long?) {
        _newInsulinTypeId.value = insulinTypeId
    }

    fun setNewInsulinUnits(units: String) {
        _newInsulinUnits.value = units.filter { it.isDigit() || it == '.' }
    }

    fun setNewLinkedMealId(mealId: Long?) {
        _newLinkedMealId.value = mealId
    }

    fun startAddReading() {
        _editingReading.value = null
        _newTimestamp.value = currentTimeMillis()
        _newValue.value = ""
        _newComment.value = ""
        _newInsulinTypeId.value = null
        _newInsulinUnits.value = ""
        _newLinkedMealId.value = null
        _showAddDialog.value = true
    }

    fun startEditReading(reading: GlucoseReading) {
        _editingReading.value = reading
        _newTimestamp.value = reading.timestamp
        _newValue.value = SettingsSession.currentGlucoseUnit.value.formatValue(reading.value)
        _newComment.value = reading.comment
        _newInsulinTypeId.value = reading.insulinTypeId
        _newInsulinUnits.value = reading.insulinUnits?.toString() ?: ""
        _newLinkedMealId.value = reading.linkedMealId
        _showAddDialog.value = true
    }

    fun dismissDialog() {
        _showAddDialog.value = false
    }

    fun submitReading() {
        val userId = UserSession.currentUserId.value ?: return
        val enteredValue = _newValue.value.toDoubleOrNull() ?: return
        val storedValue = if (SettingsSession.currentGlucoseUnit.value == GlucoseUnit.MMOL_L) {
            GlucoseUnit.convertMmolLToMgDl(enteredValue).toInt()
        } else {
            enteredValue.toInt()
        }
        val editing = _editingReading.value
        val reading = GlucoseReading(
            id = editing?.id ?: 0,
            userId = userId,
            timestamp = _newTimestamp.value,
            value = storedValue,
            comment = _newComment.value,
            insulinTypeId = _newInsulinTypeId.value,
            insulinUnits = _newInsulinUnits.value.toDoubleOrNull(),
            linkedMealId = _newLinkedMealId.value
        )
        viewModelScope.launch {
            if (editing == null) {
                glucoseReadingRepository.insert(userId, reading)
            } else {
                glucoseReadingRepository.update(userId, reading)
            }
            pushLatestReadingToWear(userId)
        }
        _showAddDialog.value = false
    }

    fun deleteReading(reading: GlucoseReading) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            glucoseReadingRepository.delete(userId, reading)
            pushLatestReadingToWear(userId)
        }
    }

    private suspend fun pushLatestReadingToWear(userId: String) {
        val latest = glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
            .first()
            .maxByOrNull { it.timestamp }
        wearSyncCoordinator.pushLatestReading(latest, SettingsSession.currentGlucoseUnit.value)
    }
}

private const val COMMENT_MAX_LENGTH = 20
