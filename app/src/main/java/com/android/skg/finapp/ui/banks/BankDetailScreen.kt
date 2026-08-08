package com.android.skg.finapp.ui.banks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.android.skg.finapp.domain.model.BankAccount
import com.android.skg.finapp.security.ClipboardHelper
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.ui.components.BiometricAuthHelper
import com.android.skg.finapp.ui.components.ConfirmDialog
import com.android.skg.finapp.ui.components.MaskedValueRow
import com.android.skg.finapp.ui.components.PinAuthDialog
import com.android.skg.finapp.util.maskAccountNumber
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankDetailScreen(
    accountId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val clipboardHelper = remember { ClipboardHelper(context, scope) }

    var account by remember { mutableStateOf<BankAccount?>(null) }
    var revealedNumber by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var biometricEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(accountId) {
        account = container.bankAccountRepository.getAccount(accountId)
        container.preferencesManager.biometricEnabled.collect { biometricEnabled = it }
    }

    fun requestReveal() {
        if (biometricEnabled && BiometricAuthHelper.canAuthenticate(activity)) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Reveal account number",
                subtitle = "Authenticate to view account details",
                onSuccess = { revealedNumber = account?.accountNumber },
                onError = { showAuthDialog = true },
            )
        } else {
            showAuthDialog = true
        }
    }

    if (showAuthDialog) {
        PinAuthDialog(
            title = "Enter PIN to reveal",
            onDismiss = { showAuthDialog = false },
            onConfirm = { pin ->
                val success = container.appLockManager.unlockWithPin(pin)
                if (success) {
                    revealedNumber = account?.accountNumber
                    showAuthDialog = false
                }
                success
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete account?",
            message = "This will permanently remove this bank account from your vault.",
            confirmLabel = "Delete",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    account?.let { container.bankAccountRepository.delete(it) }
                    showDeleteDialog = false
                    onBack()
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.nickname ?: "Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(accountId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        account?.let { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(current.bankName, style = MaterialTheme.typography.titleMedium)
                        Text(current.accountType.displayName, style = MaterialTheme.typography.bodyMedium)
                        MaskedValueRow(
                            label = "Account Number",
                            maskedValue = maskAccountNumber(current.accountNumber),
                            revealedValue = revealedNumber,
                            onReveal = {
                                if (revealedNumber == null) requestReveal() else revealedNumber = null
                            },
                            onCopy = revealedNumber?.let { value ->
                                { clipboardHelper.copyWithAutoClear("Account number", value) }
                            },
                        )
                        Text("Holder: ${current.holderName}")
                        Text("IFSC/SWIFT: ${current.ifscOrSwift}")
                        current.notes?.let {
                            Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
