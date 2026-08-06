package com.dj.insulink.shared.feature.reminders.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderTypeTest {

    @Test
    fun fromName_returnsMatchingTypeForValidNames() {
        assertEquals(ReminderType.MEAL_REMINDER, ReminderType.fromName("MEAL_REMINDER"))
        assertEquals(ReminderType.INSULIN_REMINDER, ReminderType.fromName("INSULIN_REMINDER"))
        assertEquals(ReminderType.BLOOD_SUGAR_CHECK_REMINDER, ReminderType.fromName("BLOOD_SUGAR_CHECK_REMINDER"))
    }

    @Test
    fun fromName_returnsNullForUnknownName() {
        assertNull(ReminderType.fromName("NOT_A_TYPE"))
    }

    @Test
    fun fromName_returnsNullForNullInput() {
        assertNull(ReminderType.fromName(null))
    }
}
