package com.example.finapp.ui.export

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.finapp.ui.LocalAppContainer
import com.example.finapp.ui.components.ConfirmDialog
import com.example.finapp.ui.components.PinAuthDialog
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var pendingImportContent by remember { mutableStateOf<String?>(null) }
    var showCsvWarning by remember { mutableStateOf(false) }
    var showImportModeDialog by remember { mutableStateOf(false) }
    var showAuthForExport by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var csvFileUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: error("Could not read file")
                pendingImportContent = content
                showImportModeDialog = true
            }.onFailure {
                errorMessage = it.message
            }
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        csvFileUri = uri
        scope.launch {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: error("Could not read file")
                container.exportRepository.importCsv(content)
                // Delete file after successful import
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    // Ignore file deletion errors
                }
                statusMessage = "CSV imported successfully and file deleted"
                errorMessage = null
            }.onFailure {
                errorMessage = "CSV import failed: ${it.message}"
                statusMessage = null
            }
        }
    }

    fun shareFile(fileName: String, content: String, mimeType: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(content)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share backup"))
    }

    // Share content as plain text (no file written to storage)
    fun shareText(content: String, mimeType: String, title: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_TEXT, content)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    if (showAuthForExport) {
        PinAuthDialog(
            title = "Authenticate to export",
            onDismiss = { showAuthForExport = false },
            onConfirm = { pin ->
                val success = container.appLockManager.unlockWithPin(pin)
                if (success) {
                    showAuthForExport = false
                    scope.launch {
                        runCatching {
                            val json = container.exportRepository.exportEncryptedJson(exportPassword)
                            shareFile("finapp_backup.json", json, "application/json")
                            statusMessage = "Encrypted backup ready to share"
                        }.onFailure { errorMessage = it.message }
                    }
                }
                success
            },
        )
    }

    if (showCsvWarning) {
        AlertDialog(
            onDismissRequest = { showCsvWarning = false },
            title = { Text("Unencrypted export") },
            text = {
                Text(
                    "CSV exports are NOT encrypted. Anyone with the file can read your card and account numbers. Continue only if you understand the risk.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCsvWarning = false
                        scope.launch {
                            runCatching {
                                val csv = container.exportRepository.exportCsv()
                                shareFile("finapp_backup.csv", csv, "text/csv")
                                statusMessage = "CSV exported — store it securely"
                            }.onFailure { errorMessage = it.message }
                        }
                    },
                ) { Text("Export CSV") }
            },
            dismissButton = {
                TextButton(onClick = { showCsvWarning = false }) { Text("Cancel") }
            },
        )
    }

    if (showImportModeDialog && pendingImportContent != null) {
        AlertDialog(
            onDismissRequest = {
                showImportModeDialog = false
                pendingImportContent = null
            },
            title = { Text("Import backup") },
            text = { Text("Replace all existing data, or merge with current data?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val content = pendingImportContent ?: return@TextButton
                        scope.launch {
                            runCatching {
                                val json = container.exportRepository.parseEncryptedImport(content, importPassword)
                                container.exportRepository.importData(json, replaceExisting = true)
                                statusMessage = "Backup restored (replaced existing data)"
                            }.onFailure { errorMessage = it.message ?: "Import failed" }
                            showImportModeDialog = false
                            pendingImportContent = null
                        }
                    },
                ) { Text("Replace") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val content = pendingImportContent ?: return@TextButton
                        scope.launch {
                            runCatching {
                                val json = container.exportRepository.parseEncryptedImport(content, importPassword)
                                container.exportRepository.importData(json, replaceExisting = false)
                                statusMessage = "Backup merged successfully"
                            }.onFailure { errorMessage = it.message ?: "Import failed" }
                            showImportModeDialog = false
                            pendingImportContent = null
                        }
                    },
                ) { Text("Merge") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export / Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Export", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = exportPassword,
                onValueChange = { exportPassword = it },
                label = { Text("Export password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = {
                    if (exportPassword.length < 4) {
                        errorMessage = "Export password must be at least 4 characters"
                    } else {
                        showAuthForExport = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export Encrypted JSON")
            }
            Button(
                onClick = { showCsvWarning = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Export CSV (unencrypted)")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Import", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = importPassword,
                onValueChange = { importPassword = it },
                label = { Text("Backup password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Button(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = importPassword.isNotBlank(),
            ) {
                Text("Import Encrypted Backup")
            }

            Button(
                onClick = { csvImportLauncher.launch(arrayOf("text/csv", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import CSV (bulk)")
            }

            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            val sample = container.exportRepository.buildSampleCsv()
                            // share as plain text to avoid writing file details to storage
                            shareText(sample, "text/csv", "Share sample CSV")
                            statusMessage = "Sample CSV ready to share"
                        }.onFailure { errorMessage = it.message }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Share sample CSV")
            }

            statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
