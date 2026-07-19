package com.example.finapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val cardNumberEnc: String,
    val holderName: String,
    val expiry: String,
    val cvvEnc: String,
    val network: String,
    val bank: String,
    val dueDateDay: Int?,
    val creditLimit: Double?,
    val notes: String?,
    val createdAt: Long,
    val isLTF: Boolean = false,
    val cardType: String = "OTHER",
)

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val bankName: String,
    val accountNumberEnc: String,
    val holderName: String,
    val ifscOrSwift: String,
    val accountType: String,
    val notes: String?,
    val createdAt: Long,
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val amount: Double,
    val date: Long,
    val merchant: String,
    val category: String?,
    val notes: String?,
    val createdAt: Long,
)
