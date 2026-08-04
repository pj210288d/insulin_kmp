package com.dj.insulink.shared.feature.librelink.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

class AndroidLibreLinkSessionStorage(context: Context) : LibreLinkSessionStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getSession(): LibreLinkSession? {
        val encryptedToken = prefs.getString(KEY_TOKEN, null) ?: return null
        val token = LibreLinkKeystoreCipher.decrypt(encryptedToken) ?: return null
        val email = prefs.getString(KEY_EMAIL, null) ?: return null
        val regionHost = prefs.getString(KEY_REGION_HOST, null) ?: return null
        val accountIdHash = prefs.getString(KEY_ACCOUNT_ID_HASH, null) ?: return null
        val patientId = prefs.getString(KEY_PATIENT_ID, null) ?: return null

        return LibreLinkSession(
            email = email,
            token = token,
            regionHost = regionHost,
            accountIdHash = accountIdHash,
            patientId = patientId
        )
    }

    override fun saveSession(session: LibreLinkSession) {
        prefs.edit()
            .putString(KEY_TOKEN, LibreLinkKeystoreCipher.encrypt(session.token))
            .putString(KEY_EMAIL, session.email)
            .putString(KEY_REGION_HOST, session.regionHost)
            .putString(KEY_ACCOUNT_ID_HASH, session.accountIdHash)
            .putString(KEY_PATIENT_ID, session.patientId)
            .apply()
    }

    override fun clearSession() {
        // Deliberately keeps KEY_LAST_SYNCED_TIMESTAMP: LibreLinkUp's /graph endpoint
        // returns recent history (not just points since last sync), so wiping the cursor
        // here made a reconnect to the same account re-insert readings already in Room
        // (no dedup exists below the cursor check in LibreLinkRepository.syncLatestReadings).
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .remove(KEY_REGION_HOST)
            .remove(KEY_ACCOUNT_ID_HASH)
            .remove(KEY_PATIENT_ID)
            .remove(KEY_LAST_SYNC_ERROR)
            .apply()
    }

    override fun getLastSyncedTimestamp(): Long? {
        val value = prefs.getLong(KEY_LAST_SYNCED_TIMESTAMP, NO_VALUE)
        return if (value == NO_VALUE) null else value
    }

    override fun setLastSyncedTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNCED_TIMESTAMP, timestamp).apply()
    }

    override fun getLastSyncError(): String? = prefs.getString(KEY_LAST_SYNC_ERROR, null)

    override fun setLastSyncError(message: String?) {
        prefs.edit().putString(KEY_LAST_SYNC_ERROR, message).apply()
    }

    companion object {
        private const val PREFS_NAME = "insulink_librelink"
        private const val KEY_TOKEN = "token"
        private const val KEY_EMAIL = "email"
        private const val KEY_REGION_HOST = "region_host"
        private const val KEY_ACCOUNT_ID_HASH = "account_id_hash"
        private const val KEY_PATIENT_ID = "patient_id"
        private const val KEY_LAST_SYNCED_TIMESTAMP = "last_synced_timestamp"
        private const val KEY_LAST_SYNC_ERROR = "last_sync_error"
        private const val NO_VALUE = -1L
    }
}
