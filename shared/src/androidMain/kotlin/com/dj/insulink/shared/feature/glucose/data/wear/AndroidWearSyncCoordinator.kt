package com.dj.insulink.shared.feature.glucose.data.wear

import android.content.Context
import android.util.Log
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.glucose.domain.model.HIGH_GLUCOSE_THRESHOLD
import com.dj.insulink.shared.feature.glucose.domain.model.LOWER_GLUCOSE_THRESHOLD
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidWearSyncCoordinator(
    private val context: Context
) : WearSyncCoordinator {

    override suspend fun pushLatestReading(reading: GlucoseReading?, unit: GlucoseUnit) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.awaitResult()
            Log.d(TAG, "Connected nodes: ${nodes.map { "${it.displayName} (${it.id}, nearby=${it.isNearby})" }}")

            val dataMapRequest = PutDataMapRequest.create(LATEST_READING_PATH)
            dataMapRequest.dataMap.apply {
                putBoolean(KEY_HAS_READING, reading != null)
                putInt(KEY_VALUE, reading?.value ?: 0)
                putString(
                    KEY_FORMATTED_VALUE,
                    reading?.let { "${unit.formatValue(it.value)} ${unit.suffix}" } ?: ""
                )
                putString(KEY_RANGE_STATUS, reading?.let { rangeStatusFor(it.value) } ?: "")
                putLong(KEY_TIMESTAMP, reading?.timestamp ?: 0L)
                putString(KEY_UNIT, unit.key)
            }

            val putDataRequest = dataMapRequest.asPutDataRequest().setUrgent()
            val result = Wearable.getDataClient(context).putDataItem(putDataRequest).awaitResult()
            Log.d(TAG, "Pushed latest reading DataItem: ${result.uri}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push latest reading to Wear OS watch", e)
        }
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
        const val KEY_UNIT = "unit"
        const val RANGE_LOW = "LOW"
        const val RANGE_NORMAL = "NORMAL"
        const val RANGE_HIGH = "HIGH"
        private const val TAG = "WearSyncCoordinator"
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
