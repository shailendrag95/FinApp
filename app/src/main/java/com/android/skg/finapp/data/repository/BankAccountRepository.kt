package com.android.skg.finapp.data.repository

import com.android.skg.finapp.data.local.dao.BankAccountDao
import com.android.skg.finapp.data.local.entity.BankAccountEntity
import com.android.skg.finapp.domain.model.AccountType
import com.android.skg.finapp.domain.model.BankAccount
import com.android.skg.finapp.security.EncryptionHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BankAccountRepository(
    private val dao: BankAccountDao,
    private val encryptionHelper: EncryptionHelper,
) {
    fun observeAccounts(): Flow<List<BankAccount>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getAccount(id: Long): BankAccount? =
        dao.getById(id)?.toDomain()

    suspend fun getAllAccounts(): List<BankAccount> =
        dao.getAll().map { it.toDomain() }

    suspend fun insert(account: BankAccount): Long {
        val id = dao.insert(account.toEntity())
        return if (account.id == 0L) id else account.id
    }

    suspend fun update(account: BankAccount) {
        dao.update(account.toEntity())
    }

    suspend fun delete(account: BankAccount) {
        dao.delete(account.toEntity())
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun BankAccountEntity.toDomain() = BankAccount(
        id = id,
        nickname = nickname,
        bankName = bankName,
        accountNumber = encryptionHelper.decrypt(accountNumberEnc),
        holderName = holderName,
        ifscOrSwift = ifscOrSwift,
        accountType = AccountType.valueOf(accountType),
        notes = notes,
        createdAt = createdAt,
    )

    private fun BankAccount.toEntity() = BankAccountEntity(
        id = id,
        nickname = nickname,
        bankName = bankName,
        accountNumberEnc = encryptionHelper.encrypt(accountNumber),
        holderName = holderName,
        ifscOrSwift = ifscOrSwift,
        accountType = accountType.name,
        notes = notes,
        createdAt = createdAt,
    )
}
