package com.dj.insulink.shared.feature.librelink.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.dj.insulink.shared.feature.glucose.data.repository.GlucoseReadingRepository
import com.dj.insulink.shared.feature.glucose.data.wear.WearSyncCoordinator
import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

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

    private suspend fun pushLatestReadingToWear(userId: String) {
        val glucoseReadingRepository: GlucoseReadingRepository = GlobalContext.get().get()
        val settingsPreferences: SettingsPreferences = GlobalContext.get().get()
        val wearSyncCoordinator: WearSyncCoordinator = GlobalContext.get().get()
        val latest = glucoseReadingRepository.getAllGlucoseReadingsForUser(userId)
            .first()
            .maxByOrNull { it.timestamp }
        wearSyncCoordinator.pushLatestReading(latest, settingsPreferences.getGlucoseUnit())
    }
}
