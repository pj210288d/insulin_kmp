package com.dj.insulink.wear.data

// Data Layer path/field names shared conceptually with the phone's WearSyncManager
// (app/.../core/wear/WearSyncManager.kt). The two modules don't share Kotlin code, so these
// must be kept in sync by hand whenever either side changes.
object WearDataLayerContract {
    const val LATEST_READING_PATH = "/insulink/latest_reading"
    const val KEY_HAS_READING = "has_reading"
    const val KEY_VALUE = "value"
    const val KEY_FORMATTED_VALUE = "formatted_value"
    const val KEY_RANGE_STATUS = "range_status"
    const val KEY_TIMESTAMP = "timestamp"

    const val RANGE_LOW = "LOW"
    const val RANGE_NORMAL = "NORMAL"
    const val RANGE_HIGH = "HIGH"

    const val QUICK_ADD_GLUCOSE_PATH = "/insulink/quick_add_glucose"
    const val MESSAGE_KEY_VALUE = "value"
    const val MESSAGE_KEY_TIMESTAMP = "timestamp"
}
