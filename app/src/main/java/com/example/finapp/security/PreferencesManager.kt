package com.example.finapp.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "finapp_settings")

class PreferencesManager(private val context: Context) {
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { it[DARK_THEME] ?: false }
    val autoLockMinutes: Flow<Int> = context.dataStore.data.map { it[AUTO_LOCK_MINUTES] ?: DEFAULT_AUTO_LOCK_MINUTES }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[DARK_THEME] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { it[AUTO_LOCK_MINUTES] = minutes }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    companion object {
        private val DARK_THEME = booleanPreferencesKey("dark_theme")
        private val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        const val DEFAULT_AUTO_LOCK_MINUTES = 2
    }
}
