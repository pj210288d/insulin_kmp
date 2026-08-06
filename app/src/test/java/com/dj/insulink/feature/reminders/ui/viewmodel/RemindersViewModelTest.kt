package com.dj.insulink.feature.reminders.ui.viewmodel

import android.content.Context
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.notification.ReminderScheduler
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk()
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val context: Context = mockk()

    private fun buildViewModel(
        reminders: List<Reminder> = emptyList(),
        userId: String? = "u1"
    ): RemindersViewModel {
        every { authRepository.getCurrentUserFlow() } returns flowOf(userId)
        every { reminderRepository.getAllRemindersForUser(any()) } returns flowOf(reminders)
        every { context.getString(any()) } returns "Reminder message"
        return RemindersViewModel(reminderRepository, authRepository, reminderScheduler, context)
    }

    @Test
    fun `setReminderTitle caps at twenty characters`() {
        val vm = buildViewModel()
        vm.setReminderTitle("short")
        assertEquals("short", vm.reminderTitle.value)

        vm.setReminderTitle("this title is far longer than twenty chars")
        assertEquals("short", vm.reminderTitle.value)
    }

    @Test
    fun `setters update state`() {
        val vm = buildViewModel()
        vm.setReminderType(ReminderType.INSULIN_REMINDER)
        vm.setReminderTime(4242L)
        vm.setShowAddReminderDialog(true)

        assertEquals(ReminderType.INSULIN_REMINDER, vm.reminderType.value)
        assertEquals(4242L, vm.reminderTime.value)
        assertEquals(true, vm.showAddReminderDialog.value)
    }

    @Test
    fun `addReminder inserts and schedules a daily notification then resets fields`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setReminderTitle("Lunch")
        vm.setReminderType(ReminderType.MEAL_REMINDER)
        vm.setReminderTime(0L)
        coEvery { reminderRepository.insert(any(), any()) } returns 123L

        vm.addReminder("u1")
        advanceUntilIdle()

        coVerify {
            reminderRepository.insert("u1", match { it.title == "Lunch" && it.userId == "u1" })
        }
        verify {
            reminderScheduler.scheduleDaily(
                reminderId = 123L,
                title = "Lunch",
                message = "Reminder message",
                hour = any(),
                minute = any()
            )
        }
        assertEquals("", vm.reminderTitle.value)
        assertEquals(ReminderType.MEAL_REMINDER, vm.reminderType.value)
    }

    @Test
    fun `deleteReminder cancels the schedule and deletes for a logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val reminder = Reminder(5, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)

        vm.deleteReminder("u1", reminder)
        advanceUntilIdle()

        verify { reminderScheduler.cancelReminder(5) }
        coVerify { reminderRepository.delete("u1", reminder) }
    }

    @Test
    fun `deleteReminder with null user does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        val reminder = Reminder(5, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)

        vm.deleteReminder(null, reminder)
        advanceUntilIdle()

        verify(exactly = 0) { reminderScheduler.cancelReminder(any()) }
        coVerify(exactly = 0) { reminderRepository.delete(any(), any()) }
    }

    @Test
    fun `fetchReminderDataAndUpdateDatabase delegates to repository`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.fetchReminderDataAndUpdateDatabase("u1")
        advanceUntilIdle()
        coVerify { reminderRepository.fetchAllRemindersForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `resetAddReminderFields clears the form`() {
        val vm = buildViewModel()
        vm.setReminderTitle("Lunch")
        vm.setReminderType(ReminderType.INSULIN_REMINDER)

        vm.resetAddReminderFields()

        assertEquals("", vm.reminderTitle.value)
        assertEquals(ReminderType.MEAL_REMINDER, vm.reminderType.value)
    }
}
