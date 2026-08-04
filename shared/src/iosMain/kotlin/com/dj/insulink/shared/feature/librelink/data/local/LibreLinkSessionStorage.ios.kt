package com.dj.insulink.shared.feature.librelink.data.local

import com.dj.insulink.shared.feature.librelink.domain.model.LibreLinkSession
import platform.Foundation.NSUserDefaults

// Unencrypted for now, matching SettingsPreferences' iOS actual — iOS work is unverified
// project-wide (no Mac access yet). Should move to Keychain before real iOS device use.
class IosLibreLinkSessionStorage : LibreLinkSessionStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getSession(): LibreLinkSession? {
        val token = defaults.stringForKey(KEY_TOKEN) ?: return null
        val email = defaults.stringForKey(KEY_EMAIL) ?: return null
        val regionHost = defaults.stringForKey(KEY_REGION_HOST) ?: return null
        val accountIdHash = defaults.stringForKey(KEY_ACCOUNT_ID_HASH) ?: return null
        val patientId = defaults.stringForKey(KEY_PATIENT_ID) ?: return null

        return LibreLinkSession(
            email = email,
            token = token,
            regionHost = regionHost,
            accountIdHash = accountIdHash,
            patientId = patientId
        )
    }

    override fun saveSession(session: LibreLinkSession) {
        defaults.setObject(session.token, KEY_TOKEN)
        defaults.setObject(session.email, KEY_EMAIL)
        defaults.setObject(session.regionHost, KEY_REGION_HOST)
        defaults.setObject(session.accountIdHash, KEY_ACCOUNT_ID_HASH)
        defaults.setObject(session.patientId, KEY_PATIENT_ID)
    }

    override fun clearSession() {
        // Deliberately keeps KEY_LAST_SYNCED_TIMESTAMP — see the Android actual's clearSession
        // for why: wiping it here caused reconnects to re-insert already-synced readings.
        defaults.removeObjectForKey(KEY_TOKEN)
        defaults.removeObjectForKey(KEY_EMAIL)
        defaults.removeObjectForKey(KEY_REGION_HOST)
        defaults.removeObjectForKey(KEY_ACCOUNT_ID_HASH)
        defaults.removeObjectForKey(KEY_PATIENT_ID)
        defaults.removeObjectForKey(KEY_LAST_SYNC_ERROR)
    }

    override fun getLastSyncedTimestamp(): Long? {
        if (defaults.objectForKey(KEY_LAST_SYNCED_TIMESTAMP) == null) return null
        return defaults.integerForKey(KEY_LAST_SYNCED_TIMESTAMP)
    }

    override fun setLastSyncedTimestamp(timestamp: Long) {
        defaults.setInteger(timestamp, KEY_LAST_SYNCED_TIMESTAMP)
    }

    override fun getLastSyncError(): String? = defaults.stringForKey(KEY_LAST_SYNC_ERROR)

    override fun setLastSyncError(message: String?) {
        if (message == null) {
            defaults.removeObjectForKey(KEY_LAST_SYNC_ERROR)
        } else {
            defaults.setObject(message, KEY_LAST_SYNC_ERROR)
        }
    }

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
