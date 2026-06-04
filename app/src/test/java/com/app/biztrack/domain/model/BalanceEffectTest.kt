package com.app.biztrack.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BalanceEffectTest {
    @Test
    fun incomeAddsToSourceAccount() {
        val delta = BalanceEffect.apply(TransactionTypes.INCOME, 125_000)

        assertEquals(125_000, delta.sourceDelta)
        assertEquals(0, delta.targetDelta)
    }

    @Test
    fun expenseSubtractsFromSourceAccount() {
        val delta = BalanceEffect.apply(TransactionTypes.EXPENSE, 75_000)

        assertEquals(-75_000, delta.sourceDelta)
        assertEquals(0, delta.targetDelta)
    }

    @Test
    fun transferMovesBalanceWithoutChangingTotal() {
        val delta = BalanceEffect.apply(TransactionTypes.TRANSFER, 200_000)

        assertEquals(-200_000, delta.sourceDelta)
        assertEquals(200_000, delta.targetDelta)
        assertEquals(0, delta.sourceDelta + delta.targetDelta)
    }

    @Test
    fun revertFlipsAppliedEffect() {
        val delta = BalanceEffect.revert(TransactionTypes.EXPENSE, 50_000)

        assertEquals(50_000, delta.sourceDelta)
        assertEquals(0, delta.targetDelta)
    }
}
