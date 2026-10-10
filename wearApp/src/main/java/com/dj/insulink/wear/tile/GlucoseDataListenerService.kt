package com.dj.insulink.wear.tile

import androidx.wear.tiles.TileService
import com.dj.insulink.wear.data.WearDataLayerContract
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

class GlucoseDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        val hasLatestReadingUpdate = dataEvents.any {
            it.type == DataEvent.TYPE_CHANGED &&
                it.dataItem.uri.path == WearDataLayerContract.LATEST_READING_PATH
        }
        dataEvents.release()

        if (hasLatestReadingUpdate) {
            TileService.getUpdater(applicationContext).requestUpdate(GlucoseTileService::class.java)
        }
    }
}
