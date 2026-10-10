package com.dj.insulink.shared.feature.librelink.data.local

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

interface LibreLinkSessionStorage {
    fun getSession(userId: String): LibreLinkSession?
    fun saveSession(userId: String, session: LibreLinkSession)
    fun clearSession(userId: String)
    fun getLastSyncedTimestamp(userId: String): Long?
    fun setLastSyncedTimestamp(userId: String, timestamp: Long)
    fun getLastSyncError(userId: String): String?
    fun setLastSyncError(userId: String, message: String?)
}
