package com.android.skg.finapp.ui.cards

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.abs

private const val TAG = "CardScanner"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScannerScreen(
    onCancel: () -> Unit,
    onCardScanned: (cardNumber: String, holderName: String, expiry: String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    // State for recognized details to show in editable fields
    var scannedCardNumber by remember { mutableStateOf("") }
    var scannedHolderName by remember { mutableStateOf("") }
    var scannedExpiry by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            statusMessage = "Camera permission is required to scan cards"
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission) {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            controller.setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                MlKitAnalyzer(
                    listOf(recognizer),
                    CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    ContextCompat.getMainExecutor(context)
                ) { result ->
                    val visionText = result.getValue(recognizer)
                    if (visionText != null) {
                        val (cardNumber, holderName, expiry) = extractCardDetails(visionText)
                        
                        if (cardNumber.isNotEmpty() && scannedCardNumber.isEmpty()) {
                            scannedCardNumber = cardNumber
                        }
                        if (holderName.isNotEmpty() && scannedHolderName.isEmpty()) {
                            scannedHolderName = holderName
                        }
                        if (expiry.isNotEmpty() && scannedExpiry.isEmpty()) {
                            scannedExpiry = expiry
                        }

                        if (scannedCardNumber.length >= 15) {
                            statusMessage = "Card detected! Please verify and edit if needed."
                        }
                    }
                }
            )
            controller.bindToLifecycle(lifecycleOwner)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Card") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isFlashOn = !isFlashOn
                        controller.enableTorch(isFlashOn)
                    }) {
                        Icon(
                            if (isFlashOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                            contentDescription = "Toggle Flash"
                        )
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            this.controller = controller
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay with card guide
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(220.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.1f))
                    )
                }

                // Scanning Indicator
                if (scannedCardNumber.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                            .size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }

                // Scanning Results and Status Panel
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        statusMessage ?: "Position card within the frame",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Card Number Field
                    OutlinedTextField(
                        value = scannedCardNumber,
                        onValueChange = { 
                            if (it.length <= 16) {
                                scannedCardNumber = it.filter { c -> c.isDigit() }
                            }
                        },
                        label = { Text("Card Number", color = Color.White.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Holder Name Field
                        OutlinedTextField(
                            value = scannedHolderName,
                            onValueChange = { scannedHolderName = it },
                            label = { Text("Holder Name", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            singleLine = true
                        )

                        // Expiry Field
                        OutlinedTextField(
                            value = scannedExpiry,
                            onValueChange = { scannedExpiry = it.filter { c -> c.isDigit() } },
                            label = { Text("Expiry", color = Color.White.copy(alpha = 0.7f)) },
                            modifier = Modifier.weight(0.5f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Button(
                        onClick = {
                            onCardScanned(scannedCardNumber, scannedHolderName, scannedExpiry)
                        },
                        enabled = scannedCardNumber.length >= 15,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done")
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(statusMessage ?: "Requesting permission...")
                }
            }
        }
    }
}

private fun extractCardDetails(visionText: Text): Triple<String, String, String> {
    var cardNumber = ""
    var holderName = ""
    var expiry = ""

    val fullText = visionText.text.uppercase()
    Log.d(TAG, "OCR raw text: $fullText")

    // 1. Better Card Number Detection (Horizontal & Vertical)
    val digitElements = visionText.textBlocks.flatMap { it.lines }.flatMap { it.elements }
        .filter { it.text.any { char -> char.isDigit() || char == 'I' || char == 'L' || char == 'O' || char == 'S' } }

    // Try Horizontal Groups (Standard)
    val horizontalCandidates = mutableListOf<String>()
    visionText.textBlocks.forEach { block ->
        val text = block.text.replace("\n", " ")
            .map { char ->
                when (char) {
                    'O' -> '0'
                    'I', 'L' -> '1'
                    'S' -> '5'
                    else -> char
                }
            }.joinToString("")
            .filter { it.isDigit() || it == ' ' || it == '-' }
        horizontalCandidates.add(text.filter { it.isDigit() })
    }

    // Try Vertical Groups (for Vertical Cards)
    val verticalCandidates = mutableListOf<String>()
    // Since X might vary slightly, use a larger tolerance for grouping
    val tolerantXGroups = mutableMapOf<Int, MutableList<Text.Element>>()
    digitElements.forEach { element ->
        val centerX = element.boundingBox?.centerX() ?: 0
        // Increase tolerance to 40 pixels for different resolutions/alignments
        val group = tolerantXGroups.keys.find { abs(it - centerX) < 40 }
        if (group != null) {
            tolerantXGroups[group]?.add(element)
        } else {
            tolerantXGroups[centerX] = mutableListOf(element)
        }
    }
    
    tolerantXGroups.values.forEach { group ->
        val verticalText = group.sortedBy { it.boundingBox?.centerY() ?: 0 }
            .joinToString("") { element ->
                element.text.map { char ->
                    when (char) {
                        'O' -> '0'
                        'I', 'L' -> '1'
                        'S' -> '5'
                        else -> char
                    }
                }.joinToString("")
            }
            .filter { it.isDigit() }
        
        Log.d(TAG, "Vertical candidate found: $verticalText")
        verticalCandidates.add(verticalText)
    }

    val allCandidates = (horizontalCandidates + verticalCandidates)
        .flatMap { candidate ->
            // Sliding window to find 15-16 digit sequences within longer strings
            val digitsOnly = candidate.filter { it.isDigit() }
            val sequences = mutableListOf<String>()
            if (digitsOnly.length >= 15) {
                for (i in 0..digitsOnly.length - 15) {
                    sequences.add(digitsOnly.substring(i, (i + 15).coerceAtMost(digitsOnly.length)))
                    if (i + 16 <= digitsOnly.length) {
                        sequences.add(digitsOnly.substring(i, i + 16))
                    }
                }
            }
            sequences
        }.distinct()

    Log.d(TAG, "All digit sequences: $allCandidates")

    // Find best candidate via Luhn
    var bestCandidate = ""
    for (candidate in allCandidates) {
        if (isValidLuhn(candidate)) {
            bestCandidate = candidate
            break
        }
    }
    
    if (bestCandidate.isNotEmpty()) {
        cardNumber = bestCandidate
    } else if (allCandidates.isNotEmpty()) {
        // Fallback to the longest sequence if none pass Luhn
        cardNumber = allCandidates.maxByOrNull { it.length } ?: ""
    }

    val allLines = visionText.textBlocks.flatMap { it.lines }.map { it.text.trim().uppercase() }

    // 2. Extract Expiry (MM/YY or MMYY)
    val expiryRegex = Regex("(0[1-9]|1[0-2])[ /]?(\\d{2})")
    for (line in allLines) {
        val match = expiryRegex.find(line)
        if (match != null) {
            expiry = match.groupValues[1] + match.groupValues[2]
            break
        }
    }

    // 3. Extract Holder Name
    val nameBlacklist = listOf("VISA", "MASTERCARD", "RUPAY", "CREDIT", "DEBIT", "BANK", "VALID", "FROM", "THRU", "EXPIRES", "SELECT")
    val nameRegex = Regex("^[A-Z]{2,15}(?: [A-Z]{2,15}){1,2}$")
    
    for (line in allLines) {
        if (line.length > 5 && !line.contains(Regex("\\d"))) {
            val isBlacklisted = nameBlacklist.any { line.contains(it) }
            if (!isBlacklisted && nameRegex.matches(line)) {
                holderName = line
                break
            }
        }
    }

    return Triple(cardNumber, holderName, expiry)
}

private fun isValidLuhn(number: String): Boolean {
    var sum = 0
    var alternate = false
    for (i in number.length - 1 downTo 0) {
        var n = number[i].digitToInt()
        if (alternate) {
            n *= 2
            if (n > 9) {
                n = (n % 10) + 1
            }
        }
        sum += n
        alternate = !alternate
    }
    return (sum % 10 == 0)
}

