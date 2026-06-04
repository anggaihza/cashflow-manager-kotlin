package com.app.biztrack.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_movements",
    indices = [Index("itemId")],
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val type: String,
    val quantity: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)
