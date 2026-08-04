package com.dj.insulink.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.google.firebase.auth.FirebaseAuth
import org.koin.core.context.GlobalContext

// Reads its dependency straight from Koin (the same GlobalContext lookup SharedModule.kt
// uses to bridge :shared repositories into Hilt) instead of going through Hilt's own
// WorkManager integration — avoids adding a HiltWorkerFactory/Configuration.Provider setup
// for what is otherwise a single, simple background job.
class LibreLinkSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.success()
        val libreLinkRepository: LibreLinkRepository = GlobalContext.get().get()

        val syncResult = libreLinkRepository.syncLatestReadings(userId)
        return if (syncResult.isSuccess) Result.success() else Result.retry()
    }
}
