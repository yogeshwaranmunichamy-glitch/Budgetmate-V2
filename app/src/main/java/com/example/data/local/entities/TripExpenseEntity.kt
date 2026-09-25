package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_expenses",
    indices = [
        Index("tripId"),
        Index("userId"),
        Index("category")
    ]
)
data class TripExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val userId: Long,
    val title: String, // e.g. "Beach Resort Stay", "Dinner at Thalassa", "Cab to Panjim"
    val amount: Double,
    val category: String, // "Stay & Hotel", "Flights & Travel", "Food & Dining", "Activities & Sightseeing", "Local Commute", "Shopping & Souvenirs", "Fuel", "Drinks & Party", "Emergency / Misc"
    val paidBy: String = "Me", // Companion name who paid the bill
    val splitAmong: String = "All", // Comma-separated list of members or "All"
    val date: Long,
    val paymentMethod: String = "UPI", // "UPI", "Cash", "Credit Card", "Debit Card", "Other"
    val notes: String = "",
    val isVoiceLogged: Boolean = false
)
