package com.dj.insulink.feature.insulin.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import com.dj.insulink.shared.feature.insulin.ui.viewmodel.InsulinViewModel
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
class InsulinViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val insulinTypeRepository: InsulinTypeRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        every { insulinTypeRepository.getAllInsulinTypesForUser(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    @Test
    fun `init fetches insulin types for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        InsulinViewModel(insulinTypeRepository)
        advanceUntilIdle()

        coVerify { insulinTypeRepository.fetchAllInsulinTypesForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `init does not fetch without a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)

        InsulinViewModel(insulinTypeRepository)
        advanceUntilIdle()

        coVerify(exactly = 0) { insulinTypeRepository.fetchAllInsulinTypesForUserAndUpdateDatabase(any()) }
    }

    @Test
    fun `insulinTypes exposes the repository list for the user`() = runTest(mainDispatcherRule.dispatcher) {
        val types = listOf(InsulinType(1, "u1", "NovoRapid"), InsulinType(2, "u1", "Lantus"))
        every { insulinTypeRepository.getAllInsulinTypesForUser("u1") } returns flowOf(types)

        val vm = InsulinViewModel(insulinTypeRepository)
        collectInBackground(vm.insulinTypes)
        advanceUntilIdle()

        assertEquals(types, vm.insulinTypes.value)
    }

    @Test
    fun `addInsulinType inserts the trimmed name and resets the field`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = InsulinViewModel(insulinTypeRepository)
        vm.setNewTypeName("  NovoRapid ")

        vm.addInsulinType()
        advanceUntilIdle()

        coVerify {
            insulinTypeRepository.insert("u1", InsulinType(id = 0, userId = "u1", name = "NovoRapid"))
        }
        assertEquals("", vm.newTypeName.value)
    }

    @Test
    fun `addInsulinType with a blank name does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = InsulinViewModel(insulinTypeRepository)
        vm.setNewTypeName("   ")

        vm.addInsulinType()
        advanceUntilIdle()

        coVerify(exactly = 0) { insulinTypeRepository.insert(any(), any()) }
    }

    @Test
    fun `addInsulinType without a logged in user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = InsulinViewModel(insulinTypeRepository)
        vm.setNewTypeName("NovoRapid")

        vm.addInsulinType()
        advanceUntilIdle()

        coVerify(exactly = 0) { insulinTypeRepository.insert(any(), any()) }
        assertEquals("NovoRapid", vm.newTypeName.value)
    }

    @Test
    fun `deleteInsulinType deletes for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = InsulinViewModel(insulinTypeRepository)
        val insulinType = InsulinType(5, "u1", "Lantus")

        vm.deleteInsulinType(insulinType)
        advanceUntilIdle()

        coVerify { insulinTypeRepository.delete("u1", insulinType) }
    }

    @Test
    fun `deleteInsulinType without a logged in user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = InsulinViewModel(insulinTypeRepository)

        vm.deleteInsulinType(InsulinType(5, "u1", "Lantus"))
        advanceUntilIdle()

        coVerify(exactly = 0) { insulinTypeRepository.delete(any(), any()) }
    }
}
