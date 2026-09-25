package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_scans",
    indices = [Index("userId")]
)
data class ReceiptScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val merchant: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val rawText: String,
    val timestamp: Long = System.currentTimeMillis()
)
