package com.dj.insulink.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dj.insulink.wear.data.LatestReading
import com.dj.insulink.wear.data.LatestReadingStore
import com.dj.insulink.wear.data.WearDataListener
import com.google.android.gms.wearable.Wearable

// Combines the on-watch cached value (for an instant result on cold start) with live
// DataClient updates while this composable is in the composition.
@Composable
fun rememberLatestReadingState(): State<LatestReading?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<LatestReading?>(null) }

    LaunchedEffect(Unit) {
        state.value = LatestReadingStore.getCached(context)
    }

    DisposableEffect(Unit) {
        val listener = WearDataListener { latest -> state.value = latest }
        Wearable.getDataClient(context).addListener(listener)
        onDispose { Wearable.getDataClient(context).removeListener(listener) }
    }

    return state
}
