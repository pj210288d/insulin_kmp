package com.dj.insulink.wear.data

import android.content.Context
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer

class WearMessageSender(private val context: Context) {

    suspend fun sendQuickAddGlucose(value: Int, timestampMillis: Long = System.currentTimeMillis()) {
        val nodes = Wearable.getNodeClient(context).connectedNodes.awaitResult()
        val payload = ByteBuffer.allocate(Long.SIZE_BYTES + Int.SIZE_BYTES)
            .putLong(timestampMillis)
            .putInt(value)
            .array()

        for (node in nodes) {
            Wearable.getMessageClient(context)
                .sendMessage(node.id, WearDataLayerContract.QUICK_ADD_GLUCOSE_PATH, payload)
                .awaitResult()
        }
    }
}
