package com.dj.insulink.feature.reports.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.reports.data.PdfReportGenerator
import com.dj.insulink.shared.feature.reports.data.PdfShareCoordinator
import com.dj.insulink.shared.feature.reports.ui.viewmodel.PdfGenerationState
import com.dj.insulink.shared.feature.reports.ui.viewmodel.ReportsViewModel
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val glucoseRepository: GlucoseReadingRepository = mockk(relaxed = true)
    private val settingsPreferences: SettingsPreferences = mockk(relaxed = true)
    private val pdfReportGenerator: PdfReportGenerator = mockk()
    private val pdfShareCoordinator: PdfShareCoordinator = mockk(relaxed = true)

    private val readings = listOf(
        GlucoseReading(id = 1, userId = "u1", timestamp = 150L, value = 120, comment = "")
    )

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        coEvery { glucoseRepository.getDateRange("u1") } returns (100L to 200L)
        every { glucoseRepository.getGlucoseReadingsByDateRange(any(), any(), any()) } returns flowOf(readings)
        every { settingsPreferences.getGlucoseUnit() } returns GlucoseUnit.MMOL_L
        coEvery { pdfReportGenerator.generate(any(), any(), any(), any()) } returns "/tmp/report.pdf"
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    private fun TestScope.buildViewModel(): ReportsViewModel {
        val vm = ReportsViewModel(glucoseRepository, settingsPreferences, pdfReportGenerator, pdfShareCoordinator)
        advanceUntilIdle()
        return vm
    }

    @Test
    fun `init loads the available date range and selects all of it`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        assertEquals(100L, vm.minDate.value)
        assertEquals(200L, vm.maxDate.value)
        assertEquals(100L, vm.selectedMinDate.value)
        assertEquals(200L, vm.selectedMaxDate.value)
        assertEquals(PdfGenerationState.Idle, vm.generationState.value)
    }

    @Test
    fun `init without a logged in user leaves the range empty`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)

        val vm = buildViewModel()

        assertNull(vm.minDate.value)
        assertNull(vm.selectedMaxDate.value)
        coVerify(exactly = 0) { glucoseRepository.getDateRange(any()) }
    }

    @Test
    fun `updateSelectedRange changes only the selected dates`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.updateSelectedRange(120L, 180L)

        assertEquals(120L, vm.selectedMinDate.value)
        assertEquals(180L, vm.selectedMaxDate.value)
        assertEquals(100L, vm.minDate.value)
        assertEquals(200L, vm.maxDate.value)
    }

    @Test
    fun `generateReport passes the selected range and unit to the generator`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = buildViewModel()
            vm.updateSelectedRange(120L, 180L)

            vm.generateReport()
            advanceUntilIdle()

            verify { glucoseRepository.getGlucoseReadingsByDateRange("u1", 120L, 180L) }
            coVerify { pdfReportGenerator.generate(readings, 120L, 180L, GlucoseUnit.MMOL_L) }
            assertEquals(PdfGenerationState.Success("/tmp/report.pdf"), vm.generationState.value)
        }

    @Test
    fun `generateReport without any readings reports an error`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { glucoseRepository.getDateRange("u1") } returns (null to null)
        val vm = buildViewModel()

        vm.generateReport()
        advanceUntilIdle()

        assertEquals(PdfGenerationState.Error("Nema očitavanja za izveštaj."), vm.generationState.value)
        coVerify(exactly = 0) { pdfReportGenerator.generate(any(), any(), any(), any()) }
    }

    @Test
    fun `generateReport surfaces the generator failure message`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery { pdfReportGenerator.generate(any(), any(), any(), any()) } throws IllegalStateException("disk full")
        val vm = buildViewModel()

        vm.generateReport()
        advanceUntilIdle()

        assertEquals(PdfGenerationState.Error("disk full"), vm.generationState.value)
    }

    @Test
    fun `shareReport shares the generated file`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.generateReport()
        advanceUntilIdle()

        vm.shareReport()

        verify { pdfShareCoordinator.share("/tmp/report.pdf") }
    }

    @Test
    fun `shareReport before a report exists does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.shareReport()

        verify(exactly = 0) { pdfShareCoordinator.share(any()) }
    }
}
