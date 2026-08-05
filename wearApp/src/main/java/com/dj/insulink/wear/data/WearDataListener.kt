package com.dj.insulink.wear.data

import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer

// Live updates while the watch app is in the foreground. LatestReadingStore.getCached() covers
// the cold-start case (app just opened, phone hasn't pushed anything since) - the two together
// give the "latest reading" screen a value immediately and keep it fresh afterwards.
class WearDataListener(
    private val onLatestReadingChanged: (LatestReading?) -> Unit = {}
) : DataClient.OnDataChangedListener {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            if (event.dataItem.uri.path != WearDataLayerContract.LATEST_READING_PATH) continue

            val latest = event.dataItem.toLatestReadingOrNull()
            Log.d(TAG, "Latest reading changed: $latest")
            onLatestReadingChanged(latest)
        }
        dataEvents.release()
    }

    companion object {
        private const val TAG = "WearDataListener"
    }
}

data class LatestReading(
    val value: Int,
    val formattedValue: String,
    val rangeStatus: String,
    val timestampMillis: Long,
    val unit: String
)
