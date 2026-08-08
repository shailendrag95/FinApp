package com.android.skg.finapp.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.skg.finapp.ui.LocalAppContainer
import com.android.skg.finapp.ui.banks.AddEditBankScreen
import com.android.skg.finapp.ui.banks.BankDetailScreen
import com.android.skg.finapp.ui.banks.BanksListScreen
import com.android.skg.finapp.ui.cards.AddEditCardScreen
import com.android.skg.finapp.ui.cards.CardDetailScreen
import com.android.skg.finapp.ui.cards.CardsListScreen
import com.android.skg.finapp.ui.export.ExportImportScreen
import com.android.skg.finapp.ui.lock.LockScreen
import com.android.skg.finapp.ui.lock.SetupLockScreen
import com.android.skg.finapp.ui.settings.SettingsScreen
import com.android.skg.finapp.ui.settings.CardTypeManagerScreen
import com.android.skg.finapp.ui.transactions.AddEditTransactionScreen
import kotlinx.coroutines.flow.first

@Composable
fun FinAppRoot() {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val activity = context as FragmentActivity
    val navController = rememberNavController()

    var startDestination by remember { mutableStateOf<String?>(null) }
    var biometricEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        biometricEnabled = container.preferencesManager.biometricEnabled.first()
        startDestination = when {
            !container.appLockManager.isPinSet -> Screen.SetupLock.route
            !container.appLockManager.isUnlocked -> Screen.Lock.route
            else -> Screen.Cards.route
        }
    }

    val destination = startDestination ?: return

    NavHost(navController = navController, startDestination = destination) {
        composable(Screen.SetupLock.route) {
            SetupLockScreen(
                appLockManager = container.appLockManager,
                biometricAvailable = com.android.skg.finapp.ui.components.BiometricAuthHelper
                    .canAuthenticate(activity),
                onSetupComplete = {
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.SetupLock.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Lock.route) {
            LockScreen(
                activity = activity,
                appLockManager = container.appLockManager,
                biometricEnabled = biometricEnabled,
                onUnlocked = {
                    navController.navigate(Screen.Cards.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Cards.route) {
            MainScaffold(
                currentRoute = Screen.Cards.route,
                navController = navController,
            ) {
                CardsListScreen(
                    onAddCard = { navController.navigate(Screen.CardAdd.route) },
                    onCardClick = { navController.navigate(Screen.CardDetail.createRoute(it)) },
                )
            }
        }
        composable(Screen.Banks.route) {
            MainScaffold(
                currentRoute = Screen.Banks.route,
                navController = navController,
            ) {
                BanksListScreen(
                    onAddAccount = { navController.navigate(Screen.BankAdd.route) },
                    onAccountClick = { navController.navigate(Screen.BankDetail.createRoute(it)) },
                )
            }
        }
        composable(Screen.Settings.route) {
            MainScaffold(
                currentRoute = Screen.Settings.route,
                navController = navController,
            ) {
                SettingsScreen(
                    onExportImport = { navController.navigate(Screen.ExportImport.route) },
                    onCardTypeManager = { navController.navigate(Screen.CardTypeManager.route) },
                )
            }
        }
        composable(
            route = Screen.CardDetail.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            CardDetailScreen(
                cardId = cardId,
                onBack = { navController.popBackStack() },
                onEditCard = { navController.navigate(Screen.CardEdit.createRoute(it)) },
                onAddTransaction = { navController.navigate(Screen.TransactionAdd.createRoute(it)) },
                onEditTransaction = { cId, txId ->
                    navController.navigate(Screen.TransactionEdit.createRoute(cId, txId))
                },
            )
        }
        composable(Screen.CardAdd.route) {
            AddEditCardScreen(cardId = null, onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.CardEdit.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            AddEditCardScreen(cardId = cardId, onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.BankDetail.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId") ?: return@composable
            BankDetailScreen(
                accountId = accountId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.BankEdit.createRoute(it)) },
            )
        }
        composable(Screen.BankAdd.route) {
            AddEditBankScreen(accountId = null, onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.BankEdit.route,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
        ) { entry ->
            val accountId = entry.arguments?.getLong("accountId") ?: return@composable
            AddEditBankScreen(accountId = accountId, onDone = { navController.popBackStack() })
        }
        composable(
            route = Screen.TransactionAdd.route,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            AddEditTransactionScreen(
                cardId = cardId,
                transactionId = null,
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.TransactionEdit.route,
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType },
                navArgument("transactionId") { type = NavType.LongType },
            ),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId") ?: return@composable
            val transactionId = entry.arguments?.getLong("transactionId") ?: return@composable
            AddEditTransactionScreen(
                cardId = cardId,
                transactionId = transactionId,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Screen.ExportImport.route) {
            ExportImportScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CardTypeManager.route) {
            CardTypeManagerScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(
    currentRoute: String,
    navController: androidx.navigation.NavHostController,
    content: @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        selected = route == item.screen.route,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.Cards.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}
