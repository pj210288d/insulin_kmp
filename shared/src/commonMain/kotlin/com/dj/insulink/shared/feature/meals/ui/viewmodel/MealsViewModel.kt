package com.dj.insulink.shared.feature.meals.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Osmi deljeni Compose Multiplatform MVP ekran - sada u punom paritetu sa Android-ovim
// app/feature/meals (vidi feature/meals/ui/viewmodel/MealsViewModel.kt tamo za obrazac po kom je
// ovo pisano - ista logika, samo UserSession.currentUserId umesto Hilt-ovog AuthRepository).
// Pretraga sastojaka (Spoonacular/USDA, vidi MealRepository.searchIngredients) i LogMeal
// foto-prepoznavanje (vidi MealRepository.analyzeFoodImage) su već platform-agnostični u
// MealRepository - jedino što je nedostajalo je ovaj ViewModel da ih zapravo pozove, i
// MealPhotoPickerLauncher (photo/ paket) za samo fotografisanje/biranje slike, što JESTE
// platform-specifično (dodato 2026-09-07).
class MealsViewModel(
    private val mealRepository: MealRepository
) : ViewModel() {

    // Vidi identičan komentar u GlucoseViewModel.kt - bez ovoga lokalna baza na novom
    // uređaju/instalaciji ostaje prazna, iako je nalog isti kao na uređaju gde su podaci uneti.
    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching {
                        mealRepository.fetchAllMealsForUserAndUpdateDatabase(userId)
                    }
                    loadDailyNutritionForDate(_selectedDate.value)
                }
            }
        }
    }

    private val _selectedDate = MutableStateFlow(currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mealsForSelectedDate: StateFlow<List<Meal>> = combine(
        UserSession.currentUserId,
        _selectedDate
    ) { userId, date -> userId to date }
        .flatMapLatest { (userId, date) ->
            if (userId != null) {
                mealRepository.getMealsByDateForUser(userId, date)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dailyNutrition = MutableStateFlow(DailyNutrition(0, 0, 0, 0, 0, 0.0))
    val dailyNutrition: StateFlow<DailyNutrition> = _dailyNutrition.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<Ingredient>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            val userId = UserSession.currentUserId.value
            if (query.isEmpty() || userId == null) {
                flowOf(emptyList())
            } else {
                mealRepository.searchIngredients(query, userId).catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIngredients = MutableStateFlow<List<MealIngredient>>(emptyList())
    val selectedIngredients: StateFlow<List<MealIngredient>> = _selectedIngredients.asStateFlow()

    private val _newMealName = MutableStateFlow("")
    val newMealName: StateFlow<String> = _newMealName.asStateFlow()

    private val _newMealComment = MutableStateFlow("")
    val newMealComment: StateFlow<String> = _newMealComment.asStateFlow()

    private val _newMealTimestamp = MutableStateFlow(currentTimeMillis())
    val newMealTimestamp: StateFlow<Long> = _newMealTimestamp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _showAddMealDialog = MutableStateFlow(false)
    val showAddMealDialog: StateFlow<Boolean> = _showAddMealDialog.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userIngredients: StateFlow<List<Ingredient>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) mealRepository.getUserIngredients(userId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showCreateIngredientDialog = MutableStateFlow(false)
    val showCreateIngredientDialog: StateFlow<Boolean> = _showCreateIngredientDialog.asStateFlow()

    private val _showMyIngredientsDialog = MutableStateFlow(false)
    val showMyIngredientsDialog: StateFlow<Boolean> = _showMyIngredientsDialog.asStateFlow()

    private val _isAnalyzingMealPhoto = MutableStateFlow(false)
    val isAnalyzingMealPhoto: StateFlow<Boolean> = _isAnalyzingMealPhoto.asStateFlow()

    private val _mealPhotoAnalysis = MutableStateFlow<FoodImageAnalysis?>(null)
    val mealPhotoAnalysis: StateFlow<FoodImageAnalysis?> = _mealPhotoAnalysis.asStateFlow()

    private val _mealPhotoAnalysisError = MutableStateFlow<String?>(null)
    val mealPhotoAnalysisError: StateFlow<String?> = _mealPhotoAnalysisError.asStateFlow()

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        loadDailyNutritionForDate(date)
    }

    private fun loadDailyNutritionForDate(date: Long) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            _dailyNutrition.value = mealRepository.getDailyNutrition(userId, date)
        }
    }

    fun setShowAddMealDialog(show: Boolean) {
        _showAddMealDialog.value = show
        if (show) {
            _newMealTimestamp.value = currentTimeMillis()
        } else {
            resetAddMealFields()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addIngredient(ingredient: Ingredient, quantity: Double, isFromPhotoAnalysis: Boolean = false) {
        _selectedIngredients.value += MealIngredient(
            mealId = 0L,
            ingredient = ingredient,
            quantity = quantity,
            isFromPhotoAnalysis = isFromPhotoAnalysis
        )
    }

    fun removeIngredient(mealIngredient: MealIngredient) {
        _selectedIngredients.value = _selectedIngredients.value.filter { it != mealIngredient }
    }

    fun updateIngredientQuantity(mealIngredient: MealIngredient, newQuantity: Double) {
        _selectedIngredients.value = _selectedIngredients.value.map {
            if (it == mealIngredient) it.copy(quantity = newQuantity) else it
        }
    }

    fun setNewMealName(name: String) {
        _newMealName.value = name
    }

    fun setNewMealComment(comment: String) {
        _newMealComment.value = comment
    }

    fun setNewMealTimestamp(timestamp: Long) {
        _newMealTimestamp.value = timestamp
    }

    fun setShowCreateIngredientDialog(show: Boolean) {
        _showCreateIngredientDialog.value = show
    }

    fun setShowMyIngredientsDialog(show: Boolean) {
        _showMyIngredientsDialog.value = show
    }

    fun analyzeMealPhoto(imageBytes: ByteArray) {
        viewModelScope.launch {
            _isAnalyzingMealPhoto.value = true
            _mealPhotoAnalysisError.value = null
            _mealPhotoAnalysis.value = null
            try {
                _mealPhotoAnalysis.value = mealRepository.analyzeFoodImage(imageBytes)
            } catch (e: Exception) {
                _mealPhotoAnalysisError.value = e.message ?: e.toString()
            } finally {
                _isAnalyzingMealPhoto.value = false
            }
        }
    }

    fun reportMealPhotoError(message: String) {
        _mealPhotoAnalysisError.value = message
    }

    fun acceptMealPhotoAnalysis(editedFoodNames: List<String>) {
        _mealPhotoAnalysis.value?.let { analysis ->
            val correctedName = editedFoodNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
                .ifEmpty { analysis.estimatedIngredient.name }
            addIngredient(analysis.estimatedIngredient.copy(name = correctedName), 100.0, isFromPhotoAnalysis = true)
        }
        dismissMealPhotoAnalysis()
    }

    fun dismissMealPhotoAnalysis() {
        _mealPhotoAnalysis.value = null
        _mealPhotoAnalysisError.value = null
    }

    fun submitNewMeal() {
        val userId = UserSession.currentUserId.value ?: return
        val ingredients = _selectedIngredients.value
        val name = _newMealName.value.trim()
        if (name.isEmpty() || ingredients.isEmpty()) return

        val totalCalories = ingredients.sumOf { (it.ingredient.caloriesPer100g * it.quantity / 100).toInt() }
        val totalCarbs = ingredients.sumOf { it.ingredient.carbsPer100g * it.quantity / 100 }
        val totalProtein = ingredients.sumOf { it.ingredient.proteinPer100g * it.quantity / 100 }
        val totalFat = ingredients.sumOf { it.ingredient.fatPer100g * it.quantity / 100 }
        val totalSugar = ingredients.sumOf { it.ingredient.sugarPer100g * it.quantity / 100 }
        val totalSalt = ingredients.sumOf { it.ingredient.saltPer100g * it.quantity / 100 }

        val meal = Meal(
            name = name,
            timestamp = _newMealTimestamp.value,
            calories = totalCalories,
            carbs = totalCarbs,
            protein = totalProtein,
            fat = totalFat,
            sugar = totalSugar,
            salt = totalSalt,
            comment = _newMealComment.value.trim().takeIf { it.isNotEmpty() },
            userId = userId,
            ingredients = ingredients
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                mealRepository.insert(userId, meal)
                loadDailyNutritionForDate(_selectedDate.value)
                setShowAddMealDialog(false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMeal(meal: Meal) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            try {
                mealRepository.delete(userId, meal)
                loadDailyNutritionForDate(_selectedDate.value)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createCustomIngredient(ingredient: Ingredient) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                mealRepository.insertIngredient(ingredient.copy(userId = userId))
                setShowCreateIngredientDialog(false)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCustomIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            try {
                mealRepository.deleteIngredient(ingredient)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun resetAddMealFields() {
        _newMealName.value = ""
        _newMealComment.value = ""
        _searchQuery.value = ""
        _selectedIngredients.value = emptyList()
        dismissMealPhotoAnalysis()
    }
}
