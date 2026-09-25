package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ml_feedback",
    indices = [Index("userId")]
)
data class MLFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val rawText: String,
    val predictedCategory: String,
    val correctedCategory: String,
    val timestamp: Long = System.currentTimeMillis()
)
