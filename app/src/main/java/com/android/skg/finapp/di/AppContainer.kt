package com.android.skg.finapp.di

import android.content.Context
import com.android.skg.finapp.data.local.FinAppDatabase
import com.android.skg.finapp.data.repository.BankAccountRepository
import com.android.skg.finapp.data.repository.CreditCardRepository
import com.android.skg.finapp.data.repository.ExportRepository
import com.android.skg.finapp.data.repository.TransactionRepository
import com.android.skg.finapp.security.AppLockManager
import com.android.skg.finapp.security.DatabaseKeyManager
import com.android.skg.finapp.security.EncryptionHelper
import com.android.skg.finapp.security.PreferencesManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferencesManager = PreferencesManager(appContext)
    val databaseKeyManager = DatabaseKeyManager(appContext)
    val encryptionHelper = EncryptionHelper(appContext)
    val appLockManager = AppLockManager(appContext, preferencesManager)

    private val database = FinAppDatabase.create(appContext)

    val creditCardRepository = CreditCardRepository(
        dao = database.creditCardDao(),
        encryptionHelper = encryptionHelper,
    )
    val bankAccountRepository = BankAccountRepository(
        dao = database.bankAccountDao(),
        encryptionHelper = encryptionHelper,
    )
    val transactionRepository = TransactionRepository(database.transactionDao())
    val exportRepository = ExportRepository(
        creditCardRepository = creditCardRepository,
        bankAccountRepository = bankAccountRepository,
        transactionRepository = transactionRepository,
    )
}
