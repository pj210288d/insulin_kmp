package com.dj.insulink.feature.insulin.ui.viewmodel

import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.util.MainDispatcherRule
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
class InsulinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val insulinTypeRepository: InsulinTypeRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()

    private fun buildViewModel(
        insulinTypes: List<InsulinType> = emptyList(),
        userId: String? = "u1"
    ): InsulinViewModel {
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { insulinTypeRepository.getAllInsulinTypesForUser(any()) } returns flowOf(insulinTypes)
        return InsulinViewModel(insulinTypeRepository, authRepository)
    }

    @Test
    fun `setInsulinTypeName caps at thirty characters`() {
        val vm = buildViewModel()
        vm.setInsulinTypeName("NovoRapid")
        assertEquals("NovoRapid", vm.insulinTypeName.value)

        vm.setInsulinTypeName("a".repeat(40))
        assertEquals("NovoRapid", vm.insulinTypeName.value)
    }

    @Test
    fun `setShowAddInsulinTypeDialog updates state`() {
        val vm = buildViewModel()
        vm.setShowAddInsulinTypeDialog(true)
        assertEquals(true, vm.showAddInsulinTypeDialog.value)
    }

    @Test
    fun `addInsulinType inserts a new type and resets the form`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setInsulinTypeName("NovoRapid")

        vm.addInsulinType("u1")
        advanceUntilIdle()

        coVerify {
            insulinTypeRepository.insert("u1", match { it.name == "NovoRapid" && it.userId == "u1" })
        }
        assertEquals("", vm.insulinTypeName.value)
    }

    @Test
    fun `deleteInsulinType deletes for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val insulinType = InsulinType(5, "u1", "Lantus")

        vm.deleteInsulinType("u1", insulinType)
        advanceUntilIdle()

        coVerify { insulinTypeRepository.delete("u1", insulinType) }
    }

    @Test
    fun `deleteInsulinType with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val insulinType = InsulinType(5, "u1", "Lantus")

        vm.deleteInsulinType(null, insulinType)
        advanceUntilIdle()

        coVerify(exactly = 0) { insulinTypeRepository.delete(any(), any()) }
    }

    @Test
    fun `fetchInsulinTypesForUserAndUpdateDatabase delegates to repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.fetchInsulinTypesForUserAndUpdateDatabase("u1")
        advanceUntilIdle()
        coVerify { insulinTypeRepository.fetchAllInsulinTypesForUserAndUpdateDatabase("u1") }
    }
}
