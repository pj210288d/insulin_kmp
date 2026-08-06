package com.dj.insulink.shared.feature.reminders.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType

@Entity("reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val userId: String,
    val title: String,
    val reminderType: ReminderType,
    val isDoneForToday: Boolean,
    val time: Long
)
