package com.dj.insulink.shared.feature.librelink.data.local

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

// Interface (not an expect/actual class like DatabaseFactory/SettingsPreferences) so
// LibreLinkRepository's dedup/sync logic can be unit tested in commonTest with a fake —
// unlike SettingsPreferences this repository has real logic worth covering.
interface LibreLinkSessionStorage {
    fun getSession(): LibreLinkSession?
    fun saveSession(session: LibreLinkSession)
    fun clearSession()
    fun getLastSyncedTimestamp(): Long?
    fun setLastSyncedTimestamp(timestamp: Long)
    fun getLastSyncError(): String?
    fun setLastSyncError(message: String?)
}
