package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_movements",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("userId")]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val itemId: Long,
    val type: String, // "STOCK_IN" or "STOCK_OUT"
    val quantity: Double,
    val unitPrice: Double = 0.0,
    val reason: String = "Sale", // "Sale", "Purchase", "Production Batch", "Damaged / Waste", "Adjustment"
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)
