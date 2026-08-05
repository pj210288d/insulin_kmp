package com.dj.insulink.shared.feature.librelink.data.local

import android.content.Context
import android.content.SharedPreferences
import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession

class AndroidLibreLinkSessionStorage(context: Context) : LibreLinkSessionStorage {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getSession(userId: String): LibreLinkSession? {
        val encryptedToken = prefs.getString(key(userId, KEY_TOKEN), null) ?: return null
        val token = LibreLinkKeystoreCipher.decrypt(encryptedToken) ?: return null
        val email = prefs.getString(key(userId, KEY_EMAIL), null) ?: return null
        val regionHost = prefs.getString(key(userId, KEY_REGION_HOST), null) ?: return null
        val accountIdHash = prefs.getString(key(userId, KEY_ACCOUNT_ID_HASH), null) ?: return null
        val patientId = prefs.getString(key(userId, KEY_PATIENT_ID), null) ?: return null

        return LibreLinkSession(
            email = email,
            token = token,
            regionHost = regionHost,
            accountIdHash = accountIdHash,
            patientId = patientId
        )
    }

    override fun saveSession(userId: String, session: LibreLinkSession) {
        prefs.edit()
            .putString(key(userId, KEY_TOKEN), LibreLinkKeystoreCipher.encrypt(session.token))
            .putString(key(userId, KEY_EMAIL), session.email)
            .putString(key(userId, KEY_REGION_HOST), session.regionHost)
            .putString(key(userId, KEY_ACCOUNT_ID_HASH), session.accountIdHash)
            .putString(key(userId, KEY_PATIENT_ID), session.patientId)
            .apply()
    }

    override fun clearSession(userId: String) {
        // Deliberately keeps KEY_LAST_SYNCED_TIMESTAMP: LibreLinkUp's /graph endpoint
        // returns recent history (not just points since last sync), so wiping the cursor
        // here made a reconnect to the same account re-insert readings already in Room
        // (no dedup exists below the cursor check in LibreLinkRepository.syncLatestReadings).
        prefs.edit()
            .remove(key(userId, KEY_TOKEN))
            .remove(key(userId, KEY_EMAIL))
            .remove(key(userId, KEY_REGION_HOST))
            .remove(key(userId, KEY_ACCOUNT_ID_HASH))
            .remove(key(userId, KEY_PATIENT_ID))
            .remove(key(userId, KEY_LAST_SYNC_ERROR))
            .apply()
    }

    override fun getLastSyncedTimestamp(userId: String): Long? {
        val value = prefs.getLong(key(userId, KEY_LAST_SYNCED_TIMESTAMP), NO_VALUE)
        return if (value == NO_VALUE) null else value
    }

    override fun setLastSyncedTimestamp(userId: String, timestamp: Long) {
        prefs.edit().putLong(key(userId, KEY_LAST_SYNCED_TIMESTAMP), timestamp).apply()
    }

    override fun getLastSyncError(userId: String): String? = prefs.getString(key(userId, KEY_LAST_SYNC_ERROR), null)

    override fun setLastSyncError(userId: String, message: String?) {
        prefs.edit().putString(key(userId, KEY_LAST_SYNC_ERROR), message).apply()
    }

    // Namespaces every key by the Insulink userId so multiple Google accounts signed into
    // the same install never see each other's LibreLinkUp connection.
    private fun key(userId: String, key: String) = "${userId}_$key"

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
