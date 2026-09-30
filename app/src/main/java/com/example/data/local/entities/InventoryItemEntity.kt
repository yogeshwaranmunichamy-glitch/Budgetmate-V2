package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [Index("userId")]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val sku: String = "",
    val category: String = "Retail Goods", // "Raw Materials", "Finished Goods", "Retail Goods", "Spares & Tools", "Packaging"
    val unit: String = "Pcs", // "Pcs", "Kg", "Meter", "Liter", "Bag", "Box", "Dozen", "Ton"
    val purchasePrice: Double = 0.0,
    val sellingPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStockAlert: Double = 5.0,
    val hsnCode: String = "",
    val taxRate: Double = 0.0, // 0%, 5%, 12%, 18%, 28%
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = currentStock <= minStockAlert && currentStock > 0

    val isOutOfStock: Boolean
        get() = currentStock <= 0

    val stockValue: Double
        get() = currentStock * purchasePrice

    val retailValue: Double
        get() = currentStock * sellingPrice
}
