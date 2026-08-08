package com.android.skg.finapp.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Base64
import java.security.SecureRandom

class DatabaseKeyManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getOrCreatePassphrase(): ByteArray {
        val existing = prefs.getString(KEY_PASSPHRASE, null)
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        SecureRandom().nextBytes(passphrase)
        prefs.edit()
            .putString(KEY_PASSPHRASE, Base64.encodeToString(passphrase, Base64.NO_WRAP))
            .apply()
        return passphrase
    }

    companion object {
        private const val PREFS_NAME = "finapp_db_key_prefs"
        private const val KEY_PASSPHRASE = "db_passphrase"
        private const val PASSPHRASE_LENGTH = 32
    }
}
