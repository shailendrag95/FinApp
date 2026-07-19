package com.example.finapp.domain.model

enum class CardNetwork(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    RUPAY("RuPay"),
    AMEX("Amex"),
    OTHER("Other"),
}

enum class CardType(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    RUPAY("RuPay"),
    AMEX("American Express"),
    DINERS("Diners Club"),
    DISCOVER("Discover"),
    JCB("JCB"),
    OTHER("Other"),
}

enum class AccountType(val displayName: String) {
    SAVINGS("Savings"),
    CURRENT("Current"),
}

enum class TransactionCategory(val displayName: String) {
    FOOD("Food"),
    TRAVEL("Travel"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    OTHER("Other"),
}

data class CreditCard(
    val id: Long = 0,
    val nickname: String,
    val cardNumber: String,
    val holderName: String,
    val expiry: String,
    val cvv: String,
    val network: CardNetwork,
    val bank: String,
    val dueDateDay: Int? = null,
    val creditLimit: Double? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isLTF: Boolean = false,
    val cardType: CardType = CardType.OTHER,
)

data class BankAccount(
    val id: Long = 0,
    val nickname: String,
    val bankName: String,
    val accountNumber: String,
    val holderName: String,
    val ifscOrSwift: String,
    val accountType: AccountType,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Transaction(
    val id: Long = 0,
    val cardId: Long,
    val amount: Double,
    val date: Long,
    val merchant: String,
    val category: TransactionCategory?,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class CreditCardWithSpend(
    val card: CreditCard,
    val cycleSpend: Double,
    val transactionCount: Int,
)
