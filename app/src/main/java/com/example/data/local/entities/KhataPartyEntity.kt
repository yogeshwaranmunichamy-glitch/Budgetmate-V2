package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "khata_parties")
data class KhataPartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val phone: String = "",
    val type: String = "CUSTOMER", // "CUSTOMER" or "SUPPLIER"
    val address: String = "",
    val currentBalance: Double = 0.0, // For Customer: >0 means they owe you (You'll Get); For Supplier: >0 means you owe them (You'll Give)
    val creditLimit: Double = 50000.0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastTransactionAt: Long = System.currentTimeMillis()
)
