package com.dj.insulink.shared.feature.reminders.domain.model

data class Reminder(
    val id: Long,
    val userId: String,
    val title: String,
    val reminderType: ReminderType,
    val isDoneForToday: Boolean,
    val time: Long
)

enum class ReminderType {
    MEAL_REMINDER,
    INSULIN_REMINDER,
    BLOOD_SUGAR_CHECK_REMINDER;

    companion object {
        fun fromName(name: String?): ReminderType? {
            return entries.find { it.name == name }
        }
    }
}

fun ReminderType.notificationMessage(): String = when (this) {
    ReminderType.MEAL_REMINDER -> "Vreme je za obrok"
    ReminderType.INSULIN_REMINDER -> "Vreme je za insulin"
    ReminderType.BLOOD_SUGAR_CHECK_REMINDER -> "Vreme je da izmeriš šećer"
}
