package com.android.skg.finapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.skg.finapp.data.local.dao.BankAccountDao
import com.android.skg.finapp.data.local.dao.CreditCardDao
import com.android.skg.finapp.data.local.dao.TransactionDao
import com.android.skg.finapp.data.local.entity.BankAccountEntity
import com.android.skg.finapp.data.local.entity.CreditCardEntity
import com.android.skg.finapp.data.local.entity.TransactionEntity

@Database(
    entities = [CreditCardEntity::class, BankAccountEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class FinAppDatabase : RoomDatabase() {
    abstract fun creditCardDao(): CreditCardDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        fun create(context: Context): FinAppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                FinAppDatabase::class.java,
                "finapp.db",
            )
                .build()
        }
    }
}
