package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shop_invoices",
    indices = [Index("userId"), Index("partyId")]
)
data class ShopInvoiceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val invoiceNumber: String,
    val partyId: Long? = null,
    val partyName: String = "Walk-in Customer",
    val partyPhone: String = "",
    val type: String = "SALE", // "SALE" or "PURCHASE"
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxRate: Double = 0.0, // 0%, 5%, 12%, 18%, 28%
    val taxAmount: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentStatus: String = "PAID", // "PAID", "CREDIT_UDHAR", "PARTIAL"
    val paymentMode: String = "Cash", // "Cash", "UPI", "Bank Transfer", "Credit"
    val itemsJson: String = "[]", // [{"name":"Item A","qty":2.0,"unit":"Kg","rate":100.0,"total":200.0}]
    val notes: String = "",
    val date: Long = System.currentTimeMillis()
) {
    val isFullyPaid: Boolean
        get() = paidAmount >= grandTotal

    val balanceDue: Double
        get() = (grandTotal - paidAmount).coerceAtLeast(0.0)
}
