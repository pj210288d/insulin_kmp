package com.dj.insulink.feature.meals.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate = _selectedDate.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val mealsForSelectedDate: StateFlow<List<Meal>> = combine(
        authRepository.getCurrentUserFlow(),
        _selectedDate
    ) { userId, date ->
        userId to date
    }.flatMapLatest { (userId, date) ->
        if (userId != null) {
            mealRepository.getMealsByDateForUser(userId, date)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _dailyNutrition = MutableStateFlow(DailyNutrition(0, 0, 0, 0, 0, 0.0))
    val dailyNutrition = _dailyNutrition.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val searchResults: StateFlow<List<Ingredient>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(emptyList())
            } else {
                val userId = authRepository.getCurrentUserFlow().first()
                if (userId != null) {
                    mealRepository.searchIngredients(query, userId)
                        .catch { emit(emptyList()) }
                } else {
                    flowOf(emptyList())
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedIngredients = MutableStateFlow<List<MealIngredient>>(emptyList())
    val selectedIngredients = _selectedIngredients.asStateFlow()

    private val _newMealName = MutableStateFlow("")
    val newMealName = _newMealName.asStateFlow()

    private val _newMealComment = MutableStateFlow("")
    val newMealComment = _newMealComment.asStateFlow()

    private val _newMealTimestamp = MutableStateFlow(System.currentTimeMillis())
    val newMealTimestamp = _newMealTimestamp.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userIngredients: StateFlow<List<Ingredient>> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                mealRepository.getUserIngredients(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showCreateIngredientDialog = MutableStateFlow(false)
    val showCreateIngredientDialog = _showCreateIngredientDialog.asStateFlow()

    private val _showMyIngredientsDialog = MutableStateFlow(false)
    val showMyIngredientsDialog = _showMyIngredientsDialog.asStateFlow()

    private val _isAnalyzingMealPhoto = MutableStateFlow(false)
    val isAnalyzingMealPhoto = _isAnalyzingMealPhoto.asStateFlow()

    private val _mealPhotoAnalysis = MutableStateFlow<FoodImageAnalysis?>(null)
    val mealPhotoAnalysis = _mealPhotoAnalysis.asStateFlow()

    private val _mealPhotoAnalysisError = MutableStateFlow<String?>(null)
    val mealPhotoAnalysisError = _mealPhotoAnalysisError.asStateFlow()

    fun fetchAllMealsForUserAndUpdateDatabase(userId: String?) {
        viewModelScope.launch {
            userId?.let {
                mealRepository.fetchAllMealsForUserAndUpdateDatabase(userId)
            }
        }
    }

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
        loadDailyNutritionForDate(date)
    }

    fun loadDailyNutritionForDate(date: Long) {
        viewModelScope.launch {
            val userId =
                authRepository.getCurrentUserFlow().stateIn(viewModelScope).value ?: return@launch
            val nutrition = mealRepository.getDailyNutrition(userId, date)
            _dailyNutrition.value = nutrition
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addIngredient(ingredient: Ingredient, quantity: Double) {
        val mealIngredient = MealIngredient(
            mealId = 0L,
            ingredient = ingredient,
            quantity = quantity
        )
        _selectedIngredients.value += mealIngredient
    }

    fun removeIngredient(mealIngredient: MealIngredient) {
        _selectedIngredients.value = _selectedIngredients.value.filter { it != mealIngredient }
    }

    fun updateIngredientQuantity(mealIngredient: MealIngredient, newQuantity: Double) {
        _selectedIngredients.value = _selectedIngredients.value.map { ingredient ->
            if (ingredient == mealIngredient) ingredient.copy(quantity = newQuantity) else ingredient
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

    fun prepareNewMeal() {
        resetAddMealFields()
        _newMealTimestamp.value = System.currentTimeMillis()
    }

    fun setShowCreateIngredientDialog(show: Boolean) {
        _showCreateIngredientDialog.value = show
    }

    fun setShowMyIngredientsDialog(show: Boolean) {
        _showMyIngredientsDialog.value = show
    }

    fun analyzeMealPhoto(imageBytes: ByteArray) {
        Log.d(TAG, "analyzeMealPhoto: starting, ${imageBytes.size} bytes")
        viewModelScope.launch {
            _isAnalyzingMealPhoto.value = true
            _mealPhotoAnalysisError.value = null
            _mealPhotoAnalysis.value = null
            try {
                val result = mealRepository.analyzeFoodImage(imageBytes)
                Log.d(TAG, "analyzeMealPhoto: success, recognized=${result.recognizedFoodNames}")
                _mealPhotoAnalysis.value = result
            } catch (e: Exception) {
                Log.e(TAG, "analyzeMealPhoto: failed", e)
                _mealPhotoAnalysisError.value = e.message ?: e.toString()
            } finally {
                _isAnalyzingMealPhoto.value = false
            }
        }
    }

    fun reportMealPhotoReadError() {
        Log.e(TAG, "reportMealPhotoReadError: camera did not return a usable photo")
        _mealPhotoAnalysisError.value = "Could not read the photo from the camera"
    }

    fun acceptMealPhotoAnalysis(editedFoodNames: List<String>) {
        _mealPhotoAnalysis.value?.let { analysis ->
            val correctedName = editedFoodNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(", ")
                .ifEmpty { analysis.estimatedIngredient.name }
            addIngredient(analysis.estimatedIngredient.copy(name = correctedName), 100.0)
        }
        dismissMealPhotoAnalysis()
    }

    fun dismissMealPhotoAnalysis() {
        _mealPhotoAnalysis.value = null
        _mealPhotoAnalysisError.value = null
    }

    fun submitNewMeal(userId: String?, onSuccess: () -> Unit) {
        if (userId == null) return

        val ingredients = _selectedIngredients.value
        if (ingredients.isEmpty()) return

        val totalCalories =
            ingredients.sumOf { (it.ingredient.caloriesPer100g * it.quantity / 100).toInt() }
        val totalCarbs = ingredients.sumOf { it.ingredient.carbsPer100g * it.quantity / 100 }
        val totalProtein = ingredients.sumOf { it.ingredient.proteinPer100g * it.quantity / 100 }
        val totalFat = ingredients.sumOf { it.ingredient.fatPer100g * it.quantity / 100 }
        val totalSugar = ingredients.sumOf { it.ingredient.sugarPer100g * it.quantity / 100 }
        val totalSalt = ingredients.sumOf { it.ingredient.saltPer100g * it.quantity / 100 }

        val meal = Meal(
            name = _newMealName.value,
            timestamp = _newMealTimestamp.value,
            calories = totalCalories,
            carbs = totalCarbs,
            protein = totalProtein,
            fat = totalFat,
            sugar = totalSugar,
            salt = totalSalt,
            comment = _newMealComment.value.takeIf { it.isNotEmpty() },
            userId = userId,
            ingredients = ingredients
        )

        viewModelScope.launch {
            _isLoading.value = true
            try {
                mealRepository.insert(userId, meal)
                resetAddMealFields()
                loadDailyNutritionForDate(_selectedDate.value)
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMeal(userId: String?, meal: Meal) {
        viewModelScope.launch {
            userId?.let {
                try {
                    mealRepository.delete(userId, meal)
                    loadDailyNutritionForDate(_selectedDate.value)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun createCustomIngredient(userId: String?, ingredient: Ingredient) {
        if (userId == null) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val customIngredient = ingredient.copy(userId = userId)
                mealRepository.insertIngredient(customIngredient)
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

    private companion object {
        const val TAG = "MealsViewModel"
    }
}
