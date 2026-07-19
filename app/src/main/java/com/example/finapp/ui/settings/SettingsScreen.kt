package com.example.finapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.finapp.security.AppLockManager
import com.example.finapp.security.PreferencesManager
import com.example.finapp.ui.LocalAppContainer
import com.example.finapp.ui.components.BiometricAuthHelper
import com.example.finapp.ui.components.PinAuthDialog
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onExportImport: () -> Unit,
    onCardTypeManager: () -> Unit = {},
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()

    val darkTheme by container.preferencesManager.isDarkTheme.collectAsState(initial = false)
    val autoLockMinutes by container.preferencesManager.autoLockMinutes.collectAsState(
        initial = PreferencesManager.DEFAULT_AUTO_LOCK_MINUTES,
    )
    val biometricEnabled by container.preferencesManager.biometricEnabled.collectAsState(initial = false)

    var autoLockText by remember(autoLockMinutes) { mutableStateOf(autoLockMinutes.toString()) }
    var showChangePin by remember { mutableStateOf(false) }
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    if (showChangePin) {
        PinAuthDialog(
            title = "Enter current PIN",
            onDismiss = { showChangePin = false },
            onConfirm = { pin ->
                val valid = container.appLockManager.unlockWithPin(pin)
                if (valid) {
                    oldPin = pin
                    showChangePin = false
                }
                valid
            },
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Security", style = MaterialTheme.typography.titleMedium)
            SettingRow("Biometric unlock") {
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !BiometricAuthHelper.canAuthenticate(activity)) return@Switch
                        scope.launch { container.preferencesManager.setBiometricEnabled(enabled) }
                    },
                    enabled = BiometricAuthHelper.canAuthenticate(activity),
                )
            }
            OutlinedTextField(
                value = autoLockText,
                onValueChange = {
                    if (it.all { c -> c.isDigit() }) {
                        autoLockText = it
                        it.toIntOrNull()?.let { minutes ->
                            scope.launch { container.preferencesManager.setAutoLockMinutes(minutes) }
                        }
                    }
                },
                label = { Text("Auto-lock (minutes, 0 = off)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(onClick = { showChangePin = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Change PIN")
            }

            if (oldPin.isNotEmpty()) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) newPin = it.filter { c -> c.isDigit() } },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it.filter { c -> c.isDigit() } },
                    label = { Text("Confirm new PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                pinError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = {
                        when {
                            newPin.length < AppLockManager.MIN_PIN_LENGTH -> pinError = "PIN must be at least 4 digits"
                            newPin != confirmPin -> pinError = "PINs do not match"
                            else -> {
                                val changed = container.appLockManager.changePin(oldPin, newPin)
                                pinError = if (changed) null else "Could not change PIN"
                                if (changed) {
                                    oldPin = ""
                                    newPin = ""
                                    confirmPin = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save new PIN")
                }
            }

            Text("Data", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onExportImport, modifier = Modifier.fillMaxWidth()) {
                Text("Export / Import Backup")
            }

            Text("Cards", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onCardTypeManager, modifier = Modifier.fillMaxWidth()) {
                Text("Manage Card Types")
            }

            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            SettingRow("Dark theme") {
                Switch(
                    checked = darkTheme,
                    onCheckedChange = { scope.launch { container.preferencesManager.setDarkTheme(it) } },
                )
            }

            Text("Privacy", style = MaterialTheme.typography.titleMedium)
            Text(
                "FinApp stores all data locally on your device. No internet connection is used. " +
                    "Regular encrypted backups are recommended — data is lost if the app is uninstalled without a backup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label)
        control()
    }
}
