package com.android.skg.finapp.ui.transactions

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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.android.skg.finapp.domain.model.Transaction
import com.android.skg.finapp.domain.model.TransactionCategory
import com.android.skg.finapp.ui.LocalAppContainer
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    cardId: Long,
    transactionId: Long?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    var amount by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<TransactionCategory?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            val tx = container.transactionRepository.getById(transactionId)
            existing = tx
            tx?.let {
                amount = it.amount.toString()
                merchant = it.merchant
                notes = it.notes.orEmpty()
                category = it.category
                selectedDateMillis = it.date
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (transactionId == null) "Add Transaction" else "Edit Transaction") })
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
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = dateFormat.format(Date(selectedDateMillis)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { showDatePicker = true }) { Text("Pick") }
                },
            )
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant / Description") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = category?.displayName ?: "None",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            category = null
                            categoryExpanded = false
                        },
                    )
                    TransactionCategory.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                category = option
                                categoryExpanded = false
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
                    val tx = Transaction(
                        id = existing?.id ?: 0,
                        cardId = cardId,
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        date = selectedDateMillis,
                        merchant = merchant.trim(),
                        category = category,
                        notes = notes.trim().ifBlank { null },
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    )
                    scope.launch {
                        if (existing == null) {
                            container.transactionRepository.insert(tx)
                        } else {
                            container.transactionRepository.update(tx)
                        }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && merchant.isNotBlank(),
            ) {
                Text("Save Transaction")
            }
            if (existing != null) {
                Button(
                    onClick = {
                        scope.launch {
                            existing?.let { container.transactionRepository.delete(it) }
                            onDone()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Delete Transaction")
                }
            }
        }
    }
}
