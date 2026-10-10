package com.dj.insulink.feature.reminders.ui.viewmodel

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.reminders.data.notification.ReminderNotificationScheduler
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.shared.feature.reminders.ui.viewmodel.RemindersViewModel
import com.dj.insulink.util.MainDispatcherRule
import com.dj.insulink.util.collectInBackground
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class RemindersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reminderRepository: ReminderRepository = mockk(relaxed = true)
    private val notificationScheduler: ReminderNotificationScheduler = mockk(relaxed = true)

    private val reminder = Reminder(
        id = 7,
        userId = "u1",
        title = "Lunch",
        reminderType = ReminderType.MEAL_REMINDER,
        isDoneForToday = false,
        time = 1_000L
    )

    @Before
    fun setUp() {
        UserSession.setCurrentUserId("u1")
        every { reminderRepository.getAllRemindersForUser(any()) } returns flowOf(emptyList())
        coEvery { reminderRepository.insert(any(), any()) } returns 42L
    }

    @After
    fun tearDown() {
        UserSession.setCurrentUserId(null)
    }

    private fun buildViewModel() = RemindersViewModel(reminderRepository, notificationScheduler)

    private fun timeToday(hour: Int, minute: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun `init fetches reminders for the logged in user`() = runTest(mainDispatcherRule.dispatcher) {
        buildViewModel()
        advanceUntilIdle()

        coVerify { reminderRepository.fetchAllRemindersForUserAndUpdateDatabase("u1") }
    }

    @Test
    fun `reminders exposes the repository list for the user`() = runTest(mainDispatcherRule.dispatcher) {
        every { reminderRepository.getAllRemindersForUser("u1") } returns flowOf(listOf(reminder))

        val vm = buildViewModel()
        collectInBackground(vm.reminders)
        advanceUntilIdle()

        assertEquals(listOf(reminder), vm.reminders.value)
    }

    @Test
    fun `setNewTitle ignores titles longer than twenty characters`() {
        val vm = buildViewModel()

        vm.setNewTitle("Measure sugar")
        assertEquals("Measure sugar", vm.newTitle.value)

        vm.setNewTitle("a".repeat(21))
        assertEquals("Measure sugar", vm.newTitle.value)
    }

    @Test
    fun `addReminder inserts the reminder and schedules a daily notification`() =
        runTest(mainDispatcherRule.dispatcher) {
            val time = timeToday(hour = 8, minute = 30)
            val vm = buildViewModel()
            vm.setNewTitle(" Insulin ")
            vm.setNewType(ReminderType.INSULIN_REMINDER)
            vm.setNewTime(time)

            vm.addReminder()
            advanceUntilIdle()

            coVerify {
                reminderRepository.insert(
                    "u1",
                    Reminder(
                        id = 0,
                        userId = "u1",
                        title = "Insulin",
                        reminderType = ReminderType.INSULIN_REMINDER,
                        isDoneForToday = false,
                        time = time
                    )
                )
            }
            coVerify {
                notificationScheduler.scheduleDaily(
                    reminderId = 42L,
                    title = "Insulin",
                    message = "Vreme je za insulin",
                    hour = 8,
                    minute = 30
                )
            }
            assertEquals("", vm.newTitle.value)
        }

    @Test
    fun `addReminder with a blank title does nothing`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()
        vm.setNewTitle("   ")

        vm.addReminder()
        advanceUntilIdle()

        coVerify(exactly = 0) { reminderRepository.insert(any(), any()) }
        coVerify(exactly = 0) { notificationScheduler.scheduleDaily(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `addReminder still saves the reminder when scheduling fails`() = runTest(mainDispatcherRule.dispatcher) {
        coEvery {
            notificationScheduler.scheduleDaily(any(), any(), any(), any(), any())
        } throws IllegalStateException("no permission")
        val vm = buildViewModel()
        vm.setNewTitle("Lunch")

        vm.addReminder()
        advanceUntilIdle()

        coVerify { reminderRepository.insert("u1", any()) }
    }

    @Test
    fun `toggleDoneForToday flips the done flag`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = buildViewModel()

        vm.toggleDoneForToday(reminder)
        advanceUntilIdle()

        coVerify { reminderRepository.setDoneForToday("u1", reminder, true) }
    }

    @Test
    fun `deleteReminder cancels the notification and deletes the reminder`() =
        runTest(mainDispatcherRule.dispatcher) {
            val vm = buildViewModel()

            vm.deleteReminder(reminder)
            advanceUntilIdle()

            verify { notificationScheduler.cancelReminder(7) }
            coVerify { reminderRepository.delete("u1", reminder) }
        }

    @Test
    fun `actions without a logged in user do nothing`() = runTest(mainDispatcherRule.dispatcher) {
        UserSession.setCurrentUserId(null)
        val vm = buildViewModel()
        vm.setNewTitle("Lunch")

        vm.addReminder()
        vm.toggleDoneForToday(reminder)
        vm.deleteReminder(reminder)
        advanceUntilIdle()

        coVerify(exactly = 0) { reminderRepository.insert(any(), any()) }
        coVerify(exactly = 0) { reminderRepository.setDoneForToday(any(), any(), any()) }
        coVerify(exactly = 0) { reminderRepository.delete(any(), any()) }
        verify(exactly = 0) { notificationScheduler.cancelReminder(any()) }
    }
}
