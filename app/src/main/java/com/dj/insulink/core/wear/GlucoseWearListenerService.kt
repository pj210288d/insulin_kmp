package com.dj.insulink.core.wear

import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.domain.model.GlucoseReading
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import java.nio.ByteBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// Handles a quick-add glucose reading sent from the watch (wearApp's WearMessageSender).
// Not Hilt-managed - reads its dependencies straight from Koin, same as LibreLinkSyncWorker.kt,
// since WearableListenerService's own lifecycle leaves no clean place to wire Hilt injection.
class GlucoseWearListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != QUICK_ADD_GLUCOSE_PATH) return

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val (timestamp, value) = parsePayload(messageEvent.data) ?: return

        val glucoseReadingRepository: GlucoseReadingRepository = GlobalContext.get().get()
        val settingsPreferences: SettingsPreferences = GlobalContext.get().get()
        val wearSyncManager = WearSyncManager(applicationContext)

        serviceScope.launch {
            val reading = GlucoseReading(
                id = 0,
                userId = userId,
                timestamp = timestamp,
                value = value,
                comment = ""
            )
            glucoseReadingRepository.insert(userId = userId, reading = reading)

            // Echo the true latest reading back to the watch, so its "latest reading" screen
            // and tile reflect the confirmed, saved value (not just what the watch guessed).
            val latest = glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
                .first()
                .maxByOrNull { it.timestamp }
            wearSyncManager.pushLatestReading(latest, settingsPreferences.getGlucoseUnit())
        }
    }

    private fun parsePayload(data: ByteArray): Pair<Long, Int>? {
        if (data.size != Long.SIZE_BYTES + Int.SIZE_BYTES) return null
        val buffer = ByteBuffer.wrap(data)
        val timestamp = buffer.long
        val value = buffer.int
        return timestamp to value
    }

    companion object {
        // Must match wearApp's WearDataLayerContract.QUICK_ADD_GLUCOSE_PATH.
        private const val QUICK_ADD_GLUCOSE_PATH = "/insulink/quick_add_glucose"
    }
}
