package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CurrencyFormatter {

    /**
     * Formats amounts using Indian numbering system:
     * e.g., ₹1,000, ₹25,000, ₹1,00,000, ₹10,00,000
     */
    fun formatINR(amount: Double, includeDecimals: Boolean = false): String {
        val symbols = DecimalFormatSymbols(Locale("en", "IN"))
        val pattern = if (includeDecimals) "##,##,##0.00" else "##,##,##0"
        val df = DecimalFormat(pattern, symbols)
        return "₹${df.format(amount)}"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getCurrentMonthYearString(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }
}
