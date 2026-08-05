package com.dj.insulink.shared.feature.librelink.data.local

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

// Interface (not an expect/actual class like DatabaseFactory/SettingsPreferences) so
// LibreLinkRepository's dedup/sync logic can be unit tested in commonTest with a fake —
// unlike SettingsPreferences this repository has real logic worth covering.
//
// Every method is keyed by the current Insulink (Firebase) userId - LibreLinkUp is a
// per-Insulink-account connection, not a per-device one, and prior to adding this
// parameter the underlying storage was a single global slot shared by whichever Insulink
// user happened to be signed in. That meant a second Google account signing into the same
// installed app saw (and synced readings from) the first account's LibreLinkUp connection.
interface LibreLinkSessionStorage {
    fun getSession(userId: String): LibreLinkSession?
    fun saveSession(userId: String, session: LibreLinkSession)
    fun clearSession(userId: String)
    fun getLastSyncedTimestamp(userId: String): Long?
    fun setLastSyncedTimestamp(userId: String, timestamp: Long)
    fun getLastSyncError(userId: String): String?
    fun setLastSyncError(userId: String, message: String?)
}
