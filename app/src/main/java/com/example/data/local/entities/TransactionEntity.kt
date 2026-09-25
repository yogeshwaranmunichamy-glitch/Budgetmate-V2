package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index("userId"),
        Index("date"),
        Index("category"),
        Index("type")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val paymentMethod: String, // "Cash", "UPI", "Credit Card", "Debit Card", "Bank Transfer", "Other"
    val sourceOrMerchant: String, // Merchant for expense, Source for income
    val date: Long, // timestamp in millis
    val notes: String = "",
    val isVoiceEntered: Boolean = false,
    val isReceiptScanned: Boolean = false,
    val confidenceScore: Float = 1.0f,
    val isAnomaly: Boolean = false,
    val anomalyReason: String = ""
)
