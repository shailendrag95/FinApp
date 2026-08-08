package com.android.skg.finapp.util

fun maskCardNumber(number: String): String {
    val digits = number.filter { it.isDigit() }
    if (digits.isEmpty()) return ""

    // If the number is short, mask everything except last 4
    if (digits.length <= 8) {
        val last = digits.takeLast(4)
        val masked = "*".repeat((digits.length - last.length).coerceAtLeast(0)) + last
        return masked.chunked(4).joinToString(" ")
    }

    // Keep first 4 and last 4 visible, mask the middle
    val first = digits.take(4)
    val last = digits.takeLast(4)
    val middleMasked = "*".repeat(digits.length - 8)
    val combined = (first + middleMasked + last)
    return combined.chunked(4).joinToString(" ")
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
