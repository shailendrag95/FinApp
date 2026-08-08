package com.android.skg.finapp.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class AppLockManager(
    context: Context,
    private val preferencesManager: PreferencesManager,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    var isUnlocked: Boolean = false
        private set

    var lastActivityTime: Long = System.currentTimeMillis()
        private set

    val isPinSet: Boolean
        get() = prefs.contains(KEY_PIN_HASH)

    fun touchActivity() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun lock() {
        isUnlocked = false
    }

    fun unlock() {
        isUnlocked = true
    }

    fun unlockWithPin(pin: String): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val valid = hashPin(pin, salt) == hash
        if (valid) isUnlocked = true
        return valid
    }

    fun setupPin(pin: String) {
        require(pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH) { "PIN must be 4-6 digits" }
        val salt = generateSalt()
        prefs.edit()
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hashPin(pin, salt))
            .apply()
        isUnlocked = true
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!unlockWithPin(oldPin)) return false
        lock()
        setupPin(newPin)
        return true
    }

    fun shouldAutoLock(autoLockMinutes: Int): Boolean {
        if (!isUnlocked || autoLockMinutes <= 0) return false
        val elapsed = System.currentTimeMillis() - lastActivityTime
        return elapsed >= autoLockMinutes * 60_000L
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    companion object {
        private const val PREFS_NAME = "finapp_lock_prefs"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6
    }
}
