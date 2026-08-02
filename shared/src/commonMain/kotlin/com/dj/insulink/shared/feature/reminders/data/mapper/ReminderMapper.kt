package com.dj.insulink.shared.feature.reminders.data.mapper

import com.dj.insulink.shared.core.time.currentLocalTimeOfDay
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.local.entity.ReminderEntity
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder

fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = id,
        userId = userId,
        title = title,
        reminderType = reminderType,
        isDoneForToday = localTimeOfDay(time) < currentLocalTimeOfDay(),
        time = time
    )
}

fun Reminder.toEntity(): ReminderEntity {
    return ReminderEntity(
        id = id,
        userId = userId,
        title = title,
        reminderType = reminderType,
        isDoneForToday = isDoneForToday,
        time = time
    )
}

fun List<ReminderEntity>.toDomain(): List<Reminder> {
    return map { it.toDomain() }
}

fun List<Reminder>.toEntity(): List<ReminderEntity> {
    return map { it.toEntity() }
}
