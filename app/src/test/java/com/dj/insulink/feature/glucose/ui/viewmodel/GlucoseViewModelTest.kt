package com.dj.insulink.feature.glucose.ui.viewmodel

import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.shiftedDayStartMillis
import com.dj.insulink.shared.core.time.startOfDayMillis
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.data.wear.WearSyncCoordinator
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.glucose.ui.viewmodel.GlucoseViewModel
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.collectInBackground
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
class GlucoseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val glucoseReadingRepository: GlucoseReadingRepository = mockk(relaxed = true)
    private val settingsPreferences: SettingsPreferences = mockk(relaxed = true)
    private val insulinTypeRepository: InsulinTypeRepository = mockk(relaxed = true)
    private val mealRepository: MealRepository = mockk(relaxed = true)
    private val wearSyncCoordinator: WearSyncCoordinator = mockk(relaxed = true)

    private val olderReading = GlucoseReading(id = 1, userId = "u1", timestamp = 1_000L, value = 110, comment = "")
    private val newerReading = GlucoseReading(
        id = 2,
        userId = "u1",
        timestamp = 2_000L,
        value = 108,
        comment = "after lunch",
        insulinTypeId = 5,
        insulinUnits = 4.5,
        linkedMealId = 9
    )

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MG_DL)
        every { glucoseReadingRepository.getAllGlucoseReadingsForUser(any()) } returns
            flowOf(listOf(newerReading, olderReading))
        every { glucoseReadingRepository.getGlucoseReadingsByDateRange(any(), any(), any()) } returns
            flowOf(emptyList())
        every { insulinTypeRepository.getAllInsulinTypesForUser(any()) } returns flowOf(emptyList())
        every { mealRepository.getMealsByDateForUser(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MG_DL)
    }

    private fun buildViewModel() = GlucoseViewModel(
        glucoseReadingRepository,
        settingsPreferences,
        insulinTypeRepository,
        mealRepository,
        wearSyncCoordinator
    )

    @Test
    fun `init fetches readings for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        buildViewModel()
        advanceUntilIdle()

        coVerify { glucoseReadingRepository.fetchAllGlucoseReadingsForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `exposes the latest reading and the insulin types of the user`() = runTest(mainDispatcherRule.dispatcher) {
        val insulinTypes = listOf(InsulinType(5, "u1", "NovoRapid"))
        every { insulinTypeRepository.getAllInsulinTypesForUser("u1") } returns flowOf(insulinTypes)

        val vm = buildViewModel()
        collectInBackground(vm.latestGlucoseReading, vm.allInsulinTypesForUser)
        advanceUntilIdle()

        assertEquals(newerReading, vm.latestGlucoseReading.value)
        assertEquals(insulinTypes, vm.allInsulinTypesForUser.value)
    }

    @Test
    fun `readings for the selected day are queried for that whole day`() = runTest(mainDispatcherRule.dispatcher) {
        every { glucoseReadingRepository.getGlucoseReadingsByDateRange(any(), any(), any()) } returns
            flowOf(listOf(olderReading))

        val vm = buildViewModel()
        collectInBackground(vm.glucoseReadingsForSelectedDay)
        advanceUntilIdle()

        val dayStart = vm.selectedDayStartMillis.value
        verify {
            glucoseReadingRepository.getGlucoseReadingsByDateRange(
                "u1",
                dayStart,
                shiftedDayStartMillis(dayStart, 1) - 1
            )
        }
        assertEquals(listOf(olderReading), vm.glucoseReadingsForSelectedDay.value)
    }

    @Test
    fun `day navigation moves back and forward but never past today`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        collectInBackground(vm.canGoToNextDay)
        val today = vm.selectedDayStartMillis.value
        advanceUntilIdle()
        assertFalse(vm.canGoToNextDay.value)

        vm.goToNextDay()
        assertEquals(today, vm.selectedDayStartMillis.value)

        vm.goToPreviousDay()
        advanceUntilIdle()
        assertEquals(shiftedDayStartMillis(today, -1), vm.selectedDayStartMillis.value)
        assertTrue(vm.canGoToNextDay.value)

        vm.goToNextDay()
        assertEquals(today, vm.selectedDayStartMillis.value)
    }

    @Test
    fun `setNewComment ignores comments longer than twenty characters`() {
        val vm = buildViewModel()

        vm.setNewComment("before breakfast")
        vm.setNewComment("a".repeat(21))

        assertEquals("before breakfast", vm.newComment.value)
    }

    @Test
    fun `setNewInsulinUnits keeps digits and the decimal point`() {
        val vm = buildViewModel()

        vm.setNewInsulinUnits("4,5u.5")

        assertEquals("45.5", vm.newInsulinUnits.value)
    }

    @Test
    fun `startAddReading opens an empty dialog`() {
        val vm = buildViewModel()
        vm.startEditReading(newerReading)

        vm.startAddReading()

        assertTrue(vm.showAddDialog.value)
        assertNull(vm.editingReading.value)
        assertEquals("", vm.newValue.value)
        assertEquals("", vm.newComment.value)
        assertNull(vm.newInsulinTypeId.value)
        assertEquals("", vm.newInsulinUnits.value)
        assertNull(vm.newLinkedMealId.value)
    }

    @Test
    fun `startEditReading fills the dialog from the reading`() {
        val vm = buildViewModel()

        vm.startEditReading(newerReading)

        assertTrue(vm.showAddDialog.value)
        assertEquals(newerReading, vm.editingReading.value)
        assertEquals(2_000L, vm.newTimestamp.value)
        assertEquals("108", vm.newValue.value)
        assertEquals("after lunch", vm.newComment.value)
        assertEquals(5L, vm.newInsulinTypeId.value)
        assertEquals("4.5", vm.newInsulinUnits.value)
        assertEquals(9L, vm.newLinkedMealId.value)
    }

    @Test
    fun `startEditReading shows the value in the selected unit`() {
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MMOL_L)
        val vm = buildViewModel()

        vm.startEditReading(newerReading)

        assertEquals(GlucoseUnit.MMOL_L.formatValue(108), vm.newValue.value)
    }

    @Test
    fun `dismissDialog hides the dialog`() {
        val vm = buildViewModel()
        vm.startAddReading()

        vm.dismissDialog()

        assertFalse(vm.showAddDialog.value)
    }

    @Test
    fun `submitReading inserts a new reading and closes the dialog`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.startAddReading()
        vm.setNewTimestamp(3_000L)
        vm.setNewValue("125")
        vm.setNewComment("fasting")
        vm.setNewInsulinTypeId(5)
        vm.setNewInsulinUnits("6")
        vm.setNewLinkedMealId(9)

        vm.submitReading()
        advanceUntilIdle()

        coVerify {
            glucoseReadingRepository.insert(
                "u1",
                GlucoseReading(
                    id = 0,
                    userId = "u1",
                    timestamp = 3_000L,
                    value = 125,
                    comment = "fasting",
                    insulinTypeId = 5,
                    insulinUnits = 6.0,
                    linkedMealId = 9
                )
            )
        }
        assertFalse(vm.showAddDialog.value)
    }

    @Test
    fun `submitReading stores mmol per litre values as mg per dl`() = runTest(mainDispatcherRule.dispatcher) {
        SettingsSession.setCurrentGlucoseUnit(GlucoseUnit.MMOL_L)
        val vm = buildViewModel()
        vm.startAddReading()
        vm.setNewValue("6.0")

        vm.submitReading()
        advanceUntilIdle()

        val expected = GlucoseUnit.convertMmolLToMgDl(6.0).toInt()
        assertEquals(108, expected)
        coVerify { glucoseReadingRepository.insert("u1", match { it.value == expected }) }
    }

    @Test
    fun `submitReading updates the reading being edited`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.startEditReading(newerReading)
        vm.setNewValue("130")

        vm.submitReading()
        advanceUntilIdle()

        coVerify { glucoseReadingRepository.update("u1", newerReading.copy(value = 130)) }
        coVerify(exactly = 0) { glucoseReadingRepository.insert(any(), any()) }
    }

    @Test
    fun `submitReading with an invalid value keeps the dialog open`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.startAddReading()
        vm.setNewValue("abc")

        vm.submitReading()
        advanceUntilIdle()

        coVerify(exactly = 0) { glucoseReadingRepository.insert(any(), any()) }
        assertTrue(vm.showAddDialog.value)
    }

    @Test
    fun `submitReading sends the most recent reading to the watch`() = runTest(mainDispatcherRule.dispatcher) {
        every { glucoseReadingRepository.getAllGlucoseReadingsForUser("u1") } returns
            flowOf(listOf(olderReading, newerReading))
        val vm = buildViewModel()
        vm.startAddReading()
        vm.setNewValue("125")

        vm.submitReading()
        advanceUntilIdle()

        coVerify { wearSyncCoordinator.pushLatestReading(newerReading, GlucoseUnit.MG_DL) }
    }

    @Test
    fun `deleteReading deletes it and refreshes the watch`() = runTest(mainDispatcherRule.dispatcher) {
        every { glucoseReadingRepository.getAllGlucoseReadingsForUser("u1") } returns flowOf(listOf(olderReading))
        val vm = buildViewModel()

        vm.deleteReading(newerReading)
        advanceUntilIdle()

        coVerify { glucoseReadingRepository.delete("u1", newerReading) }
        coVerify { wearSyncCoordinator.pushLatestReading(olderReading, GlucoseUnit.MG_DL) }
    }

    @Test
    fun `actions without a logged in user do nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = buildViewModel()
        vm.startAddReading()
        vm.setNewValue("125")

        vm.submitReading()
        vm.deleteReading(newerReading)
        advanceUntilIdle()

        coVerify(exactly = 0) { glucoseReadingRepository.insert(any(), any()) }
        coVerify(exactly = 0) { glucoseReadingRepository.delete(any(), any()) }
        coVerify(exactly = 0) { wearSyncCoordinator.pushLatestReading(any(), any()) }
    }

    @Test
    fun `selected day starts at the beginning of today`() {
        val vm = buildViewModel()

        assertEquals(startOfDayMillis(System.currentTimeMillis()), vm.selectedDayStartMillis.value)
    }
}
