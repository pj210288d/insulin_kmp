package com.dj.insulink.shared.feature.fitness.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.fitness.data.repository.ExerciseRepository
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Šesti deljeni Compose Multiplatform MVP ekran - vidi Glucose/Statistics/Insulin/Settings/
// Reminders ViewModel-e za obrazac. Namerno samo dodavanje + lista (bez brisanja) - Android-ov
// pravi Fitness ekran (feature/fitness/ui/viewmodel/FitnessViewModel u :app) takođe nema
// brisanje (ExerciseDao nema per-item delete metodu, samo deleteAllForUser za remote-refresh),
// pa je ovo stvarna 1:1 paritetna funkcionalnost, ne svesno umanjena.
class FitnessViewModel(
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val exercises: StateFlow<List<Exercise>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                exerciseRepository.getAllExercisesForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sportName = MutableStateFlow("")
    val sportName: StateFlow<String> = _sportName.asStateFlow()

    private val _durationMinutes = MutableStateFlow("")
    val durationMinutes: StateFlow<String> = _durationMinutes.asStateFlow()

    private val _glucoseBefore = MutableStateFlow("")
    val glucoseBefore: StateFlow<String> = _glucoseBefore.asStateFlow()

    private val _glucoseAfter = MutableStateFlow("")
    val glucoseAfter: StateFlow<String> = _glucoseAfter.asStateFlow()

    fun setSportName(value: String) {
        _sportName.value = value
    }

    fun setDurationMinutes(value: String) {
        _durationMinutes.value = value.filter { it.isDigit() }
    }

    fun setGlucoseBefore(value: String) {
        _glucoseBefore.value = value.filter { it.isDigit() }
    }

    fun setGlucoseAfter(value: String) {
        _glucoseAfter.value = value.filter { it.isDigit() }
    }

    fun addExercise() {
        val userId = UserSession.currentUserId.value ?: return
        val name = _sportName.value.trim()
        val minutes = _durationMinutes.value.toIntOrNull()
        val before = _glucoseBefore.value.toIntOrNull()
        val after = _glucoseAfter.value.toIntOrNull()
        if (name.isEmpty() || minutes == null || before == null || after == null) return

        viewModelScope.launch {
            exerciseRepository.insert(
                userId,
                Exercise(
                    id = 0,
                    userId = userId,
                    sportName = name,
                    durationHours = minutes / MINUTES_PER_HOUR,
                    durationMinutes = minutes % MINUTES_PER_HOUR,
                    glucoseBefore = before,
                    glucoseAfter = after
                )
            )
        }
        _sportName.value = ""
        _durationMinutes.value = ""
        _glucoseBefore.value = ""
        _glucoseAfter.value = ""
    }
}

private const val MINUTES_PER_HOUR = 60
