package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["userId", "category", "monthYear"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-09"
    val warningThresholdPercent: Double = 80.0 // when it turns orange
)
