package com.dj.insulink.wear.data

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

// Small local substitute for kotlinx-coroutines-play-services' Task.await(), to avoid pulling
// in a whole extra dependency for a handful of call sites. Mirrors the identical helper in the
// phone's WearSyncManager.kt.
internal suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
