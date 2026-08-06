package com.dj.insulink.feature.fitness.ui.viewmodel

import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.fitness.data.repository.ExerciseRepository
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.awaitUntil
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exerciseRepository: ExerciseRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val settingsPreferences: SettingsPreferences = mockk()

    private fun buildViewModel(
        unit: GlucoseUnit = GlucoseUnit.MG_DL,
        exercises: List<Exercise> = emptyList(),
        userId: String? = "u1"
    ): FitnessViewModel {
        every { settingsPreferences.getGlucoseUnit() } returns unit
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { exerciseRepository.getAllExercisesForUser(any()) } returns flowOf(exercises)
        return FitnessViewModel(exerciseRepository, authRepository, settingsPreferences)
    }

    @Test
    fun `setters update their state`() {
        val vm = buildViewModel()
        vm.setActivityName("Running")
        vm.setDurationHours("1")
        vm.setDurationMinutes("30")
        vm.setGlucoseBefore("150")
        vm.setGlucoseAfter("110")
        vm.setShowSportsActivityDialog(true)

        assertEquals("Running", vm.activityName.value)
        assertEquals("1", vm.durationHours.value)
        assertEquals("30", vm.durationMinutes.value)
        assertEquals("150", vm.glucoseBefore.value)
        assertEquals("110", vm.glucoseAfter.value)
        assertEquals(true, vm.showAddSportsActivityDialog.value)
    }

    @Test
    fun `refreshGlucoseUnit re-reads from preferences`() {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        assertEquals(GlucoseUnit.MG_DL, vm.glucoseUnit.value)

        every { settingsPreferences.getGlucoseUnit() } returns GlucoseUnit.MMOL_L
        vm.refreshGlucoseUnit()

        assertEquals(GlucoseUnit.MMOL_L, vm.glucoseUnit.value)
    }

    @Test
    fun `onAddExerciseClick stores mg dL values directly and resets fields`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        vm.setActivityName("Running")
        vm.setDurationHours("1")
        vm.setDurationMinutes("30")
        vm.setGlucoseBefore("150")
        vm.setGlucoseAfter("110")

        vm.onAddExerciseClick("u1")
        advanceUntilIdle()

        coVerify {
            exerciseRepository.insert(
                "u1",
                match {
                    it.sportName == "Running" && it.durationHours == 1 && it.durationMinutes == 30 &&
                        it.glucoseBefore == 150 && it.glucoseAfter == 110
                }
            )
        }
        assertEquals("", vm.activityName.value)
        assertEquals("", vm.durationHours.value)
        assertEquals("", vm.glucoseBefore.value)
    }

    @Test
    fun `onAddExerciseClick converts mmol per L values to mg dL before storing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MMOL_L)
        vm.setActivityName("Cycling")
        vm.setGlucoseBefore("6.0") // 6.0 * 18.0182 = 108.1 -> 108
        vm.setGlucoseAfter("5.0")  // 5.0 * 18.0182 = 90.1 -> 90

        vm.onAddExerciseClick("u1")
        advanceUntilIdle()

        coVerify {
            exerciseRepository.insert(
                "u1",
                match { it.glucoseBefore == 108 && it.glucoseAfter == 90 }
            )
        }
    }

    @Test
    fun `onAddExerciseClick with null user does not insert but still resets fields`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setActivityName("Running")

        vm.onAddExerciseClick(null)
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.insert(any(), any()) }
        assertEquals("", vm.activityName.value)
    }

    @Test
    fun `onAddExerciseClick defaults blank numeric fields to zero`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        vm.setActivityName("Walking")
        // durations and glucose left blank

        vm.onAddExerciseClick("u1")
        advanceUntilIdle()

        coVerify {
            exerciseRepository.insert(
                "u1",
                match {
                    it.durationHours == 0 && it.durationMinutes == 0 &&
                        it.glucoseBefore == 0 && it.glucoseAfter == 0
                }
            )
        }
    }

    @Test
    fun `fetchAllExercisesForUserAndUpdateDatabase delegates to repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.fetchAllExercisesForUserAndUpdateDatabase("u1")
        advanceUntilIdle()

        coVerify { exerciseRepository.fetchAllExercisesForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `calculatedSports groups by sport averages drops and sorts by frequency`() = runTest(mainDispatcherRule.dispatcher) {
        val exercises = listOf(
            Exercise(1, "u1", "Running", 1, 0, 150, 110),   // drop 40 / 1h = 40
            Exercise(2, "u1", "Running", 0, 30, 120, 100),  // drop 20 / 0.5h = 40
            Exercise(3, "u1", "Cycling", 0, 0, 100, 100),   // totalHours 0 -> 0.0
            Exercise(4, "u1", "", 1, 0, 100, 90)            // blank -> filtered out
        )
        val vm = buildViewModel(exercises = exercises)

        vm.calculatedSports.test {
            val sports = awaitUntil { it.isNotEmpty() }
            assertEquals(listOf("Running", "Cycling"), sports.map { it.name })
            val running = sports.first { it.name == "Running" }
            assertEquals(40, running.avgDropPerHour)
            assertEquals(40, running.lastDropPerHour)
            val cycling = sports.first { it.name == "Cycling" }
            assertEquals(0, cycling.avgDropPerHour)
            assertEquals(0, cycling.lastDropPerHour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `allExercisesForUser is empty when there is no logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(userId = null)

        vm.allExercisesForUser.test {
            assertEquals(emptyList<Exercise>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
