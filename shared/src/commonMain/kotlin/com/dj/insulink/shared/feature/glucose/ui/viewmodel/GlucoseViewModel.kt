package com.dj.insulink.shared.feature.glucose.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
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
// Dijalog za dodavanje/izmenu očitavanja je sada u punom paritetu sa Android-ovim ekranom
// (app/.../feature/glucose/ui/AddGlucoseReadingDialog.kt + GlucoseViewModel.kt) - ručna izmena
// datuma/vremena, insulin tip, insulinske jedinice, povezan obrok - obrazac i imena polja
// namerno prate taj kod 1:1. Jedino i dalje izostaje Wear OS push (Android-only hardver, van
// dosega ove migracije).
class GlucoseViewModel(
    private val glucoseReadingRepository: GlucoseReadingRepository,
    private val settingsPreferences: SettingsPreferences,
    private val insulinTypeRepository: InsulinTypeRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    // Vidi identičan komentar u drugim shared ViewModel-ima (Insulin/Fitness/Reminders/Meals) -
    // bez ovoga lokalna baza na novom uređaju/instalaciji ostaje prazna, iako je nalog isti kao
    // na uređaju gde su podaci uneti.
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

    // Svi korisnikovi insulin tipovi, za dropdown u dijalogu - vidi InsulinViewModel za isti
    // izvor podataka (Insulin tab).
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

    // Obroci ISTOG dana kao trenutno uneti datum/vreme u dijalogu (ne izabranog dana na glavnom
    // ekranu) - isti obrazac kao Android-ov sameDayMealsForNewReading. Mora biti posle
    // _newTimestamp deklaracije (Kotlin inicijalizuje property-je odozgo na dole).
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
        _newValue.value = _glucoseUnit.value.formatValue(reading.value)
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
        val storedValue = if (_glucoseUnit.value == GlucoseUnit.MMOL_L) {
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
