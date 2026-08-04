package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

class FakeLibreLinkSessionStorage : LibreLinkSessionStorage {
    var storedSession: LibreLinkSession? = null
    var storedLastSyncedTimestamp: Long? = null
    var storedLastSyncError: String? = null

    override fun getSession(): LibreLinkSession? = storedSession

    override fun saveSession(session: LibreLinkSession) {
        storedSession = session
    }

    override fun clearSession() {
        storedSession = null
        storedLastSyncError = null
    }

    override fun getLastSyncedTimestamp(): Long? = storedLastSyncedTimestamp

    override fun setLastSyncedTimestamp(timestamp: Long) {
        storedLastSyncedTimestamp = timestamp
    }

    override fun getLastSyncError(): String? = storedLastSyncError

    override fun setLastSyncError(message: String?) {
        storedLastSyncError = message
    }
}
