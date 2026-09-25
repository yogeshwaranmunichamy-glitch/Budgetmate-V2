package com.example.ml

import com.example.data.local.entities.TransactionEntity
import kotlin.math.pow
import kotlin.math.sqrt

data class AnomalyCheckResult(
    val isAnomaly: Boolean,
    val reason: String,
    val averageAmount: Double,
    val ratio: Double
)

object AnomalyDetectionEngine {

    /**
     * Checks whether an expense is statistically anomalous within its category
     * using Z-Score (Mean + 2.5 * Standard Deviation) and IQR methods.
     */
    fun checkAnomaly(
        newAmount: Double,
        category: String,
        historicalCategoryExpenses: List<TransactionEntity>
    ): AnomalyCheckResult {
        val amounts = historicalCategoryExpenses
            .filter { it.type == "EXPENSE" && it.category.equals(category, ignoreCase = true) }
            .map { it.amount }

        // If there's less than 3 historical transactions for this category, we can't reliably detect outliers
        if (amounts.size < 3) {
            return AnomalyCheckResult(
                isAnomaly = false,
                reason = "Not enough category history to detect anomalies",
                averageAmount = if (amounts.isNotEmpty()) amounts.average() else newAmount,
                ratio = 1.0
            )
        }

        val mean = amounts.average()
        val variance = amounts.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        // Z-score calculation
        val zScore = if (stdDev > 0) (newAmount - mean) / stdDev else 0.0

        // IQR method for robust outlier detection
        val sorted = amounts.sorted()
        val q1 = sorted[(sorted.size * 0.25).toInt()]
        val q3 = sorted[(sorted.size * 0.75).toInt()]
        val iqr = q3 - q1
        val iqrUpperLimit = q3 + (1.75 * iqr)

        val ratio = if (mean > 0) newAmount / mean else 1.0

        // Flag anomaly if Z-Score > 2.2 or exceeds IQR upper threshold with significant ratio (> 2.0x)
        val isOutlier = (zScore > 2.2 || newAmount > iqrUpperLimit) && ratio >= 2.0 && newAmount > 500.0

        val formattedAvg = String.format("%.0f", mean)
        val formattedRatio = String.format("%.1f", ratio)

        val reason = if (isOutlier) {
            "Unusual spending: ₹${newAmount.toInt()} is ${formattedRatio}x higher than your average $category expense (₹$formattedAvg)."
        } else {
            "Spending is within normal range for $category."
        }

        return AnomalyCheckResult(
            isAnomaly = isOutlier,
            reason = reason,
            averageAmount = mean,
            ratio = ratio
        )
    }
}
