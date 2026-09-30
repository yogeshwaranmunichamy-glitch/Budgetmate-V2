package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shop_profiles")
data class ShopProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val businessName: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val gstin: String = "",
    val businessType: String = "Kirana & Retail", // "Kirana & Retail", "Small Scale Industry / Workshop", "Garments", "Hardware & Electricals", "Food & Restaurant", "Wholesale & Spares", "Services"
    val openingCash: Double = 0.0,
    val isGstEnabled: Boolean = false,
    val upiId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
