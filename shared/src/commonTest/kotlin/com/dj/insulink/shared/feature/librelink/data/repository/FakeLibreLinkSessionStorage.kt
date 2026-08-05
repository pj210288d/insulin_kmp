package com.dj.insulink.shared.feature.librelink.data.repository

import com.dj.insulink.shared.feature.librelink.data.local.LibreLinkSessionStorage
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

class FakeLibreLinkSessionStorage : LibreLinkSessionStorage {
    val storedSessions = mutableMapOf<String, LibreLinkSession>()
    val storedLastSyncedTimestamps = mutableMapOf<String, Long>()
    val storedLastSyncErrors = mutableMapOf<String, String?>()

    // Single-user test convenience: most tests only ever deal with one userId ("u1"), so
    // these mirror the old no-arg fields against that key for readability at call sites.
    var storedSession: LibreLinkSession?
        get() = storedSessions["u1"]
        set(value) {
            if (value != null) storedSessions["u1"] = value else storedSessions.remove("u1")
        }

    var storedLastSyncedTimestamp: Long?
        get() = storedLastSyncedTimestamps["u1"]
        set(value) {
            if (value != null) storedLastSyncedTimestamps["u1"] = value else storedLastSyncedTimestamps.remove("u1")
        }

    var storedLastSyncError: String?
        get() = storedLastSyncErrors["u1"]
        set(value) {
            storedLastSyncErrors["u1"] = value
        }

    override fun getSession(userId: String): LibreLinkSession? = storedSessions[userId]

    override fun saveSession(userId: String, session: LibreLinkSession) {
        storedSessions[userId] = session
    }

    override fun clearSession(userId: String) {
        storedSessions.remove(userId)
        storedLastSyncErrors.remove(userId)
    }

    override fun getLastSyncedTimestamp(userId: String): Long? = storedLastSyncedTimestamps[userId]

    override fun setLastSyncedTimestamp(userId: String, timestamp: Long) {
        storedLastSyncedTimestamps[userId] = timestamp
    }

    override fun getLastSyncError(userId: String): String? = storedLastSyncErrors[userId]

    override fun setLastSyncError(userId: String, message: String?) {
        storedLastSyncErrors[userId] = message
    }
}
