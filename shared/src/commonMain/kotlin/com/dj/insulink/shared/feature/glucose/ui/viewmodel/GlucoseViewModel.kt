package com.dj.insulink.shared.feature.glucose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// MVP verzija Glucose ekrana deljena preko Compose Multiplatform-a (Android + iOS) - vidi
// GlucoseScreen.kt u istom paketu i App.kt (org.example.project) koji je koristi kao iOS root.
// Namerno manji obim od postojećeg, potpuno funkcionalnog Android-only ekrana
// (app/.../feature/glucose/ui/viewmodel/GlucoseViewModel.kt): bez insulin/meal povezivanja i
// bez Wear OS push-a, i bez ručne izmene datuma/vremena očitavanja (nova očitavanja dobijaju
// currentTimeMillis(), izmena čuva originalni timestamp) - sve da bi prvi iOS build/test
// ciklus, rađen "na slepo" dok autor ne dobije Mac, ostao što manjeg rizika. Android i dalje
// prevashodno koristi svoj postojeći ekran; ovaj deljeni je dodatno dostupan i na Android
// strani (side drawer) da dokaže da isti kod stvarno radi na oba OS-a.
class GlucoseViewModel(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _glucoseUnit = MutableStateFlow(settingsPreferences.getGlucoseUnit())
    val glucoseUnit: StateFlow<GlucoseUnit> = _glucoseUnit.asStateFlow()

    fun refreshGlucoseUnit() {
        _glucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }

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

    // Pravi najnoviji unos, namerno nezavisan od izabranog dana - vidi napomenu u Android
    // GlucoseViewModel-u za isto ponašanje.
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

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    private val _newValue = MutableStateFlow("")
    val newValue: StateFlow<String> = _newValue.asStateFlow()

    private val _newComment = MutableStateFlow("")
    val newComment: StateFlow<String> = _newComment.asStateFlow()

    private val _editingReading = MutableStateFlow<GlucoseReading?>(null)
    val editingReading: StateFlow<GlucoseReading?> = _editingReading.asStateFlow()

    fun setNewValue(value: String) {
        _newValue.value = value
    }

    fun setNewComment(comment: String) {
        if (comment.length <= COMMENT_MAX_LENGTH) {
            _newComment.value = comment
        }
    }

    fun startAddReading() {
        _editingReading.value = null
        _newValue.value = ""
        _newComment.value = ""
        _showAddDialog.value = true
    }

    fun startEditReading(reading: GlucoseReading) {
        _editingReading.value = reading
        _newValue.value = _glucoseUnit.value.formatValue(reading.value)
        _newComment.value = reading.comment
        _showAddDialog.value = true
    }

    fun dismissDialog() {
        _showAddDialog.value = false
    }

    fun submitReading() {
        val userId = UserSession.currentUserId.value ?: return
        val enteredValue = _newValue.value.toDoubleOrNull() ?: return
        val storedValue = if (_glucoseUnit.value == GlucoseUnit.MMOL_L) {
            GlucoseUnit.convertMmolLToMgDl(enteredValue).toInt()
        } else {
            enteredValue.toInt()
        }
        val editing = _editingReading.value
        val reading = GlucoseReading(
            id = editing?.id ?: 0,
            userId = userId,
            timestamp = editing?.timestamp ?: currentTimeMillis(),
            value = storedValue,
            comment = _newComment.value,
            insulinTypeId = editing?.insulinTypeId,
            insulinUnits = editing?.insulinUnits,
            linkedMealId = editing?.linkedMealId
        )
        viewModelScope.launch {
            if (editing == null) {
                glucoseReadingRepository.insert(userId, reading)
            } else {
                glucoseReadingRepository.update(userId, reading)
            }
        }
        _showAddDialog.value = false
    }

    fun deleteReading(reading: GlucoseReading) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            glucoseReadingRepository.delete(userId, reading)
        }
    }
}

private const val COMMENT_MAX_LENGTH = 20
