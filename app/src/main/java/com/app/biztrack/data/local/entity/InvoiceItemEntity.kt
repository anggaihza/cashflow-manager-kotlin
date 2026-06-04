package com.app.biztrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    indices = [Index("invoiceId"), Index("inventoryItemId")],
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val inventoryItemId: Long? = null,
    val description: String,
    val quantity: Long,
    val unitPrice: Long,
    val totalAmount: Long,
)
