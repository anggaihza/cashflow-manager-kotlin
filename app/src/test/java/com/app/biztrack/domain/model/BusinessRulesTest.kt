package com.app.biztrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BusinessRulesTest {
    @Test
    fun enoughBalancePasses() {
        BusinessRules.requireEnoughBalance("Cash", currentBalance = 100_000, amount = 99_000)
    }

    @Test
    fun insufficientBalanceFailsWithAccountName() {
        val error = runCatching {
            BusinessRules.requireEnoughBalance("Bank", currentBalance = 10_000, amount = 15_000)
        }.exceptionOrNull()

        assertEquals("Saldo Bank tidak cukup untuk transaksi ini.", error?.message)
    }

    @Test
    fun enoughStockPasses() {
        BusinessRules.requireEnoughStock("Produk A", currentStock = 5, quantity = 5)
    }

    @Test
    fun insufficientStockFailsWithItemName() {
        val error = runCatching {
            BusinessRules.requireEnoughStock("Produk B", currentStock = 2, quantity = 3)
        }.exceptionOrNull()

        assertEquals("Stok Produk B tidak cukup.", error?.message)
    }
}
