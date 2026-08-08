package com.android.skg.finapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.android.skg.finapp.data.local.entity.BankAccountEntity
import com.android.skg.finapp.data.local.entity.CreditCardEntity
import com.android.skg.finapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY nickname ASC")
    fun observeAll(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards ORDER BY nickname ASC")
    suspend fun getAll(): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CreditCardEntity): Long

    @Update
    suspend fun update(entity: CreditCardEntity)

    @Delete
    suspend fun delete(entity: CreditCardEntity)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAll()
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY nickname ASC")
    fun observeAll(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts ORDER BY nickname ASC")
    suspend fun getAll(): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getById(id: Long): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BankAccountEntity): Long

    @Update
    suspend fun update(entity: BankAccountEntity)

    @Delete
    suspend fun delete(entity: BankAccountEntity)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    fun observeByCard(cardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId AND date BETWEEN :start AND :end")
    suspend fun getByCardInRange(cardId: Long, start: Long, end: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY date DESC")
    suspend fun getAllByCard(cardId: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TransactionEntity): Long

    @Update
    suspend fun update(entity: TransactionEntity)

    @Delete
    suspend fun delete(entity: TransactionEntity)

    @Query("DELETE FROM transactions WHERE cardId = :cardId")
    suspend fun deleteByCardId(cardId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
