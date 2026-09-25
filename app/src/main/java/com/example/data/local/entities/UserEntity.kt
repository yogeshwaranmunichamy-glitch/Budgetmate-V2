package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val email: String,
    val passwordHash: String,
    val currencySymbol: String = "₹",
    val monthlyIncomeTarget: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)
