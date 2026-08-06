package com.dj.insulink.shared.feature.librelink.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// androidx.security:security-crypto (EncryptedSharedPreferences) was deprecated by Google
// as of its 1.1.0 stable release (July 2025) in favor of using Android Keystore directly,
// so the LibreLinkUp auth token is encrypted here with a Keystore-backed AES key instead
// of that library.
internal object LibreLinkKeystoreCipher {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "librelink_session_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cipherText, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? {
        return try {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val cipherText = combined.copyOfRange(GCM_IV_LENGTH_BYTES, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}
