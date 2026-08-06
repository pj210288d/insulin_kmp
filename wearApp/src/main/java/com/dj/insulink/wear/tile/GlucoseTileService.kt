package com.dj.insulink.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.dj.insulink.wear.MainActivity
import com.dj.insulink.wear.R
import com.dj.insulink.wear.data.LatestReading
import com.dj.insulink.wear.data.LatestReadingStore
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.future

// Shows the latest glucose reading as a glanceable tile. Reads straight from the on-watch
// Data Layer cache (LatestReadingStore) - the same source the "latest reading" screen uses -
// so it works even if the watch app hasn't been opened since the phone last pushed a value.
// Tapping the tile opens MainActivity (which lands on the "latest reading" screen, with the
// "Add" button leading to quick add - a dedicated tile-only quick-add action is left for later).
class GlucoseTileService : TileService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onTileRequest(
        requestParams: RequestBuilders.TileRequest
    ): ListenableFuture<TileBuilders.Tile> = serviceScope.future { buildTile() }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ): ListenableFuture<ResourceBuilders.Resources> = serviceScope.future {
        ResourceBuilders.Resources.Builder().setVersion(RESOURCES_VERSION).build()
    }

    private suspend fun buildTile(): TileBuilders.Tile {
        val latest = LatestReadingStore.getCached(applicationContext)
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(buildLayout(latest))
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun buildLayout(latest: LatestReading?): LayoutElementBuilders.LayoutElement {
        val valueText = latest?.formattedValue ?: getString(R.string.no_reading_yet)

        val openAppClickable = ModifiersBuilders.Clickable.Builder()
            .setId(OPEN_APP_CLICK_ID)
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(MainActivity::class.java.name)
                            .build()
                    )
                    .build()
            )
            .build()

        return LayoutElementBuilders.Box.Builder()
            .setModifiers(
                ModifiersBuilders.Modifiers.Builder()
                    .setClickable(openAppClickable)
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Text.Builder()
                    .setText(valueText)
                    .build()
            )
            .build()
    }

    companion object {
        private const val RESOURCES_VERSION = "1"
        private const val OPEN_APP_CLICK_ID = "open_app"
    }
}
