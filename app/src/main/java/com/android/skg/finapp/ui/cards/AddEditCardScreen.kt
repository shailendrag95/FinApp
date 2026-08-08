package com.android.skg.finapp.ui.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.android.skg.finapp.domain.model.CardNetwork
import com.android.skg.finapp.domain.model.CardType
import com.android.skg.finapp.domain.model.CreditCard
import com.android.skg.finapp.ui.LocalAppContainer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCardScreen(
    cardId: Long?,
    onDone: () -> Unit,
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var nickname by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var holderName by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var network by remember { mutableStateOf(CardNetwork.VISA) }
    var bank by remember { mutableStateOf("") }
    var dueDateDay by remember { mutableStateOf("") }
    var creditLimit by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var networkExpanded by remember { mutableStateOf(false) }
    var cardTypeExpanded by remember { mutableStateOf(false) }
    var isLTF by remember { mutableStateOf(false) }
    var cardType by remember { mutableStateOf(CardType.OTHER) }
    var existingCard by remember { mutableStateOf<CreditCard?>(null) }

    LaunchedEffect(cardId) {
        if (cardId != null) {
            val card = container.creditCardRepository.getCard(cardId)
            existingCard = card
            card?.let {
                nickname = it.nickname
                cardNumber = it.cardNumber
                holderName = it.holderName
                expiry = it.expiry
                cvv = it.cvv
                network = it.network
                bank = it.bank
                dueDateDay = it.dueDateDay?.toString().orEmpty()
                creditLimit = it.creditLimit?.toString().orEmpty()
                notes = it.notes.orEmpty()
                isLTF = it.isLTF
                cardType = it.cardType
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(if (cardId == null) "Add Card" else "Edit Card") })
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
                label = { Text("Card Nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 19) cardNumber = it.filter { c -> c.isDigit() || c == ' ' } },
                label = { Text("Card Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = holderName,
                onValueChange = { holderName = it },
                label = { Text("Cardholder Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ExposedDropdownMenuBox(
                    expanded = cardTypeExpanded,
                    onExpandedChange = { cardTypeExpanded = it },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = cardType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Card Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = cardTypeExpanded,
                        onDismissRequest = { cardTypeExpanded = false },
                    ) {
                        CardType.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    cardType = option
                                    cardTypeExpanded = false
                                },
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isLTF,
                        onCheckedChange = { isLTF = it },
                    )
                    Text("Lifetime Free", modifier = Modifier.padding(start = 4.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { if (it.length <= 4) expiry = it.filter { c -> c.isDigit() } },
                    label = { Text("Expiry (MMYY)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 4) cvv = it.filter { c -> c.isDigit() } },
                    label = { Text("CVV") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            ExposedDropdownMenuBox(
                expanded = networkExpanded,
                onExpandedChange = { networkExpanded = it },
            ) {
                OutlinedTextField(
                    value = network.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Card Network") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = networkExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = networkExpanded,
                    onDismissRequest = { networkExpanded = false },
                ) {
                    CardNetwork.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                network = option
                                networkExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = bank,
                onValueChange = { bank = it },
                label = { Text("Issuing Bank") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dueDateDay,
                    onValueChange = { if (it.length <= 2) dueDateDay = it.filter { c -> c.isDigit() } },
                    label = { Text("Due Date (day)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Credit Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
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
                    val card = CreditCard(
                        id = existingCard?.id ?: 0,
                        nickname = nickname.trim(),
                        cardNumber = cardNumber.filter { it.isDigit() },
                        holderName = holderName.trim(),
                        expiry = expiry,
                        cvv = cvv,
                        network = network,
                        bank = bank.trim(),
                        dueDateDay = dueDateDay.toIntOrNull()?.coerceIn(1, 28),
                        creditLimit = creditLimit.toDoubleOrNull(),
                        notes = notes.trim().ifBlank { null },
                        createdAt = existingCard?.createdAt ?: System.currentTimeMillis(),
                        isLTF = isLTF,
                        cardType = cardType,
                    )
                    scope.launch {
                        if (existingCard == null) {
                            container.creditCardRepository.insert(card)
                        } else {
                            container.creditCardRepository.update(card)
                        }
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = nickname.isNotBlank() && cardNumber.length >= 12 && holderName.isNotBlank() &&
                    expiry.length == 4 && cvv.length >= 3 && bank.isNotBlank(),
            ) {
                Text("Save Card")
            }
        }
    }
}
