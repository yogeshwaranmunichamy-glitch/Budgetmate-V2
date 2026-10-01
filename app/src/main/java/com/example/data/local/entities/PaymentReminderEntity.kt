package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_reminders",
    indices = [
        Index("userId"),
        Index("dueDate"),
        Index("status"),
        Index("reminderType")
    ]
)
data class PaymentReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String, // e.g. "Ramesh Udhar Repayment", "HDFC Car Loan EMI", "Shop Rent"
    val personOrEntity: String, // e.g. "Ramesh Kumar", "HDFC Bank", "Landlord", "Suresh"
    val amount: Double,
    val reminderType: String, // "TO_COLLECT" (Udhar / Receivable), "TO_PAY" (Loan / EMI / Payable), "BILL" (Utility / Credit Card)
    val dueDate: Long, // Epoch millis timestamp
    val reminderTime: String = "09:00 AM", // Formatted time
    val isAlarmEnabled: Boolean = true, // Play alarm sound & notify
    val status: String = "PENDING", // "PENDING", "COMPLETED", "SNOOZED"
    val priority: String = "HIGH", // "NORMAL", "HIGH", "URGENT"
    val phoneNumber: String = "", // Contact phone for WhatsApp / SMS nudge
    val notes: String = "", // e.g. "Borrowed 500 for grocery, promised to return"
    val repeatInterval: String = "NONE", // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
