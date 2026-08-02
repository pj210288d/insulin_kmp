package com.dj.insulink.shared.feature.reminders.data.mapper

import com.dj.insulink.shared.core.time.currentLocalTimeOfDay
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderMapperTest {

    private val entity = ReminderEntity(
        id = 2, userId = "u1", title = "Take insulin",
        reminderType = ReminderType.INSULIN_REMINDER, isDoneForToday = false, time = 5000L
    )

    private fun expectedIsDoneForToday(time: Long): Boolean =
        localTimeOfDay(time) < currentLocalTimeOfDay()

    @Test
    fun entityMapsToDomainPreservingScalarFields() {
        val domain = entity.toDomain()
        assertEquals(entity.id, domain.id)
        assertEquals(entity.userId, domain.userId)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.reminderType, domain.reminderType)
        assertEquals(entity.time, domain.time)
    }

    @Test
    fun entityMapsToDomainComputingIsDoneForTodayFromTheTimeOfDay() {
        val domain = entity.toDomain()
        assertEquals(expectedIsDoneForToday(entity.time), domain.isDoneForToday)
    }

    @Test
    fun domainMapsToEntityPreservingTheExplicitIsDoneForTodayFlag() {
        val domain = Reminder(
            id = 2, userId = "u1", title = "Take insulin",
            reminderType = ReminderType.INSULIN_REMINDER, isDoneForToday = true, time = 5000L
        )
        assertEquals(entity.copy(isDoneForToday = true), domain.toEntity())
    }

    @Test
    fun listMappersMapEveryElement() {
        val entities = listOf(entity, entity.copy(id = 3, title = "Check sugar"))
        val domains = entities.toDomain()
        assertEquals(2, domains.size)
        assertEquals(listOf(2L, 3L), domains.map { it.id })
    }
}
