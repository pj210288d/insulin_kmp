package com.dj.insulink.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable

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
