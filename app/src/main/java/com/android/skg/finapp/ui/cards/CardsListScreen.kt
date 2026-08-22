package com.android.skg.finapp.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.skg.finapp.domain.model.CardSortOrder
import com.android.skg.finapp.domain.model.CreditCard
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.util.formatExpiry
import com.android.skg.finapp.util.maskCardNumber

@Composable
private fun CardChip() {
    Box(
        modifier = Modifier
            .size(width = 42.dp, height = 32.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFFD700), Color(0xFFDAA520))
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .border(0.5.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val color = Color.Black.copy(alpha = 0.15f)
            val stroke = 1.dp.toPx()

            // Characteristic EMV chip patterns
            drawLine(color, Offset(w * 0.3f, 0f), Offset(w * 0.3f, h), stroke)
            drawLine(color, Offset(w * 0.7f, 0f), Offset(w * 0.7f, h), stroke)
            drawLine(color, Offset(0f, h * 0.4f), Offset(w, h * 0.4f), stroke)
            drawLine(color, Offset(0f, h * 0.6f), Offset(w, h * 0.6f), stroke)

            drawRect(color, Offset(w * 0.4f, h * 0.1f), Size(w * 0.2f, h * 0.8f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsListScreen(
    onAddCard: () -> Unit,
    onScanCard: () -> Unit,
    onCardClick: (Long) -> Unit,
) {
    val container = LocalAppContainer.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf(CardSortOrder.NICKNAME_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    val rawCards by if (searchQuery.isBlank()) {
        container.creditCardRepository.observeCards()
    } else {
        container.creditCardRepository.searchCards(searchQuery)
    }.collectAsState(initial = emptyList())

    val cards = remember(rawCards, sortOrder) {
        when (sortOrder) {
            CardSortOrder.NICKNAME_ASC -> rawCards.sortedBy { it.nickname.lowercase() }
            CardSortOrder.NICKNAME_DESC -> rawCards.sortedByDescending { it.nickname.lowercase() }
            CardSortOrder.BANK_ASC -> rawCards.sortedBy { it.bank.lowercase() }
            CardSortOrder.BANK_DESC -> rawCards.sortedByDescending { it.bank.lowercase() }
            CardSortOrder.NEWEST_ADDED -> rawCards.sortedByDescending { it.createdAt }
            CardSortOrder.EXPIRY_SOONEST -> rawCards.sortedBy { card ->
                // Parse MMYY to YYYYMM for correct chronological sorting
                if (card.expiry.length == 4) {
                    val month = card.expiry.substring(0, 2)
                    val year = card.expiry.substring(2, 4)
                    "20$year$month"
                } else "999999" // Move invalid/empty to the end
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search bank or nickname") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            }
                        )
                    } else {
                        Text("Credit Cards")
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = onScanCard) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan card"
                            )
                        }
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                CardSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(order.displayName) },
                                        onClick = {
                                            sortOrder = order
                                            showSortMenu = false
                                        },
                                        trailingIcon = {
                                            if (sortOrder == order) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        },
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
                if (searchQuery.isNotBlank()) {
                    Text("No results for \"$searchQuery\"", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Try a different search term.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text("No cards yet", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Tap + to add your first credit card.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                    Text(
                        card.nickname.replace(card.bank, "").trim(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Light,
                    )



                    Box(
                        modifier = Modifier
                            .background(
                                color = if (card.isLTF) Color.Green.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            if (card.isLTF) {
                                Text(
                                    "LTF",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            else {
                                Text(
                                    "Non-LTF",
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
                        CardChip()

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
                        Column {
                            Text(
                                card.holderName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = Color.White.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(6.dp),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    card.network.displayName.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

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

        networkName.contains("Rupay", ignoreCase = true) -> {
            Pair(Color(0xFFEB001B), Color(0xFF2B66AA))
        }

        else -> {
            Pair(Color(0xFF424242), Color(0xFF616161))
        }
    }
}
