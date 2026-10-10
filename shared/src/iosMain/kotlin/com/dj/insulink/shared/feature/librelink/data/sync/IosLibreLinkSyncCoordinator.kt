package com.dj.insulink.shared.feature.librelink.data.sync

import com.dj.insulink.shared.feature.librelink.data.repository.LibreLinkRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class IosLibreLinkSyncCoordinator(
    private val libreLinkRepository: LibreLinkRepository
) : LibreLinkSyncCoordinator {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var periodicSyncJob: Job? = null

    override fun start(userId: String) {
        periodicSyncJob?.cancel()
        periodicSyncJob = scope.launch {
            while (isActive) {
                delay(PERIODIC_SYNC_INTERVAL_MS)
                runCatching { libreLinkRepository.syncLatestReadings(userId) }
            }
        }
    }

    override fun stop() {
        periodicSyncJob?.cancel()
        periodicSyncJob = null
    }

    private companion object {
        const val PERIODIC_SYNC_INTERVAL_MS = 10L * 60L * 1000L
    }
}
