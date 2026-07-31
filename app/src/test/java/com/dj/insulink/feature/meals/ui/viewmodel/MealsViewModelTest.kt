package com.dj.insulink.feature.meals.ui.viewmodel

import app.cash.turbine.test
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.meals.domain.model.DailyNutrition
import com.dj.insulink.shared.feature.meals.domain.model.Ingredient
import com.dj.insulink.shared.feature.meals.domain.model.Meal
import com.dj.insulink.shared.feature.meals.domain.model.MealIngredient
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.awaitUntil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MealsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mealRepository: MealRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()

    private val ingredient = Ingredient(
        id = 1, name = "Oats", caloriesPer100g = 100.0, proteinPer100g = 10.0,
        carbsPer100g = 20.0, fatPer100g = 5.0, sugarPer100g = 2.0, saltPer100g = 1.0
    )

    private fun buildViewModel(
        userId: String? = "u1",
        meals: List<Meal> = emptyList(),
        ingredients: List<Ingredient> = emptyList()
    ): MealsViewModel {
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { mealRepository.getMealsByDateForUser(any(), any()) } returns flowOf(meals)
        every { mealRepository.getUserIngredients(any()) } returns flowOf(ingredients)
        every { mealRepository.searchIngredients(any(), any()) } returns flowOf(ingredients)
        coEvery { mealRepository.getDailyNutrition(any(), any()) } returns DailyNutrition(0, 0, 0, 0, 0, 0.0)
        return MealsViewModel(mealRepository, authRepository)
    }

    @Test
    fun `addIngredient appends a meal ingredient`() {
        val vm = buildViewModel()
        vm.addIngredient(ingredient, 150.0)

        assertEquals(1, vm.selectedIngredients.value.size)
        assertEquals(150.0, vm.selectedIngredients.value.first().quantity, 0.0)
        assertEquals(ingredient, vm.selectedIngredients.value.first().ingredient)
    }

    @Test
    fun `removeIngredient removes the matching entry`() {
        val vm = buildViewModel()
        vm.addIngredient(ingredient, 150.0)
        val added = vm.selectedIngredients.value.first()

        vm.removeIngredient(added)

        assertTrue(vm.selectedIngredients.value.isEmpty())
    }

    @Test
    fun `updateIngredientQuantity changes only the matching entry`() {
        val vm = buildViewModel()
        vm.addIngredient(ingredient, 150.0)
        val added = vm.selectedIngredients.value.first()

        vm.updateIngredientQuantity(added, 250.0)

        assertEquals(250.0, vm.selectedIngredients.value.first().quantity, 0.0)
    }

    @Test
    fun `meal field setters update state`() {
        val vm = buildViewModel()
        vm.setNewMealName("Lunch")
        vm.setNewMealComment("tasty")
        vm.setNewMealTimestamp(999L)
        vm.setSearchQuery("oat")
        vm.setShowCreateIngredientDialog(true)
        vm.setShowMyIngredientsDialog(true)

        assertEquals("Lunch", vm.newMealName.value)
        assertEquals("tasty", vm.newMealComment.value)
        assertEquals(999L, vm.newMealTimestamp.value)
        assertEquals("oat", vm.searchQuery.value)
        assertTrue(vm.showCreateIngredientDialog.value)
        assertTrue(vm.showMyIngredientsDialog.value)
    }

    @Test
    fun `prepareNewMeal clears the form`() {
        val vm = buildViewModel()
        vm.setNewMealName("Lunch")
        vm.addIngredient(ingredient, 100.0)

        vm.prepareNewMeal()

        assertEquals("", vm.newMealName.value)
        assertTrue(vm.selectedIngredients.value.isEmpty())
    }

    @Test
    fun `setSelectedDate updates state and loads nutrition`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        coEvery { mealRepository.getDailyNutrition("u1", 500L) } returns DailyNutrition(300, 40, 20, 10, 5, 1.0)

        vm.setSelectedDate(500L)
        advanceUntilIdle()

        assertEquals(500L, vm.selectedDate.value)
        assertEquals(300, vm.dailyNutrition.value.calories)
    }

    @Test
    fun `submitNewMeal computes totals inserts and calls onSuccess`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setNewMealName("Breakfast")
        vm.addIngredient(ingredient, 200.0) // quantity 200g of a 100 kcal/100g ingredient
        var success = false

        vm.submitNewMeal("u1") { success = true }
        advanceUntilIdle()

        coVerify {
            mealRepository.insert(
                "u1",
                match {
                    it.name == "Breakfast" && it.calories == 200 && it.carbs == 40.0 &&
                        it.protein == 20.0 && it.fat == 10.0 && it.sugar == 4.0 && it.salt == 2.0 &&
                        it.comment == null
                }
            )
        }
        assertTrue(success)
        assertTrue(vm.selectedIngredients.value.isEmpty()) // reset
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun `submitNewMeal with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.addIngredient(ingredient, 100.0)
        var success = false

        vm.submitNewMeal(null) { success = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.insert(any(), any()) }
        assertFalse(success)
    }

    @Test
    fun `submitNewMeal with no ingredients does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        var success = false

        vm.submitNewMeal("u1") { success = true }
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.insert(any(), any()) }
        assertFalse(success)
    }

    @Test
    fun `deleteMeal delegates for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val meal = Meal(
            id = 1, name = "Lunch", timestamp = 1L, calories = 1, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1"
        )

        vm.deleteMeal("u1", meal)
        advanceUntilIdle()

        coVerify { mealRepository.delete("u1", meal) }
    }

    @Test
    fun `deleteMeal with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val meal = Meal(
            id = 1, name = "Lunch", timestamp = 1L, calories = 1, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1"
        )

        vm.deleteMeal(null, meal)
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.delete(any(), any()) }
    }

    @Test
    fun `createCustomIngredient saves with the user id and closes the dialog`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setShowCreateIngredientDialog(true)

        vm.createCustomIngredient("u1", ingredient)
        advanceUntilIdle()

        coVerify { mealRepository.insertIngredient(match { it.userId == "u1" }) }
        assertFalse(vm.showCreateIngredientDialog.value)
    }

    @Test
    fun `createCustomIngredient with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.createCustomIngredient(null, ingredient)
        advanceUntilIdle()

        coVerify(exactly = 0) { mealRepository.insertIngredient(any()) }
    }

    @Test
    fun `deleteCustomIngredient delegates to repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.deleteCustomIngredient(ingredient)
        advanceUntilIdle()

        coVerify { mealRepository.deleteIngredient(ingredient) }
    }

    @Test
    fun `fetchAllMealsForUserAndUpdateDatabase delegates only for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.fetchAllMealsForUserAndUpdateDatabase("u1")
        advanceUntilIdle()
        coVerify { mealRepository.fetchAllMealsForUserAndUpdateDatabase("u1") }

        vm.fetchAllMealsForUserAndUpdateDatabase(null)
        advanceUntilIdle()
        coVerify(exactly = 1) { mealRepository.fetchAllMealsForUserAndUpdateDatabase(any()) }
    }

    @Test
    fun `searchResults is empty for a blank query`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.searchResults.test {
            assertEquals(emptyList<Ingredient>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `searchResults emits repository matches after debounce`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(ingredients = listOf(ingredient))

        vm.searchResults.test {
            assertEquals(emptyList<Ingredient>(), awaitItem()) // initial
            vm.setSearchQuery("oats")
            advanceTimeBy(301)
            runCurrent()
            val results = awaitUntil { it.isNotEmpty() }
            assertEquals(listOf(ingredient), results)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `userIngredients surfaces repository data`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(ingredients = listOf(ingredient))

        vm.userIngredients.test {
            val items = awaitUntil { it.isNotEmpty() }
            assertEquals(listOf(ingredient), items)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mealsForSelectedDate surfaces repository data`() = runTest(mainDispatcherRule.dispatcher) {
        val meal = Meal(
            id = 1, name = "Lunch", timestamp = 1L, calories = 1, carbs = 1.0, protein = 1.0,
            fat = 1.0, sugar = 1.0, salt = 1.0, comment = null, userId = "u1"
        )
        val vm = buildViewModel(meals = listOf(meal))

        vm.mealsForSelectedDate.test {
            val items = awaitUntil { it.isNotEmpty() }
            assertEquals(listOf(meal), items)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
