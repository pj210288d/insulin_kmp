package com.dj.insulink.core.wear

import android.content.Context
import com.dj.insulink.feature.glucose.ui.HIGH_GLUCOSE_THRESHOLD
import com.dj.insulink.feature.glucose.ui.LOWER_GLUCOSE_THRESHOLD
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// Pushes the current user's latest glucose reading to the paired Wear OS watch over the
// Wearable Data Layer API. This is a phone-only, Android-only concern (com.google.android.gms
// isn't available in :shared), so it lives here rather than in the KMP repository layer.
//
// Called from three places once a reading changes for the currently signed-in user:
//   1. GlucoseViewModel.submitNewGlucoseReading() - manual add/edit on the phone
//   2. LibreLinkSyncWorker - after a successful background LibreLinkUp sync
//   3. GlucoseWearListenerService - after inserting a quick-add reading sent by the watch
//
// The DataItem path/field names below must stay in sync with wearApp's WearDataListener,
// which has no shared Kotlin code with :app to enforce this at compile time.
@Singleton
class WearSyncManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun pushLatestReading(reading: GlucoseReading?, glucoseUnit: GlucoseUnit) {
        val dataMapRequest = PutDataMapRequest.create(LATEST_READING_PATH)
        dataMapRequest.dataMap.apply {
            putBoolean(KEY_HAS_READING, reading != null)
            putInt(KEY_VALUE, reading?.value ?: 0)
            putString(
                KEY_FORMATTED_VALUE,
                reading?.let { "${glucoseUnit.formatValue(it.value)} ${glucoseUnit.suffix}" } ?: ""
            )
            putString(KEY_RANGE_STATUS, reading?.let { rangeStatusFor(it.value) } ?: "")
            putLong(KEY_TIMESTAMP, reading?.timestamp ?: 0L)
        }

        val putDataRequest = dataMapRequest.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(putDataRequest).awaitResult()
    }

    private fun rangeStatusFor(value: Int): String = when {
        value < LOWER_GLUCOSE_THRESHOLD -> RANGE_LOW
        value <= HIGH_GLUCOSE_THRESHOLD -> RANGE_NORMAL
        else -> RANGE_HIGH
    }

    companion object {
        const val LATEST_READING_PATH = "/insulink/latest_reading"
        const val KEY_HAS_READING = "has_reading"
        const val KEY_VALUE = "value"
        const val KEY_FORMATTED_VALUE = "formatted_value"
        const val KEY_RANGE_STATUS = "range_status"
        const val KEY_TIMESTAMP = "timestamp"
        const val RANGE_LOW = "LOW"
        const val RANGE_NORMAL = "NORMAL"
        const val RANGE_HIGH = "HIGH"
    }
}

// Small local substitute for kotlinx-coroutines-play-services' Task.await(), to avoid
// pulling in a whole extra dependency for this one call site.
private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
