package com.dj.insulink.feature.meals.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysis
import com.dj.insulink.shared.feature.meals.domain.model.FoodImageAnalysisException
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.ui.viewmodel.MealsViewModel
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.collectInBackground
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MealsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mealRepository: MealRepository = mockk(relaxed = true)

    private val rice = Ingredient(
        id = 1,
        name = "Rice",
        caloriesPer100g = 200.0,
        proteinPer100g = 10.0,
        carbsPer100g = 20.0,
        fatPer100g = 5.0,
        sugarPer100g = 2.0,
        saltPer100g = 1.0
    )
    private val nutrition = DailyNutrition(calories = 1800, carbs = 200, protein = 90, fat = 60, sugar = 40, salt = 4.5)
    private val meal = Meal(
        id = 3,
        name = "Lunch",
        timestamp = 1_000L,
        calories = 300,
        carbs = 30.0,
        protein = 15.0,
        fat = 7.5,
        sugar = 3.0,
        salt = 1.5,
        comment = null,
        userId = "u1"
    )

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        every { mealRepository.getMealsByDateForUser(any(), any()) } returns flowOf(emptyList())
        every { mealRepository.getUserIngredients(any()) } returns flowOf(emptyList())
        every { mealRepository.searchIngredients(any(), any()) } returns flowOf(emptyList())
        coEvery { mealRepository.getDailyNutrition(any(), any()) } returns nutrition
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    private fun buildViewModel() = MealsViewModel(mealRepository)

    @Test
    fun `init fetches meals and loads daily nutrition for the selected date`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = buildViewModel()
            advanceUntilIdle()

            coVerify { mealRepository.fetchAllMealsForUserAndUpdateDatabase("u1") }
            coVerify { mealRepository.getDailyNutrition("u1", vm.selectedDate.value) }
            assertEquals(nutrition, vm.dailyNutrition.value)
        }

    @Test
    fun `meals and custom ingredients come from the repository`() = runTest(mainDispatcherRule.dispatcher) {
        every { mealRepository.getMealsByDateForUser("u1", any()) } returns flowOf(listOf(meal))
        every { mealRepository.getUserIngredients("u1") } returns flowOf(listOf(rice))

        val vm = buildViewModel()
        collectInBackground(vm.mealsForSelectedDate, vm.userIngredients)
        advanceUntilIdle()

        assertEquals(listOf(meal), vm.mealsForSelectedDate.value)
        assertEquals(listOf(rice), vm.userIngredients.value)
    }

    @Test
    fun `setSelectedDate reloads daily nutrition for that date`() = runTest(mainDispatcherRule.dispatcher) {
        val otherDay = DailyNutrition(calories = 900, carbs = 100, protein = 40, fat = 30, sugar = 20, salt = 2.0)
        coEvery { mealRepository.getDailyNutrition("u1", 5_000L) } returns otherDay
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.setSelectedDate(5_000L)
        advanceUntilIdle()

        assertEquals(5_000L, vm.selectedDate.value)
        assertEquals(otherDay, vm.dailyNutrition.value)
    }

    @Test
    fun `search returns matching ingredients after the query settles`() = runTest(mainDispatcherRule.dispatcher) {
        every { mealRepository.searchIngredients("ric", "u1") } returns flowOf(listOf(rice))
        val vm = buildViewModel()
        collectInBackground(vm.searchResults)

        vm.setSearchQuery("ric")
        advanceUntilIdle()
        assertEquals(listOf(rice), vm.searchResults.value)

        vm.setSearchQuery("")
        advanceUntilIdle()
        assertEquals(emptyList<Ingredient>(), vm.searchResults.value)
    }

    @Test
    fun `ingredients can be added, resized and removed`() {
        val vm = buildViewModel()

        vm.addIngredient(rice, 150.0)
        val added = vm.selectedIngredients.value.single()
        assertEquals(rice, added.ingredient)
        assertEquals(150.0, added.quantity, 0.0)
        assertFalse(added.isFromPhotoAnalysis)

        vm.updateIngredientQuantity(added, 80.0)
        val resized = vm.selectedIngredients.value.single()
        assertEquals(80.0, resized.quantity, 0.0)

        vm.removeIngredient(resized)
        assertTrue(vm.selectedIngredients.value.isEmpty())
    }

    @Test
    fun `submitNewMeal saves the meal with totals scaled by quantity`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setShowAddMealDialog(true)
        vm.setNewMealName(" Lunch ")
        vm.setNewMealComment("  ")
        vm.setNewMealTimestamp(1_000L)
        vm.addIngredient(rice, 150.0)

        vm.submitNewMeal()
        advanceUntilIdle()

        coVerify {
            mealRepository.insert(
                "u1",
                match {
                    it.name == "Lunch" &&
                        it.timestamp == 1_000L &&
                        it.userId == "u1" &&
                        it.comment == null &&
                        it.calories == 300 &&
                        it.carbs == 30.0 &&
                        it.protein == 15.0 &&
                        it.fat == 7.5 &&
                        it.sugar == 3.0 &&
                        it.salt == 1.5 &&
                        it.ingredients.single().ingredient == rice
                }
            )
        }
        assertFalse(vm.showAddMealDialog.value)
        assertEquals("", vm.newMealName.value)
        assertTrue(vm.selectedIngredients.value.isEmpty())
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `submitNewMeal without a name or ingredients does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.setNewMealName("Lunch")
        vm.submitNewMeal()
        vm.setNewMealName("  ")
        vm.addIngredient(rice, 100.0)
        vm.submitNewMeal()
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.insert(any(), any()) }
    }

    @Test
    fun `submitNewMeal keeps the form when saving fails`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { mealRepository.insert(any(), any()) } throws IllegalStateException("offline")
        val vm = buildViewModel()
        vm.setShowAddMealDialog(true)
        vm.setNewMealName("Lunch")
        vm.addIngredient(rice, 100.0)

        vm.submitNewMeal()
        advanceUntilIdle()

        assertTrue(vm.showAddMealDialog.value)
        assertEquals("Lunch", vm.newMealName.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `closing the add meal dialog resets the form`() {
        val vm = buildViewModel()
        vm.setShowAddMealDialog(true)
        vm.setNewMealName("Lunch")
        vm.setNewMealComment("tasty")
        vm.setSearchQuery("ric")
        vm.addIngredient(rice, 100.0)

        vm.setShowAddMealDialog(false)

        assertEquals("", vm.newMealName.value)
        assertEquals("", vm.newMealComment.value)
        assertEquals("", vm.searchQuery.value)
        assertTrue(vm.selectedIngredients.value.isEmpty())
    }

    @Test
    fun `deleteMeal deletes the meal and reloads daily nutrition`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.deleteMeal(meal)
        advanceUntilIdle()

        coVerify { mealRepository.delete("u1", meal) }
        coVerify(exactly = 2) { mealRepository.getDailyNutrition("u1", vm.selectedDate.value) }
    }

    @Test
    fun `createCustomIngredient saves it for the user and closes the dialog`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = buildViewModel()
            vm.setShowCreateIngredientDialog(true)

            vm.createCustomIngredient(rice)
            advanceUntilIdle()

            coVerify { mealRepository.insertIngredient(rice.copy(userId = "u1")) }
            assertFalse(vm.showCreateIngredientDialog.value)
        }

    @Test
    fun `deleteCustomIngredient delegates to the repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.deleteCustomIngredient(rice)
        advanceUntilIdle()

        coVerify { mealRepository.deleteIngredient(rice) }
    }

    @Test
    fun `analyzeMealPhoto exposes the analysis`() = runTest(mainDispatcherRule.dispatcher) {
        val analysis = FoodImageAnalysis(listOf("rice"), rice)
        coEvery { mealRepository.analyzeFoodImage(any()) } returns analysis
        val vm = buildViewModel()

        vm.analyzeMealPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals(analysis, vm.mealPhotoAnalysis.value)
        assertNull(vm.mealPhotoAnalysisError.value)
        assertFalse(vm.isAnalyzingMealPhoto.value)
    }

    @Test
    fun `analyzeMealPhoto failure exposes the error message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { mealRepository.analyzeFoodImage(any()) } throws FoodImageAnalysisException("No food found")
        val vm = buildViewModel()

        vm.analyzeMealPhoto(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        assertNull(vm.mealPhotoAnalysis.value)
        assertEquals("No food found", vm.mealPhotoAnalysisError.value)
        assertFalse(vm.isAnalyzingMealPhoto.value)
    }

    @Test
    fun `acceptMealPhotoAnalysis adds the ingredient with the corrected names`() =
        runTest(mainDispatcherRule.dispatcher) {
            coEvery { mealRepository.analyzeFoodImage(any()) } returns FoodImageAnalysis(listOf("rice"), rice)
            val vm = buildViewModel()
            vm.analyzeMealPhoto(byteArrayOf(1))
            advanceUntilIdle()

            vm.acceptMealPhotoAnalysis(listOf(" brown rice ", "", "chicken"))

            val added = vm.selectedIngredients.value.single()
            assertEquals(rice.copy(name = "brown rice, chicken"), added.ingredient)
            assertEquals(100.0, added.quantity, 0.0)
            assertTrue(added.isFromPhotoAnalysis)
            assertNull(vm.mealPhotoAnalysis.value)
        }

    @Test
    fun `acceptMealPhotoAnalysis falls back to the estimated name`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { mealRepository.analyzeFoodImage(any()) } returns FoodImageAnalysis(listOf("rice"), rice)
        val vm = buildViewModel()
        vm.analyzeMealPhoto(byteArrayOf(1))
        advanceUntilIdle()

        vm.acceptMealPhotoAnalysis(listOf(" ", ""))

        assertEquals("Rice", vm.selectedIngredients.value.single().ingredient.name)
    }

    @Test
    fun `reportMealPhotoError and dismiss update the error state`() {
        val vm = buildViewModel()

        vm.reportMealPhotoError("Camera unavailable")
        assertEquals("Camera unavailable", vm.mealPhotoAnalysisError.value)

        vm.dismissMealPhotoAnalysis()
        assertNull(vm.mealPhotoAnalysisError.value)
    }

    @Test
    fun `actions without a logged in user do nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = buildViewModel()
        vm.setNewMealName("Lunch")
        vm.addIngredient(rice, 100.0)

        vm.submitNewMeal()
        vm.deleteMeal(meal)
        vm.createCustomIngredient(rice)
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.insert(any(), any()) }
        coVerify(exactly = 0) { mealRepository.delete(any(), any()) }
        coVerify(exactly = 0) { mealRepository.insertIngredient(any()) }
    }
}
