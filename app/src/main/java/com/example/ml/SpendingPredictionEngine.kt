package com.example.ml

import com.example.data.local.entities.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CategoryPrediction(
    val category: String,
    val estimatedNextMonthSpending: Double,
    val confidenceGrade: String, // "High", "Moderate", "Low"
    val trendDirection: String // "Increasing", "Decreasing", "Stable"
)

data class OverallSpendingPrediction(
    val estimatedTotalNextMonth: Double,
    val monthlyGrowthRatePercent: Double,
    val categoryPredictions: List<CategoryPrediction>,
    val explanation: String
)

object SpendingPredictionEngine {

    /**
     * Estimates future month spending using historical monthly aggregation,
     * linear regression trend slope, and exponential moving average (alpha=0.6).
     */
    fun predictNextMonthSpending(allExpenses: List<TransactionEntity>): OverallSpendingPrediction {
        if (allExpenses.isEmpty()) {
            return OverallSpendingPrediction(
                estimatedTotalNextMonth = 0.0,
                monthlyGrowthRatePercent = 0.0,
                categoryPredictions = emptyList(),
                explanation = "Add transactions across multiple months to enable ML spending forecasting."
            )
        }

        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
        val groupedByMonth = allExpenses
            .filter { it.type == "EXPENSE" }
            .groupBy { monthFormat.format(it.date) }
            .toSortedMap()

        val monthlyTotals = groupedByMonth.values.map { list -> list.sumOf { it.amount } }

        // Linear Regression & EMA
        val estimatedTotal = if (monthlyTotals.size >= 2) {
            val n = monthlyTotals.size
            var sumX = 0.0
            var sumY = 0.0
            var sumXY = 0.0
            var sumX2 = 0.0
            for (i in 0 until n) {
                val x = i.toDouble()
                val y = monthlyTotals[i]
                sumX += x
                sumY += y
                sumXY += x * y
                sumX2 += x * x
            }
            val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX).coerceAtLeast(0.0001)
            val intercept = (sumY - slope * sumX) / n

            // Projected next month (x = n)
            val linearForecast = (slope * n + intercept).coerceAtLeast(0.0)

            // Exponential Moving Average
            var ema = monthlyTotals.first()
            val alpha = 0.6
            for (valTotal in monthlyTotals.drop(1)) {
                ema = alpha * valTotal + (1 - alpha) * ema
            }

            // Blended model (50% Linear + 50% EMA)
            (linearForecast * 0.5 + ema * 0.5)
        } else {
            monthlyTotals.lastOrNull() ?: 0.0
        }

        // Calculate Category Predictions
        val groupedByCategory = allExpenses
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }

        val categoryPredictions = mutableListOf<CategoryPrediction>()
        for ((cat, catExpenses) in groupedByCategory) {
            val catMonthly = catExpenses.groupBy { monthFormat.format(it.date) }.values.map { it.sumOf { exp -> exp.amount } }
            val catEstimate = if (catMonthly.size >= 2) {
                val recent = catMonthly.takeLast(2)
                val diff = recent[1] - recent[0]
                (recent[1] + (diff * 0.5)).coerceAtLeast(catMonthly.average() * 0.5)
            } else {
                catMonthly.average()
            }

            val trend = when {
                catMonthly.size < 2 -> "Stable"
                catMonthly.last() > catMonthly.first() * 1.10 -> "Increasing"
                catMonthly.last() < catMonthly.first() * 0.90 -> "Decreasing"
                else -> "Stable"
            }

            val confidence = when {
                catExpenses.size >= 10 -> "High"
                catExpenses.size >= 4 -> "Moderate"
                else -> "Low"
            }

            categoryPredictions.add(
                CategoryPrediction(
                    category = cat,
                    estimatedNextMonthSpending = catEstimate,
                    confidenceGrade = confidence,
                    trendDirection = trend
                )
            )
        }

        categoryPredictions.sortByDescending { it.estimatedNextMonthSpending }

        val lastMonthTotal = monthlyTotals.lastOrNull() ?: 1.0
        val growthRate = if (lastMonthTotal > 0) ((estimatedTotal - lastMonthTotal) / lastMonthTotal) * 100.0 else 0.0

        val formattedTotal = String.format("₹%,.0f", estimatedTotal)
        val formattedGrowth = String.format("%+.1f%%", growthRate)

        val explanation = "Based on your spending patterns across ${monthlyTotals.size} recorded periods, estimated next month spending is $formattedTotal ($formattedGrowth trend). Predictions are statistical estimates based on your local historical data."

        return OverallSpendingPrediction(
            estimatedTotalNextMonth = estimatedTotal,
            monthlyGrowthRatePercent = growthRate,
            categoryPredictions = categoryPredictions,
            explanation = explanation
        )
    }
}
