package com.app.biztrack.utils

import java.text.NumberFormat
import java.util.Locale

fun formatMoney(amount: Long, currency: String = "IDR"): String {
    val locale = if (currency == "IDR") Locale.forLanguageTag("id-ID") else Locale.US
    return NumberFormat.getCurrencyInstance(locale).apply {
        this.currency = java.util.Currency.getInstance(currency)
        maximumFractionDigits = 0
    }.format(amount)
}

fun parseMoney(value: String): Long? {
    val digits = value.filter { it.isDigit() }
    return digits.toLongOrNull()
}
