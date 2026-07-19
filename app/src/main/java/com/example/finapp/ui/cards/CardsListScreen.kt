package com.example.finapp.ui.cards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finapp.domain.model.CreditCard
import com.example.finapp.ui.LocalAppContainer
import com.example.finapp.util.formatCurrency
import com.example.finapp.util.maskCardNumber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsListScreen(
    onAddCard: () -> Unit,
    onCardClick: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    val cards by container.creditCardRepository.observeCards().collectAsState(initial = emptyList())

    Scaffold(
        topBar = { TopAppBar(title = { Text("Credit Cards") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCard) {
                Icon(Icons.Default.Add, contentDescription = "Add card")
            }
        },
    ) { padding ->
        if (cards.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("No cards yet", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Tap + to add your first credit card.",
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
                items(cards, key = { it.id }) { card ->
                    CardListItem(card = card, onClick = { onCardClick(card.id) })
                }
            }
        }
    }
}

@Composable
private fun CardListItem(card: CreditCard, onClick: () -> Unit) {
    val container = LocalAppContainer.current
    val cycleSpend by produceState(0.0, card.id, card.dueDateDay) {
        value = container.transactionRepository.getCycleSpend(card.id, card.dueDateDay)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(card.nickname, style = MaterialTheme.typography.titleMedium)
                if (card.isLTF) {
                    Text(
                        "LTF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                maskCardNumber(card.cardNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${card.network.displayName} · ${card.bank} · ${card.cardType.displayName}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "Cycle spend: ${formatCurrency(cycleSpend)}",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
