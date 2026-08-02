package com.dj.insulink.shared.feature.reminders.data.repository

import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ReminderRepositoryTest {

    private val dao = FakeReminderDao()
    private val remote = FakeReminderRemoteDataSource()
    private val repository = ReminderRepository(dao, remote)

    @Test
    fun getAllRemindersForUser_mapsAndSortsByTimeOfDay() = runTest {
        val later = ReminderEntity(1, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 43_200_000L)
        val earlier = ReminderEntity(2, "u1", "Insulin", ReminderType.INSULIN_REMINDER, false, 36_000_000L)
        dao.allRemindersFlow = flowOf(listOf(later, earlier))

        val result = repository.getAllRemindersForUser("u1").first()

        assertEquals(2, result.size)
        val times = result.map { localTimeOfDay(it.time) }
        assertEquals(times.sorted(), times) // ascending by time of day
    }

    @Test
    fun insert_assignsGeneratedIdForANewReminderAndReturnsIt() = runTest {
        val reminder = Reminder(0, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)

        val returnedId = repository.insert("u1", reminder)

        assertTrue(returnedId != 0L)
        assertEquals(returnedId, dao.insertedReminders.single().id)
    }

    @Test
    fun insert_keepsAnExistingId() = runTest {
        val reminder = Reminder(5, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)

        val returnedId = repository.insert("u1", reminder)

        assertEquals(5L, returnedId)
        assertEquals(5L, dao.insertedReminders.single().id)
    }

    @Test
    fun delete_removesTheReminderLocally() = runTest {
        val reminder = Reminder(5, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)

        repository.delete("u1", reminder)

        assertEquals(5L, dao.deletedReminders.single().id)
    }

    @Test
    fun fetchAllRemindersForUserAndUpdateDatabase_replacesLocalCache() = runTest {
        remote.fetchAllResult = listOf(Reminder(1, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L))

        repository.fetchAllRemindersForUserAndUpdateDatabase("u1")

        assertEquals("u1", dao.deleteAllForUserCalledWith)
        assertEquals(
            listOf(ReminderEntity(1, "u1", "Lunch", ReminderType.MEAL_REMINDER, false, 1000L)),
            dao.insertAllCalledWith
        )
    }
}
