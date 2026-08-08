package com.android.skg.finapp.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.android.skg.finapp.domain.model.CreditCard
import com.android.skg.finapp.domain.model.Transaction
import com.android.skg.finapp.domain.model.TransactionCategory
import com.android.skg.finapp.security.ClipboardHelper
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.ui.components.BiometricAuthHelper
import com.android.skg.finapp.ui.components.ConfirmDialog
import com.android.skg.finapp.ui.components.MaskedValueRow
import com.android.skg.finapp.ui.components.PinAuthDialog
import com.android.skg.finapp.util.formatCurrency
import com.android.skg.finapp.util.formatExpiry
import com.android.skg.finapp.util.maskCardNumber
import com.android.skg.finapp.util.maskCvv
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: Long,
    onBack: () -> Unit,
    onEditCard: (Long) -> Unit,
    onAddTransaction: (Long) -> Unit,
    onEditTransaction: (Long, Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val scope = rememberCoroutineScope()
    val clipboardHelper = remember { ClipboardHelper(context, scope) }

    var card by remember { mutableStateOf<CreditCard?>(null) }
    var revealedNumber by remember { mutableStateOf<String?>(null) }
    var revealedCvv by remember { mutableStateOf<String?>(null) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authTarget by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cascadeDelete by remember { mutableStateOf(true) }
    var cycleSpend by remember { mutableStateOf(0.0) }
    var biometricEnabled by remember { mutableStateOf(false) }

    val transactions by container.transactionRepository.observeByCard(cardId)
        .collectAsState(initial = emptyList())

    LaunchedEffect(cardId) {
        card = container.creditCardRepository.getCard(cardId)
        container.preferencesManager.biometricEnabled.collect { biometricEnabled = it }
    }

    LaunchedEffect(card) {
        card?.let {
            cycleSpend = container.transactionRepository.getCycleSpend(it.id, it.dueDateDay)
        }
    }

    fun requestReveal(target: String) {
        authTarget = target
        if (biometricEnabled && BiometricAuthHelper.canAuthenticate(activity)) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "Reveal sensitive data",
                subtitle = "Authenticate to view card details",
                onSuccess = {
                    when (target) {
                        "number" -> revealedNumber = card?.cardNumber
                        "cvv" -> revealedCvv = card?.cvv
                    }
                },
                onError = { showAuthDialog = true },
            )
        } else {
            showAuthDialog = true
        }
    }

    if (showAuthDialog) {
        PinAuthDialog(
            title = "Enter PIN to reveal",
            onDismiss = { showAuthDialog = false; authTarget = null },
            onConfirm = { pin ->
                val success = container.appLockManager.unlockWithPin(pin)
                if (success) {
                    when (authTarget) {
                        "number" -> revealedNumber = card?.cardNumber
                        "cvv" -> revealedCvv = card?.cvv
                    }
                    showAuthDialog = false
                    authTarget = null
                }
                success
            },
        )
    }

    if (showDeleteDialog && card != null) {
        ConfirmDialog(
            title = "Delete card?",
            message = if (cascadeDelete) {
                "This will delete the card and all its transactions."
            } else {
                "This will delete the card. Transactions will remain orphaned."
            },
            confirmLabel = "Delete",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    val current = card ?: return@launch
                    if (cascadeDelete) {
                        container.transactionRepository.deleteByCardId(current.id)
                    }
                    container.creditCardRepository.delete(current)
                    showDeleteDialog = false
                    onBack()
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(card?.nickname ?: "Card") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { card?.let { onEditCard(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTransaction(cardId) }) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
            }
        },
    ) { padding ->
        card?.let { current ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(current.network.displayName, style = MaterialTheme.typography.labelLarge)
                            Text(current.bank, style = MaterialTheme.typography.bodyMedium)
                            if (current.isLTF) {
                                Text(
                                    "✓ Lifetime Free Card",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                            Text(
                                "Card Type: ${current.cardType.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            MaskedValueRow(
                                label = "Card Number",
                                maskedValue = maskCardNumber(current.cardNumber),
                                revealedValue = revealedNumber,
                                onReveal = {
                                    if (revealedNumber == null) requestReveal("number")
                                    else revealedNumber = null
                                },
                                onCopy = revealedNumber?.let { value ->
                                    { clipboardHelper.copyWithAutoClear("Card number", value) }
                                },
                            )
                            Text("Holder: ${current.holderName}")
                            Text("Expiry: ${formatExpiry(current.expiry)}")
                            MaskedValueRow(
                                label = "CVV",
                                maskedValue = maskCvv(current.cvv),
                                revealedValue = revealedCvv,
                                onReveal = {
                                    if (revealedCvv == null) requestReveal("cvv")
                                    else revealedCvv = null
                                },
                            )
                            current.creditLimit?.let {
                                Text("Credit limit: ${formatCurrency(it)}")
                            }
                            current.dueDateDay?.let {
                                Text("Due date: day $it of month")
                            }
                            Text(
                                "Cycle spend: ${formatCurrency(cycleSpend)}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                            current.notes?.let {
                                Text("Notes: $it", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    Text("Transactions", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                if (transactions.isEmpty()) {
                    item {
                        Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRow(
                            transaction = tx,
                            onClick = { onEditTransaction(cardId, tx.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: Transaction, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(transaction.merchant, style = MaterialTheme.typography.titleSmall)
            Text(dateFormat.format(Date(transaction.date)))
            Text(
                formatCurrency(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            transaction.category?.let {
                Text(it.displayName, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
