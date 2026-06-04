package com.app.biztrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debt_receivables")
data class DebtReceivableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val partyName: String,
    val totalAmount: Long,
    val paidAmount: Long = 0,
    val remainingAmount: Long = totalAmount - paidAmount,
    val dueDate: Long? = null,
    val status: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
