package com.dj.insulink.feature.fitness.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.fitness.data.repository.ExerciseRepository
import com.dj.insulink.shared.feature.fitness.domain.model.Exercise
import com.dj.insulink.shared.feature.fitness.ui.viewmodel.FitnessViewModel
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.collectInBackground
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FitnessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val exerciseRepository: ExerciseRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        every { exerciseRepository.getAllExercisesForUser(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    private fun FitnessViewModel.fillForm(
        name: String = "Running",
        minutes: String = "90",
        before: String = "140",
        after: String = "110"
    ) {
        setSportName(name)
        setDurationMinutes(minutes)
        setGlucoseBefore(before)
        setGlucoseAfter(after)
    }

    @Test
    fun `init fetches exercises for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        FitnessViewModel(exerciseRepository)
        advanceUntilIdle()

        coVerify { exerciseRepository.fetchAllExercisesForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `exercises exposes the repository list for the user`() = runTest(mainDispatcherRule.dispatcher) {
        val exercises = listOf(Exercise(1, "u1", "Running", 1, 30, 140, 110))
        every { exerciseRepository.getAllExercisesForUser("u1") } returns flowOf(exercises)

        val vm = FitnessViewModel(exerciseRepository)
        collectInBackground(vm.exercises)
        advanceUntilIdle()

        assertEquals(exercises, vm.exercises.value)
    }

    @Test
    fun `numeric fields keep digits only`() {
        val vm = FitnessViewModel(exerciseRepository)

        vm.setDurationMinutes("4a5 ")
        vm.setGlucoseBefore("1.40")
        vm.setGlucoseAfter("-110")

        assertEquals("45", vm.durationMinutes.value)
        assertEquals("140", vm.glucoseBefore.value)
        assertEquals("110", vm.glucoseAfter.value)
    }

    @Test
    fun `addExercise splits minutes into hours and minutes and resets the form`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = FitnessViewModel(exerciseRepository)
            vm.fillForm(name = " Running ", minutes = "90")

            vm.addExercise()
            advanceUntilIdle()

            coVerify {
                exerciseRepository.insert(
                    "u1",
                    Exercise(
                        id = 0,
                        userId = "u1",
                        sportName = "Running",
                        durationHours = 1,
                        durationMinutes = 30,
                        glucoseBefore = 140,
                        glucoseAfter = 110
                    )
                )
            }
            assertEquals("", vm.sportName.value)
            assertEquals("", vm.durationMinutes.value)
            assertEquals("", vm.glucoseBefore.value)
            assertEquals("", vm.glucoseAfter.value)
        }

    @Test
    fun `addExercise with a missing field does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = FitnessViewModel(exerciseRepository)
        vm.fillForm(after = "")

        vm.addExercise()
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.insert(any(), any()) }
        assertEquals("Running", vm.sportName.value)
    }

    @Test
    fun `addExercise with a blank sport name does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = FitnessViewModel(exerciseRepository)
        vm.fillForm(name = "  ")

        vm.addExercise()
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.insert(any(), any()) }
    }

    @Test
    fun `addExercise without a logged in user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = FitnessViewModel(exerciseRepository)
        vm.fillForm()

        vm.addExercise()
        advanceUntilIdle()

        coVerify(exactly = 0) { exerciseRepository.insert(any(), any()) }
    }
}
