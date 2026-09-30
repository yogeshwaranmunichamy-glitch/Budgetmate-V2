package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_cash_registers",
    indices = [Index("userId"), Index("date")]
)
data class DailyCashRegisterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long = System.currentTimeMillis(),
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val cashKhataCollected: Double = 0.0,
    val cashShopExpenses: Double = 0.0,
    val cashSupplierPaid: Double = 0.0,
    val upiCollected: Double = 0.0,
    val bankCollected: Double = 0.0,
    val actualClosingCash: Double = 0.0,
    val notes: String = "",
    val isClosed: Boolean = false
) {
    val totalCashInflow: Double
        get() = cashSales + cashKhataCollected

    val totalCashOutflow: Double
        get() = cashShopExpenses + cashSupplierPaid

    val expectedClosingCash: Double
        get() = openingCash + totalCashInflow - totalCashOutflow

    val cashDiscrepancy: Double
        get() = actualClosingCash - expectedClosingCash
}
