package com.dj.insulink.shared.feature.reminders.domain.model

data class Reminder(
    val id: Long,
    val userId: String,
    val title: String,
    val reminderType: ReminderType,
    val isDoneForToday: Boolean,
    val time: Long
)

// Pure identity enum, no Android resource references (@StringRes/@DrawableRes) - those
// live in the app module's UI layer, mapped from this type there. commonMain can't
// reference app-module generated resource IDs.
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
