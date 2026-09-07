package com.dj.insulink.shared.feature.auth.data.local

import platform.Foundation.NSDate
import platform.Foundation.NSUserDefaults
import platform.Foundation.timeIntervalSince1970

// Samo iOS (isti NSUserDefaults obrazac kao SettingsPreferences.ios.kt) - Android nema
// ekvivalent jer ne treba: pravi Firebase Auth GMS SDK sam čuva/obnavlja sesiju. Čuva ceo
// profil (ne samo tokene) da restoreSession() ne mora dodatni Firestore poziv na svaki cold
// start dok je token još validan.
class AuthTokenStorage {
    private val defaults = NSUserDefaults.standardUserDefaults

    fun save(session: StoredAuthSession) {
        defaults.setObject(session.idToken, KEY_ID_TOKEN)
        defaults.setObject(session.refreshToken, KEY_REFRESH_TOKEN)
        defaults.setObject(session.expiresAtEpochSeconds, KEY_EXPIRES_AT)
        defaults.setObject(session.uid, KEY_UID)
        defaults.setObject(session.email, KEY_EMAIL)
        defaults.setObject(session.firstName, KEY_FIRST_NAME)
        defaults.setObject(session.lastName, KEY_LAST_NAME)
        defaults.setObject(session.friendCode, KEY_FRIEND_CODE)
        defaults.setBool(session.isEmailVerified, KEY_EMAIL_VERIFIED)
    }

    fun load(): StoredAuthSession? {
        val idToken = defaults.stringForKey(KEY_ID_TOKEN) ?: return null
        val refreshToken = defaults.stringForKey(KEY_REFRESH_TOKEN) ?: return null
        val uid = defaults.stringForKey(KEY_UID) ?: return null
        return StoredAuthSession(
            idToken = idToken,
            refreshToken = refreshToken,
            expiresAtEpochSeconds = defaults.doubleForKey(KEY_EXPIRES_AT),
            uid = uid,
            email = defaults.stringForKey(KEY_EMAIL).orEmpty(),
            firstName = defaults.stringForKey(KEY_FIRST_NAME).orEmpty(),
            lastName = defaults.stringForKey(KEY_LAST_NAME).orEmpty(),
            friendCode = defaults.stringForKey(KEY_FRIEND_CODE).orEmpty(),
            isEmailVerified = defaults.boolForKey(KEY_EMAIL_VERIFIED)
        )
    }

    fun clear() {
        listOf(
            KEY_ID_TOKEN, KEY_REFRESH_TOKEN, KEY_EXPIRES_AT, KEY_UID, KEY_EMAIL,
            KEY_FIRST_NAME, KEY_LAST_NAME, KEY_FRIEND_CODE, KEY_EMAIL_VERIFIED
        ).forEach { defaults.removeObjectForKey(it) }
    }

    companion object {
        private const val KEY_ID_TOKEN = "auth_id_token"
        private const val KEY_REFRESH_TOKEN = "auth_refresh_token"
        private const val KEY_EXPIRES_AT = "auth_expires_at"
        private const val KEY_UID = "auth_uid"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_FIRST_NAME = "auth_first_name"
        private const val KEY_LAST_NAME = "auth_last_name"
        private const val KEY_FRIEND_CODE = "auth_friend_code"
        private const val KEY_EMAIL_VERIFIED = "auth_email_verified"

        fun nowEpochSeconds(): Double = NSDate().timeIntervalSince1970
    }
}

data class StoredAuthSession(
    val idToken: String,
    val refreshToken: String,
    val expiresAtEpochSeconds: Double,
    val uid: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val friendCode: String,
    val isEmailVerified: Boolean
)
