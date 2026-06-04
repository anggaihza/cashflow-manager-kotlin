package com.app.biztrack.domain.model

object BusinessRules {
    fun requireEnoughBalance(accountName: String, currentBalance: Long, amount: Long) {
        require(currentBalance >= amount) {
            "Saldo $accountName tidak cukup untuk transaksi ini."
        }
    }

    fun requireEnoughStock(itemName: String, currentStock: Long, quantity: Long) {
        require(currentStock >= quantity) {
            "Stok $itemName tidak cukup."
        }
    }
}
