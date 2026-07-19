package com.example.finapp.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.finapp.security.AppLockManager
import com.example.finapp.ui.components.BiometricAuthHelper

@Composable
fun SetupLockScreen(
    appLockManager: AppLockManager,
    biometricAvailable: Boolean,
    onSetupComplete: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Welcome to FinApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Set a 4–6 digit PIN to protect your financial data. Everything stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= AppLockManager.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) pin = it
            },
            label = { Text("Create PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= AppLockManager.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) confirmPin = it
            },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                when {
                    pin.length < AppLockManager.MIN_PIN_LENGTH -> error = "PIN must be at least 4 digits"
                    pin != confirmPin -> error = "PINs do not match"
                    else -> {
                        appLockManager.setupPin(pin)
                        onSetupComplete()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length >= AppLockManager.MIN_PIN_LENGTH && confirmPin.length >= AppLockManager.MIN_PIN_LENGTH,
        ) {
            Text("Continue")
        }
        if (biometricAvailable) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You can enable biometric unlock in Settings after setup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun LockScreen(
    activity: FragmentActivity,
    appLockManager: AppLockManager,
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("FinApp Locked", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Enter your PIN to continue", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= AppLockManager.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) {
                    pin = it
                    error = null
                    if (it.length >= AppLockManager.MIN_PIN_LENGTH && appLockManager.unlockWithPin(it)) {
                        onUnlocked()
                    }
                }
            },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (biometricEnabled && BiometricAuthHelper.canAuthenticate(activity)) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    BiometricAuthHelper.authenticate(
                        activity = activity,
                        title = "Unlock FinApp",
                        subtitle = "Use biometrics to access your vault",
                        onSuccess = {
                            appLockManager.unlock()
                            onUnlocked()
                        },
                        onError = { error = it },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Use Biometrics")
            }
        }
    }
}
