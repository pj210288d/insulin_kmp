package com.dj.insulink.shared.feature.meals.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Osmi deljeni Compose Multiplatform MVP ekran - vidi ostale ViewModel-e u shared/commonMain za
// obrazac. Namerno SAMO ručan unos (naziv/kalorije/ugljeni hidrati) - LogMeal prepoznavanje sa
// slike (postoji u MealRepository.analyzeFoodImage, mrežni deo je već dokazano platform-
// agnostičan) namerno nije povezano ovde jer zahteva fotografisanje, što je platform-specifičan
// UI kod (Android koristi CameraX/Intent u AddMealWrapper.kt - iOS bi trebalo UIImagePicker,
// nov, nepotvrđen kod bez mogućnosti testiranja pre Mac-a). Sastojci/pretraga takođe van obima -
// isti princip kao Insulin/Fitness (prost CRUD, ne pun workflow).
class MealsViewModel(
    private val mealRepository: MealRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val meals: StateFlow<List<Meal>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                mealRepository.getAllMealsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newName = MutableStateFlow("")
    val newName: StateFlow<String> = _newName.asStateFlow()

    private val _newCalories = MutableStateFlow("")
    val newCalories: StateFlow<String> = _newCalories.asStateFlow()

    private val _newCarbs = MutableStateFlow("")
    val newCarbs: StateFlow<String> = _newCarbs.asStateFlow()

    fun setNewName(value: String) {
        _newName.value = value
    }

    fun setNewCalories(value: String) {
        _newCalories.value = value.filter { it.isDigit() }
    }

    fun setNewCarbs(value: String) {
        _newCarbs.value = value.filter { it.isDigit() || it == '.' }
    }

    fun addMeal() {
        val userId = UserSession.currentUserId.value ?: return
        val name = _newName.value.trim()
        if (name.isEmpty()) return

        viewModelScope.launch {
            mealRepository.insert(
                userId,
                Meal(
                    id = 0,
                    name = name,
                    timestamp = currentTimeMillis(),
                    calories = _newCalories.value.toIntOrNull(),
                    carbs = _newCarbs.value.toDoubleOrNull(),
                    protein = null,
                    fat = null,
                    sugar = null,
                    salt = null,
                    comment = null,
                    userId = userId
                )
            )
        }
        _newName.value = ""
        _newCalories.value = ""
        _newCarbs.value = ""
    }

    fun deleteMeal(meal: Meal) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            mealRepository.delete(userId, meal)
        }
    }
}
