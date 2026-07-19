package com.example.finapp.ui.banks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finapp.domain.model.BankAccount
import com.example.finapp.ui.LocalAppContainer
import com.example.finapp.util.maskAccountNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BanksListScreen(
    onAddAccount: () -> Unit,
    onAccountClick: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val accounts by container.bankAccountRepository.observeAccounts().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Bank Accounts") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No bank accounts yet", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tap + to store account details securely.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(accounts, key = { it.id }) { account ->
                    BankListItem(account = account, onClick = { onAccountClick(account.id) })
                }
            }
        }
    }
}

@Composable
private fun BankListItem(account: BankAccount, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(account.nickname, style = MaterialTheme.typography.titleMedium)
            Text(account.bankName, style = MaterialTheme.typography.bodyMedium)
            Text(
                maskAccountNumber(account.accountNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(account.accountType.displayName, style = MaterialTheme.typography.labelMedium)
        }
    }
}
