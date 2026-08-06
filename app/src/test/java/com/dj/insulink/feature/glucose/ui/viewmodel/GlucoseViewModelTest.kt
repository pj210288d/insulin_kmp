package com.dj.insulink.feature.glucose.ui.viewmodel

import app.cash.turbine.test
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.wear.WearSyncManager
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.meals.data.repository.MealRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.awaitUntil
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlucoseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: GlucoseReadingRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val settingsPreferences: SettingsPreferences = mockk()
    private val insulinTypeRepository: InsulinTypeRepository = mockk(relaxed = true)
    private val mealRepository: MealRepository = mockk(relaxed = true)
    private val wearSyncManager: WearSyncManager = mockk(relaxed = true)

    private fun buildViewModel(
        unit: GlucoseUnit = GlucoseUnit.MG_DL,
        readings: List<GlucoseReading> = emptyList(),
        userId: String? = "u1"
    ): GlucoseViewModel {
        every { settingsPreferences.getGlucoseUnit() } returns unit
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { repository.getAllGlucoseReadingsForUser(any()) } returns flowOf(readings)
        return GlucoseViewModel(
            repository,
            authRepository,
            settingsPreferences,
            insulinTypeRepository,
            mealRepository,
            wearSyncManager
        )
    }

    @Test
    fun `setters update state and comment is capped at twenty characters`() {
        val vm = buildViewModel()
        vm.setNewGlucoseReadingValue("123")
        vm.setNewGlucoseReadingTimestamp(5000L)
        vm.setShowAddGlucoseReadingDialog(true)
        vm.setSelectedTimespan(GlucoseReadingTimespan.LAST_WEEK)

        assertEquals("123", vm.newGlucoseReadingValue.value)
        assertEquals(5000L, vm.newGlucoseReadingTimestamp.value)
        assertEquals(true, vm.showAddGlucoseReadingDialog.value)
        assertEquals(GlucoseReadingTimespan.LAST_WEEK, vm.selectedTimespan.value)

        vm.setNewGlucoseReadingComment("short comment")
        assertEquals("short comment", vm.newGlucoseReadingComment.value)

        vm.setNewGlucoseReadingComment("this comment is definitely longer than twenty characters")
        assertEquals("short comment", vm.newGlucoseReadingComment.value) // unchanged
    }

    @Test
    fun `refreshGlucoseUnit re-reads from preferences`() {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        every { settingsPreferences.getGlucoseUnit() } returns GlucoseUnit.MMOL_L
        vm.refreshGlucoseUnit()
        assertEquals(GlucoseUnit.MMOL_L, vm.glucoseUnit.value)
    }

    @Test
    fun `submitNewGlucoseReading stores mg dL value directly`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        vm.setNewGlucoseReadingTimestamp(1234L)
        vm.setNewGlucoseReadingValue("120")
        vm.setNewGlucoseReadingComment("ok")

        vm.submitNewGlucoseReading("u1")
        advanceUntilIdle()

        coVerify {
            repository.insert(
                "u1",
                match { it.value == 120 && it.timestamp == 1234L && it.comment == "ok" && it.userId == "u1" }
            )
        }
        assertEquals("", vm.newGlucoseReadingValue.value) // reset
    }

    @Test
    fun `submitNewGlucoseReading converts mmol per L to mg dL`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MMOL_L)
        vm.setNewGlucoseReadingValue("6.0") // 6.0 * 18.0182 = 108

        vm.submitNewGlucoseReading("u1")
        advanceUntilIdle()

        coVerify { repository.insert("u1", match { it.value == 108 }) }
    }

    @Test
    fun `submitNewGlucoseReading ignores non numeric value`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setNewGlucoseReadingValue("")

        vm.submitNewGlucoseReading("u1")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any(), any()) }
    }

    @Test
    fun `submitNewGlucoseReading with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setNewGlucoseReadingValue("120")

        vm.submitNewGlucoseReading(null)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.insert(any(), any()) }
    }

    @Test
    fun `deleteGlucoseReading delegates to repository for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val reading = GlucoseReading(1, "u1", 1000L, 100, "")

        vm.deleteGlucoseReading("u1", reading)
        advanceUntilIdle()

        coVerify { repository.delete("u1", reading) }
    }

    @Test
    fun `deleteGlucoseReading with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val reading = GlucoseReading(1, "u1", 1000L, 100, "")

        vm.deleteGlucoseReading(null, reading)
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.delete(any(), any()) }
    }

    @Test
    fun `fetchAll delegates only for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.fetchAllGlucoseReadingsForUserAndUpdateDatabase("u1")
        advanceUntilIdle()
        coVerify { repository.fetchAllGlucoseReadingsForUserAndUpdateDatabase("u1") }

        vm.fetchAllGlucoseReadingsForUserAndUpdateDatabase(null)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.fetchAllGlucoseReadingsForUserAndUpdateDatabase(any()) }
    }

    @Test
    fun `allGlucoseReadings returns all readings for the ALL_READINGS timespan`() = runTest(mainDispatcherRule.dispatcher) {
        val now = System.currentTimeMillis()
        val readings = listOf(
            GlucoseReading(1, "u1", now, 100, ""),
            GlucoseReading(2, "u1", now - 10L * 24 * 60 * 60 * 1000, 90, "")
        )
        val vm = buildViewModel(readings = readings)

        vm.allGlucoseReadings.test {
            val items = awaitUntil { it.isNotEmpty() }
            assertEquals(2, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `allGlucoseReadings filters out readings older than the selected timespan`() = runTest(mainDispatcherRule.dispatcher) {
        val now = System.currentTimeMillis()
        val recent = GlucoseReading(1, "u1", now, 100, "")
        val old = GlucoseReading(2, "u1", now - 10L * 24 * 60 * 60 * 1000, 90, "")
        val vm = buildViewModel(readings = listOf(recent, old))
        vm.setSelectedTimespan(GlucoseReadingTimespan.LAST_DAY)

        vm.allGlucoseReadings.test {
            val items = awaitUntil { it.isNotEmpty() }
            assertEquals(listOf(1L), items.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `latestGlucoseReading reflects the first reading`() = runTest(mainDispatcherRule.dispatcher) {
        val now = System.currentTimeMillis()
        val readings = listOf(GlucoseReading(1, "u1", now, 100, ""))
        val vm = buildViewModel(readings = readings)

        vm.latestGlucoseReading.test {
            val item = awaitUntil { it != null }
            assertEquals(1L, item!!.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startEditingGlucoseReading pre-fills fields including insulin and meal link`() {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        val reading = GlucoseReading(
            id = 7,
            userId = "u1",
            timestamp = 5000L,
            value = 130,
            comment = "after lunch",
            insulinTypeId = 3,
            insulinUnits = 4.5,
            linkedMealId = 9
        )

        vm.startEditingGlucoseReading(reading)

        assertEquals(7L, vm.editingReadingId.value)
        assertEquals(5000L, vm.newGlucoseReadingTimestamp.value)
        assertEquals("130", vm.newGlucoseReadingValue.value)
        assertEquals("after lunch", vm.newGlucoseReadingComment.value)
        assertEquals(3L, vm.newGlucoseReadingInsulinTypeId.value)
        assertEquals("4.5", vm.newGlucoseReadingInsulinUnits.value)
        assertEquals(9L, vm.newGlucoseReadingLinkedMealId.value)
        assertEquals(true, vm.showAddGlucoseReadingDialog.value)
    }

    @Test
    fun `startAddGlucoseReading resets fields and clears edit mode`() {
        val vm = buildViewModel()
        vm.startEditingGlucoseReading(GlucoseReading(7, "u1", 5000L, 130, "x", 3, 4.5, 9))

        vm.startAddGlucoseReading()

        assertNull(vm.editingReadingId.value)
        assertEquals("", vm.newGlucoseReadingValue.value)
        assertEquals("", vm.newGlucoseReadingComment.value)
        assertNull(vm.newGlucoseReadingInsulinTypeId.value)
        assertEquals("", vm.newGlucoseReadingInsulinUnits.value)
        assertNull(vm.newGlucoseReadingLinkedMealId.value)
        assertEquals(true, vm.showAddGlucoseReadingDialog.value)
    }

    @Test
    fun `submitNewGlucoseReading in edit mode updates instead of inserting`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        vm.startEditingGlucoseReading(GlucoseReading(7, "u1", 5000L, 100, ""))
        vm.setNewGlucoseReadingValue("140")
        vm.setNewGlucoseReadingInsulinTypeId(3)
        vm.setNewGlucoseReadingInsulinUnits("5")
        vm.setNewGlucoseReadingLinkedMealId(9)

        vm.submitNewGlucoseReading("u1")
        advanceUntilIdle()

        coVerify {
            repository.update(
                "u1",
                match {
                    it.id == 7L && it.value == 140 && it.insulinTypeId == 3L &&
                        it.insulinUnits == 5.0 && it.linkedMealId == 9L
                }
            )
        }
        coVerify(exactly = 0) { repository.insert(any(), any()) }
        assertNull(vm.editingReadingId.value)
    }

    @Test
    fun `submitNewGlucoseReading in add mode includes insulin and meal fields`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel(unit = GlucoseUnit.MG_DL)
        vm.setNewGlucoseReadingValue("120")
        vm.setNewGlucoseReadingInsulinTypeId(3)
        vm.setNewGlucoseReadingInsulinUnits("5")
        vm.setNewGlucoseReadingLinkedMealId(9)

        vm.submitNewGlucoseReading("u1")
        advanceUntilIdle()

        coVerify {
            repository.insert(
                "u1",
                match { it.insulinTypeId == 3L && it.insulinUnits == 5.0 && it.linkedMealId == 9L }
            )
        }
    }
}
