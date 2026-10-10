package com.dj.insulink.shared.feature.librelink.data.sync

interface LibreLinkSyncCoordinator {
    fun start(userId: String)
    fun stop()
}
