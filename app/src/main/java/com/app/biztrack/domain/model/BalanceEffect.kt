package com.app.biztrack.domain.model

data class BalanceDelta(
    val sourceDelta: Long,
    val targetDelta: Long = 0,
)

object BalanceEffect {
    fun apply(type: String, amount: Long): BalanceDelta =
        when (type) {
            TransactionTypes.INCOME -> BalanceDelta(sourceDelta = amount)
            TransactionTypes.EXPENSE -> BalanceDelta(sourceDelta = -amount)
            TransactionTypes.TRANSFER -> BalanceDelta(sourceDelta = -amount, targetDelta = amount)
            else -> BalanceDelta(sourceDelta = 0)
        }

    fun revert(type: String, amount: Long): BalanceDelta {
        val applied = apply(type, amount)
        return BalanceDelta(
            sourceDelta = -applied.sourceDelta,
            targetDelta = -applied.targetDelta,
        )
    }
}
