package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trip_plan_items",
    indices = [
        Index("tripId"),
        Index("userId")
    ]
)
data class TripPlanItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tripId: Long,
    val userId: Long,
    val title: String, // e.g. "Book Resort", "Rent Scooter", "Scuba Diving Slot"
    val estimatedCost: Double = 0.0,
    val isDone: Boolean = false,
    val dayNumber: Int = 0, // 0 for Pre-trip, 1 for Day 1, 2 for Day 2, etc.
    val notes: String = ""
)
