package com.example.finapp.di

import android.content.Context
import com.example.finapp.data.local.FinAppDatabase
import com.example.finapp.data.repository.BankAccountRepository
import com.example.finapp.data.repository.CreditCardRepository
import com.example.finapp.data.repository.ExportRepository
import com.example.finapp.data.repository.TransactionRepository
import com.example.finapp.security.AppLockManager
import com.example.finapp.security.DatabaseKeyManager
import com.example.finapp.security.EncryptionHelper
import com.example.finapp.security.PreferencesManager

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
