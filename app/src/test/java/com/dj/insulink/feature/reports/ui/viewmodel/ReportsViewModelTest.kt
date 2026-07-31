package com.dj.insulink.feature.reports.ui.viewmodel

import android.content.Context
import com.dj.insulink.feature.dataREMOVE.pdf.GlucoseReportPdfGenerator
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.feature.settings.data.SettingsPreferences
import com.dj.insulink.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: GlucoseReadingRepository = mockk(relaxed = true)
    private val settingsPreferences: SettingsPreferences = mockk()
    private val context: Context = mockk()

    @Before
    fun setUp() {
        every { context.getString(any()) } returns "message"
        every { context.cacheDir } returns File("build/test-cache")
        every { settingsPreferences.getGlucoseUnit() } returns GlucoseUnit.MG_DL
    }

    @After
    fun tearDown() {
        unmockkConstructor(GlucoseReportPdfGenerator::class)
    }

    private fun buildViewModel(): ReportsViewModel =
        ReportsViewModel(repository, settingsPreferences, context)

    @Test
    fun `initializeDateRange loads the range and filtered readings`() = runTest(mainDispatcherRule.dispatcher) {
        val readings = listOf(
            GlucoseReading(2, "u1", 200L, 110, ""),
            GlucoseReading(1, "u1", 100L, 90, "")
        )
        coEvery { repository.getDateRange("u1") } returns Pair(100L, 200L)
        every { repository.getGlucoseReadingsByDateRange("u1", 100L, 200L) } returns flowOf(readings)
        val vm = buildViewModel()

        vm.initializeDateRange("u1")
        advanceUntilIdle()

        assertEquals(100L, vm.minDate.value)
        assertEquals(200L, vm.maxDate.value)
        assertEquals(100L, vm.selectedMinDate.value)
        assertEquals(200L, vm.selectedMaxDate.value)
        assertEquals(listOf(1L, 2L), vm.filteredReadings.value.map { it.id }) // sorted by timestamp
    }

    @Test
    fun `initializeDateRange with null range does not filter`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.getDateRange("u1") } returns Pair(null, null)
        val vm = buildViewModel()

        vm.initializeDateRange("u1")
        advanceUntilIdle()

        assertNull(vm.minDate.value)
        assertTrue(vm.filteredReadings.value.isEmpty())
    }

    @Test
    fun `updateDateRange updates only the selected range`() {
        val vm = buildViewModel()
        vm.updateDateRange(10L, 20L)
        assertEquals(10L, vm.selectedMinDate.value)
        assertEquals(20L, vm.selectedMaxDate.value)
    }

    @Test
    fun `filterReadingsByCurrentDateRange filters when a range is known`() = runTest(mainDispatcherRule.dispatcher) {
        val readings = listOf(GlucoseReading(1, "u1", 100L, 90, ""))
        coEvery { repository.getDateRange("u1") } returns Pair(100L, 200L)
        every { repository.getGlucoseReadingsByDateRange("u1", 100L, 200L) } returns flowOf(readings)
        val vm = buildViewModel()
        vm.initializeDateRange("u1")
        advanceUntilIdle()

        vm.filterReadingsByCurrentDateRange("u1")
        advanceUntilIdle()

        assertEquals(listOf(1L), vm.filteredReadings.value.map { it.id })
    }

    @Test
    fun `resetDateRange reloads the range from the repository`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { repository.getDateRange("u1") } returns Pair(50L, 80L)
        every { repository.getGlucoseReadingsByDateRange("u1", 50L, 80L) } returns flowOf(emptyList())
        val vm = buildViewModel()

        vm.resetDateRange("u1")
        advanceUntilIdle()

        assertEquals(50L, vm.minDate.value)
        assertEquals(80L, vm.maxDate.value)
    }

    @Test
    fun `generatePdfReport reports an error when there is no data`() {
        val vm = buildViewModel()
        // no selected dates and no readings
        vm.generatePdfReport("u1")

        assertTrue(vm.pdfGenerationState.value is PdfGenerationState.Error)
    }

    @Test
    fun `generatePdfReport produces a success state when generation succeeds`() = runTest(mainDispatcherRule.dispatcher) {
        mockkConstructor(GlucoseReportPdfGenerator::class)
        val outputFile = File("build/test-cache/report.pdf")
        coEvery {
            anyConstructed<GlucoseReportPdfGenerator>().generatePdf(any(), any(), any(), any(), any())
        } returns Result.success(outputFile)

        val vm = buildViewModel()
        vm.updateDateRange(100L, 200L)
        coEvery { repository.getDateRange("u1") } returns Pair(100L, 200L)
        every { repository.getGlucoseReadingsByDateRange(any(), any(), any()) } returns
            flowOf(listOf(GlucoseReading(1, "u1", 150L, 100, "")))
        vm.filterReadingsByDateRange("u1", 100L, 200L)
        advanceUntilIdle()

        vm.generatePdfReport("u1")
        advanceUntilIdle()

        assertEquals(PdfGenerationState.Success(outputFile), vm.pdfGenerationState.value)
        assertEquals(outputFile, vm.getCurrentPdfFile())
    }

    @Test
    fun `generatePdfReport produces an error state when generation fails`() = runTest(mainDispatcherRule.dispatcher) {
        mockkConstructor(GlucoseReportPdfGenerator::class)
        coEvery {
            anyConstructed<GlucoseReportPdfGenerator>().generatePdf(any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("disk full"))

        val vm = buildViewModel()
        vm.updateDateRange(100L, 200L)
        every { repository.getGlucoseReadingsByDateRange(any(), any(), any()) } returns
            flowOf(listOf(GlucoseReading(1, "u1", 150L, 100, "")))
        vm.filterReadingsByDateRange("u1", 100L, 200L)
        advanceUntilIdle()

        vm.generatePdfReport("u1")
        advanceUntilIdle()

        assertTrue(vm.pdfGenerationState.value is PdfGenerationState.Error)
    }

    @Test
    fun `resetPdfGenerationState returns to idle`() {
        val vm = buildViewModel()
        vm.generatePdfReport("u1") // -> Error (no data)
        vm.resetPdfGenerationState()

        assertEquals(PdfGenerationState.Idle, vm.pdfGenerationState.value)
        assertNull(vm.getCurrentPdfFile())
    }
}
