package com.dj.insulink.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Android's PeriodicWorkRequest cannot go below a 15-minute interval regardless of the
// value passed in — that's an OS-level floor, not a WorkManager limitation.
class LibreLinkSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun enqueue() {
        val request = PeriodicWorkRequest.Builder(
            LibreLinkSyncWorker::class.java,
            SYNC_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            // PeriodicWorkRequest otherwise runs an immediate first execution on enqueue,
            // which races with the direct syncLatestReadings() call in
            // LibreLinkViewModel.connect() and double-inserts the same readings.
            .setInitialDelay(SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "librelink_sync"
        private const val SYNC_INTERVAL_MINUTES = 15L
    }
}
