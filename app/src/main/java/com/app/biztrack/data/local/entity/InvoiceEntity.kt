package com.app.biztrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoices",
    indices = [Index("invoiceNumber"), Index("status"), Index("contactId"), Index("dueDate")],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val contactId: Long?,
    val issueDate: Long,
    val dueDate: Long,
    val status: String,
    val totalAmount: Long,
    val paidAmount: Long = 0,
    val note: String = "",
    val transactionId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
