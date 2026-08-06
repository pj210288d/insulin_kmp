package com.dj.insulink.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable

// Reads whatever the phone last pushed straight from the on-watch Data Layer cache. Works even
// when the phone is unreachable right now - DataClient persists the last synced DataItem
// locally on the watch. Used for cold start (before any live WearDataListener update arrives)
// and by GlucoseTileService, which runs in its own process and can't share in-memory state
// with the Activity.
object LatestReadingStore {

    suspend fun getCached(context: Context): LatestReading? {
        val dataItems = Wearable.getDataClient(context).dataItems.awaitResult()
        try {
            for (item in dataItems) {
                if (item.uri.path == WearDataLayerContract.LATEST_READING_PATH) {
                    return item.toLatestReadingOrNull()
                }
            }
            return null
        } finally {
            dataItems.release()
        }
    }
}
