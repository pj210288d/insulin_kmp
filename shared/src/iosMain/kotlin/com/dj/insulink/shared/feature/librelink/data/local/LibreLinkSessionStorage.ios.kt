package com.dj.insulink.shared.feature.librelink.data.local

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import platform.Foundation.NSUserDefaults

// Unencrypted for now, matching SettingsPreferences' iOS actual — iOS work is unverified
// project-wide (no Mac access yet). Should move to Keychain before real iOS device use.
class IosLibreLinkSessionStorage : LibreLinkSessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getSession(userId: String): LibreLinkSession? {
        val token = defaults.stringForKey(key(userId, KEY_TOKEN)) ?: return null
        val email = defaults.stringForKey(key(userId, KEY_EMAIL)) ?: return null
        val regionHost = defaults.stringForKey(key(userId, KEY_REGION_HOST)) ?: return null
        val accountIdHash = defaults.stringForKey(key(userId, KEY_ACCOUNT_ID_HASH)) ?: return null
        val patientId = defaults.stringForKey(key(userId, KEY_PATIENT_ID)) ?: return null

        return LibreLinkSession(
            email = email,
            token = token,
            regionHost = regionHost,
            accountIdHash = accountIdHash,
            patientId = patientId
        )
    }

    override fun saveSession(userId: String, session: LibreLinkSession) {
        defaults.setObject(session.token, key(userId, KEY_TOKEN))
        defaults.setObject(session.email, key(userId, KEY_EMAIL))
        defaults.setObject(session.regionHost, key(userId, KEY_REGION_HOST))
        defaults.setObject(session.accountIdHash, key(userId, KEY_ACCOUNT_ID_HASH))
        defaults.setObject(session.patientId, key(userId, KEY_PATIENT_ID))
    }

    override fun clearSession(userId: String) {
        // Deliberately keeps KEY_LAST_SYNCED_TIMESTAMP — see the Android actual's clearSession
        // for why: wiping it here caused reconnects to re-insert already-synced readings.
        defaults.removeObjectForKey(key(userId, KEY_TOKEN))
        defaults.removeObjectForKey(key(userId, KEY_EMAIL))
        defaults.removeObjectForKey(key(userId, KEY_REGION_HOST))
        defaults.removeObjectForKey(key(userId, KEY_ACCOUNT_ID_HASH))
        defaults.removeObjectForKey(key(userId, KEY_PATIENT_ID))
        defaults.removeObjectForKey(key(userId, KEY_LAST_SYNC_ERROR))
    }

    override fun getLastSyncedTimestamp(userId: String): Long? {
        if (defaults.objectForKey(key(userId, KEY_LAST_SYNCED_TIMESTAMP)) == null) return null
        return defaults.integerForKey(key(userId, KEY_LAST_SYNCED_TIMESTAMP))
    }

    override fun setLastSyncedTimestamp(userId: String, timestamp: Long) {
        defaults.setInteger(timestamp, key(userId, KEY_LAST_SYNCED_TIMESTAMP))
    }

    override fun getLastSyncError(userId: String): String? = defaults.stringForKey(key(userId, KEY_LAST_SYNC_ERROR))

    override fun setLastSyncError(userId: String, message: String?) {
        if (message == null) {
            defaults.removeObjectForKey(key(userId, KEY_LAST_SYNC_ERROR))
        } else {
            defaults.setObject(message, key(userId, KEY_LAST_SYNC_ERROR))
        }
    }

    // Namespaces every key by the Insulink userId so multiple Google accounts signed into
    // the same install never see each other's LibreLinkUp connection.
    private fun key(userId: String, key: String) = "${userId}_$key"

    companion object {
        private const val KEY_TOKEN = "librelink_token"
        private const val KEY_EMAIL = "librelink_email"
        private const val KEY_REGION_HOST = "librelink_region_host"
        private const val KEY_ACCOUNT_ID_HASH = "librelink_account_id_hash"
        private const val KEY_PATIENT_ID = "librelink_patient_id"
        private const val KEY_LAST_SYNCED_TIMESTAMP = "librelink_last_synced_timestamp"
        private const val KEY_LAST_SYNC_ERROR = "librelink_last_sync_error"
    }
}
