package com.android.skg.finapp.ui.cards

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScannerScreen(
    onCancel: () -> Unit,
    onCardScanned: (cardNumber: String, holderName: String, expiry: String) -> Unit,
) {
    val context = LocalContext.current
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) {
            statusMessage = "Processing image..."
            val imageFile = File(context.cacheDir, "card_scan.jpg")
            if (imageFile.exists()) {
                processCardImage(imageFile, onCardScanned) { message ->
                    statusMessage = message
                }
            }
        } else {
            statusMessage = "Camera cancelled"
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
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Scan your credit card", style = MaterialTheme.typography.titleMedium)

                statusMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }

                Button(
                    onClick = {
                        val imageFile = File(context.cacheDir, "card_scan.jpg")
                        val imageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            imageFile,
                        )
                        cameraLauncher.launch(imageUri)
                    },
                ) {
                    Text("Capture with Camera")
                }

                Button(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun processCardImage(
    imageFile: File,
    onCardScanned: (cardNumber: String, holderName: String, expiry: String) -> Unit,
    onStatusMessage: (String?) -> Unit,
) {
    val executor = Executors.newSingleThreadExecutor()
    executor.execute {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap != null) {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val recognizedText = visionText.text
                        val (cardNumber, holderName, expiry) = extractCardDetails(recognizedText)

                        if (cardNumber.isNotEmpty()) {
                            onStatusMessage(null)
                            onCardScanned(cardNumber, holderName, expiry)
                        } else {
                            onStatusMessage("No card detected. Please try again.")
                        }
                    }
                    .addOnFailureListener {
                        onStatusMessage("Recognition failed: ${it.localizedMessage}")
                    }
            } else {
                onStatusMessage("Failed to load image")
            }
        } catch (e: Exception) {
            onStatusMessage("Error: ${e.localizedMessage}")
        }
    }
}

private fun extractCardDetails(text: String): Triple<String, String, String> {
    var cardNumber = ""
    var holderName = ""
    var expiry = ""

    val lines = text.split("\n").map { it.trim() }

    // Extract card number: look for 16 consecutive digits
    val cardNumberRegex = Regex("\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}|\\d{16}")
    for (line in lines) {
        val match = cardNumberRegex.find(line)
        if (match != null) {
            cardNumber = match.value.filter { it.isDigit() }
            if (cardNumber.length == 16) break
        }
    }

    // Extract expiry: look for MM/YY or MMYY pattern
    val expiryRegex = Regex("(0[1-9]|1[0-2])/?\\d{2}")
    for (line in lines) {
        val match = expiryRegex.find(line)
        if (match != null) {
            expiry = match.value.filter { it.isDigit() }.take(4)
            break
        }
    }

    // Extract holder name: typically on a separate line, letters and spaces
    val nameRegex = Regex("[A-Z][A-Z\\s]{5,}")
    for (line in lines) {
        if (line.length > 5 && !line.contains(Regex("\\d"))) {
            if (nameRegex.containsMatchIn(line)) {
                holderName = line.uppercase()
                break
            }
        }
    }

    return Triple(cardNumber, holderName, expiry)
}


