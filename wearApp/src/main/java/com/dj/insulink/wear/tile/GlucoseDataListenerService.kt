package com.dj.insulink.wear.tile

import androidx.wear.tiles.TileService
import com.dj.insulink.wear.data.WearDataLayerContract
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService

// Runs even when the watch app isn't open, so the tile refreshes promptly whenever the phone
// pushes a new latest reading (manual add, LibreLinkUp sync, or a watch quick-add echo) rather
// than waiting for the platform's own periodic tile refresh.
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
