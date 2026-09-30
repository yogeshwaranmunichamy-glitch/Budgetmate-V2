package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "khata_entries",
    foreignKeys = [
        ForeignKey(
            entity = KhataPartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partyId"), Index("userId")]
)
data class KhataEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val partyId: Long,
    val type: String, // "GAVE" (Udhar / Credit) or "GOT" (Jama / Payment Received)
    val amount: Double,
    val description: String = "",
    val billNumber: String = "",
    val paymentMethod: String = "Cash", // "Cash", "UPI", "Bank Transfer", "Credit"
    val date: Long = System.currentTimeMillis()
)
