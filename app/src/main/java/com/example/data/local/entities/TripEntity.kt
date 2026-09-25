package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [
        Index("userId"),
        Index("status")
    ]
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String, // e.g. "Goa Vacation 2026"
    val destination: String, // e.g. "Goa, India"
    val startDate: Long,
    val endDate: Long,
    val budget: Double,
    val currency: String = "INR", // "INR", "USD", "EUR"
    val companions: String = "Me", // Comma-separated list of travelers: e.g. "Me, Rahul, Priya, Arun"
    val status: String = "ACTIVE", // "UPCOMING", "ACTIVE", "COMPLETED"
    val notes: String = "",
    val coverEmoji: String = "🏖️" // Emoji like 🏖️, 🏔️, ✈️, 🌴, 🚗, 🏕️
)
