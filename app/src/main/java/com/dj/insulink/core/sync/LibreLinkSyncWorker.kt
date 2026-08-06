package com.dj.insulink.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.dj.insulink.core.wear.WearSyncManager
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
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
        if (syncResult.isSuccess) {
            pushLatestReadingToWear(userId)
        }
        return if (syncResult.isSuccess) Result.success() else Result.retry()
    }

    // Keeps the Wear OS companion app's "latest reading" screen/tile fresh after a background
    // LibreLinkUp sync, not just after a manual add on the phone (see GlucoseViewModel).
    private suspend fun pushLatestReadingToWear(userId: String) {
        val glucoseReadingRepository: GlucoseReadingRepository = GlobalContext.get().get()
        val settingsPreferences: SettingsPreferences = GlobalContext.get().get()
        val latest = glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
            .first()
            .maxByOrNull { it.timestamp }
        WearSyncManager(applicationContext).pushLatestReading(latest, settingsPreferences.getGlucoseUnit())
    }
}
