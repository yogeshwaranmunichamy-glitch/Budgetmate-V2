package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    indices = [Index("userId")]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val sourceOrMerchant: String,
    val frequency: String, // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    val nextDueDate: Long, // timestamp millis
    val notes: String = "",
    val isActive: Boolean = true
)
