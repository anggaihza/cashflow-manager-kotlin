package com.app.biztrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("date"),
        Index("type"),
        Index("categoryId"),
        Index("cashAccountId"),
        Index("paymentMethodId"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val date: Long,
    val categoryId: Long? = null,
    val cashAccountId: Long,
    val targetCashAccountId: Long? = null,
    val paymentMethodId: Long? = null,
    val note: String = "",
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
