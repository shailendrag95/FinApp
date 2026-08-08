package com.android.skg.finapp.ui.banks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.skg.finapp.domain.model.AccountType
import com.android.skg.finapp.domain.model.BankAccount
import com.android.skg.finapp.ui.LocalAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBankScreen(
    accountId: Long?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var nickname by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var ifscOrSwift by remember { mutableStateOf("") }
    var accountType by remember { mutableStateOf(AccountType.SAVINGS) }
    var notes by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<BankAccount?>(null) }

    LaunchedEffect(accountId) {
        if (accountId != null) {
            val account = container.bankAccountRepository.getAccount(accountId)
            existing = account
            account?.let {
                nickname = it.nickname
                bankName = it.bankName
                accountNumber = it.accountNumber
                holderName = it.holderName
                ifscOrSwift = it.ifscOrSwift
                accountType = it.accountType
                notes = it.notes.orEmpty()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (accountId == null) "Add Account" else "Edit Account") })
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
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Account Nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = bankName,
                onValueChange = { bankName = it },
                label = { Text("Bank Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it.filter { c -> c.isDigit() } },
                label = { Text("Account Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Account Holder Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = ifscOrSwift,
                onValueChange = { ifscOrSwift = it.uppercase() },
                label = { Text("IFSC / SWIFT / Routing Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = it },
            ) {
                OutlinedTextField(
                    value = accountType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false },
                ) {
                    AccountType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                accountType = option
                                typeExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val account = BankAccount(
                        id = existing?.id ?: 0,
                        nickname = nickname.trim(),
                        bankName = bankName.trim(),
                        accountNumber = accountNumber,
                        holderName = holderName.trim(),
                        ifscOrSwift = ifscOrSwift.trim(),
                        accountType = accountType,
                        notes = notes.trim().ifBlank { null },
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    )
                    scope.launch {
                        if (existing == null) {
                            container.bankAccountRepository.insert(account)
                        } else {
                            container.bankAccountRepository.update(account)
                        }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nickname.isNotBlank() && bankName.isNotBlank() &&
                    accountNumber.length >= 4 && holderName.isNotBlank() && ifscOrSwift.isNotBlank(),
            ) {
                Text("Save Account")
            }
        }
    }
}
