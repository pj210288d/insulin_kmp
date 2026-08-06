package com.dj.insulink.feature.reminders.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dj.insulink.R
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType

// ReminderType lives in :shared (commonMain), so it can't hold Android resource IDs directly.
// This maps it to the app module's string/drawable resources for UI display.
@get:StringRes
val ReminderType.displayNameRes: Int
    get() = when (this) {
        ReminderType.MEAL_REMINDER -> R.string.reminder_type_meal
        ReminderType.INSULIN_REMINDER -> R.string.reminder_type_insulin
        ReminderType.BLOOD_SUGAR_CHECK_REMINDER -> R.string.reminder_type_blood_sugar
    }

@get:DrawableRes
val ReminderType.icon: Int
    get() = when (this) {
        ReminderType.MEAL_REMINDER -> R.drawable.ic_utensils
        ReminderType.INSULIN_REMINDER -> R.drawable.ic_syringe
        ReminderType.BLOOD_SUGAR_CHECK_REMINDER -> R.drawable.ic_blood
    }
