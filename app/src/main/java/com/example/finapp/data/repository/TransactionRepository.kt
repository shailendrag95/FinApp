package com.example.finapp.data.repository

import com.example.finapp.data.local.dao.TransactionDao
import com.example.finapp.data.local.entity.TransactionEntity
import com.example.finapp.domain.model.Transaction
import com.example.finapp.domain.model.TransactionCategory
import com.example.finapp.util.billingCycleRange
import com.example.finapp.util.currentMonthRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TransactionRepository(
    private val dao: TransactionDao,
) {
    fun observeByCard(cardId: Long): Flow<List<Transaction>> =
        dao.observeByCard(cardId).map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.toDomain()

    suspend fun getAll(): List<Transaction> =
        dao.getAll().map { it.toDomain() }

    suspend fun getCycleSpend(cardId: Long, dueDateDay: Int?): Double {
        val range = billingCycleRange(dueDateDay) ?: currentMonthRange()
        return dao.getByCardInRange(cardId, range.first, range.second)
            .sumOf { it.amount }
    }

    suspend fun getFiltered(
        cardId: Long,
        startDate: Long?,
        endDate: Long?,
        category: TransactionCategory?,
    ): List<Transaction> {
        val transactions = dao.getAllByCard(cardId).map { it.toDomain() }
        return transactions.filter { tx ->
            val inRange = (startDate == null || tx.date >= startDate) &&
                (endDate == null || tx.date <= endDate)
            val inCategory = category == null || tx.category == category
            inRange && inCategory
        }
    }

    suspend fun insert(transaction: Transaction): Long {
        val id = dao.insert(transaction.toEntity())
        return if (transaction.id == 0L) id else transaction.id
    }

    suspend fun update(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    suspend fun delete(transaction: Transaction) {
        dao.delete(transaction.toEntity())
    }

    suspend fun deleteByCardId(cardId: Long) {
        dao.deleteByCardId(cardId)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        cardId = cardId,
        amount = amount,
        date = date,
        merchant = merchant,
        category = category?.let { TransactionCategory.valueOf(it) },
        notes = notes,
        createdAt = createdAt,
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id,
        cardId = cardId,
        amount = amount,
        date = date,
        merchant = merchant,
        category = category?.name,
        notes = notes,
        createdAt = createdAt,
    )
}
