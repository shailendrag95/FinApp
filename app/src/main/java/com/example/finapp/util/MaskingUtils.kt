package com.example.finapp.util

fun maskCardNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    if (digits.length < 4) return "****"
    return "**** **** **** ${digits.takeLast(4)}"
}

fun maskAccountNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    if (digits.length < 4) return "****"
    return "****${digits.takeLast(4)}"
}

fun maskCvv(cvv: String): String = "*".repeat(cvv.length.coerceAtLeast(3))

fun formatCurrency(amount: Double): String = "₹%,.2f".format(amount)

fun formatExpiry(expiry: String): String {
    val digits = expiry.filter { it.isDigit() }
    return when {
        digits.length >= 4 -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}"
        else -> expiry
    }
}
