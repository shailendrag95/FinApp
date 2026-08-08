package com.android.skg.finapp.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.skg.finapp.domain.model.CreditCard
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.util.formatExpiry
import com.android.skg.finapp.util.maskCardNumber

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
    // Define card colors based on card network
    val (primaryColor, accentColor) = getCardColors(card.network.displayName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryColor, accentColor),
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top row with card name and LTF badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        card.bank,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Light,
                    )
                    if (card.isLTF) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "LTF",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Card chip, number and a small network/type badge
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // chip visualization
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .background(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(5.dp),
                                )
                        )

                        // card type small badge on right
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(6.dp),
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                card.cardType.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.85f,
                            )
                        }
                    }

                    // Masked card number, show first 4 and last 4
                    Text(
                        maskCardNumber(card.cardNumber),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Bottom row with card holder name, expiry and cycle spend
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            card.holderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "EXPIRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Light,
                            )
                            Text(
                                formatExpiry(card.expiry),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCardColors(networkName: String): Pair<Color, Color> {
    return when {
        networkName.contains("Visa", ignoreCase = true) -> {
            Pair(Color(0xFF1434CB), Color(0xFF00A8E8))
        }

        networkName.contains("Mastercard", ignoreCase = true) -> {
            Pair(Color(0xFFFF5F00), Color(0xFFEB001B))
        }

        networkName.contains("Amex", ignoreCase = true) -> {
            Pair(Color(0xFF006FCF), Color(0xFF00A9CE))
        }

        networkName.contains("Rupay", ignoreCase = true) -> {
            Pair(Color(0xFF003DA5), Color(0xFF2B66AA))
        }

        else -> {
            Pair(Color(0xFF424242), Color(0xFF616161))
        }
    }
}
