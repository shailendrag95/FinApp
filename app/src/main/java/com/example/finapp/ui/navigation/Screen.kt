package com.example.finapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object SetupLock : Screen("setup_lock")
    data object Lock : Screen("lock")
    data object Cards : Screen("cards")
    data object Banks : Screen("banks")
    data object Settings : Screen("settings")
    data object CardAdd : Screen("card_add")
    data object CardEdit : Screen("card_edit/{cardId}") {
        fun createRoute(cardId: Long) = "card_edit/$cardId"
    }
    data object CardDetail : Screen("card_detail/{cardId}") {
        fun createRoute(cardId: Long) = "card_detail/$cardId"
    }
    data object BankAdd : Screen("bank_add")
    data object BankEdit : Screen("bank_edit/{accountId}") {
        fun createRoute(accountId: Long) = "bank_edit/$accountId"
    }
    data object BankDetail : Screen("bank_detail/{accountId}") {
        fun createRoute(accountId: Long) = "bank_detail/$accountId"
    }
    data object TransactionAdd : Screen("transaction_add/{cardId}") {
        fun createRoute(cardId: Long) = "transaction_add/$cardId"
    }
    data object TransactionEdit : Screen("transaction_edit/{cardId}/{transactionId}") {
        fun createRoute(cardId: Long, transactionId: Long) = "transaction_edit/$cardId/$transactionId"
    }
    data object ExportImport : Screen("export_import")
    data object CardTypeManager : Screen("card_type_manager")
}

enum class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
) {
    Cards(Screen.Cards, "Cards", Icons.Default.CreditCard),
    Banks(Screen.Banks, "Accounts", Icons.Default.AccountBalance),
    Settings(Screen.Settings, "Settings", Icons.Default.Settings),
}
