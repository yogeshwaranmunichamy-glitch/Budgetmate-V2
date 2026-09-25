package com.example.ml

import com.example.data.local.entities.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class InsightCard(
    val title: String,
    val description: String,
    val impact: String, // "POSITIVE", "ATTENTION", "NEUTRAL"
    val statValue: String
)

data class ComprehensiveSpendingAnalysis(
    val averageMonthlyExpense: Double,
    val averageTransactionAmount: Double,
    val highestCategory: String,
    val highestCategoryAmount: Double,
    val lowestCategory: String,
    val lowestCategoryAmount: Double,
    val weekendPercentage: Double,
    val weekdayPercentage: Double,
    val savingsRatePercentage: Double,
    val totalExpenseCount: Int,
    val insightsList: List<InsightCard>
)

object SpendingInsightsEngine {

    fun generateAnalysis(
        expenses: List<TransactionEntity>,
        incomes: List<TransactionEntity>
    ): ComprehensiveSpendingAnalysis {
        val expenseList = expenses.filter { it.type == "EXPENSE" }
        val totalExpense = expenseList.sumOf { it.amount }
        val totalIncome = incomes.filter { it.type == "INCOME" }.sumOf { it.amount }
        val count = expenseList.size

        if (count == 0) {
            return ComprehensiveSpendingAnalysis(
                averageMonthlyExpense = 0.0,
                averageTransactionAmount = 0.0,
                highestCategory = "None",
                highestCategoryAmount = 0.0,
                lowestCategory = "None",
                lowestCategoryAmount = 0.0,
                weekendPercentage = 0.0,
                weekdayPercentage = 0.0,
                savingsRatePercentage = 0.0,
                totalExpenseCount = 0,
                insightsList = listOf(
                    InsightCard(
                        title = "Getting Started",
                        description = "Add daily expenses and income to generate smart spending analytics and ML insights.",
                        impact = "NEUTRAL",
                        statValue = "0 Records"
                    )
                )
            )
        }

        val avgPerTransaction = totalExpense / count

        // Category breakdown
        val catMap = expenseList.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }
        val sortedCat = catMap.toList().sortedByDescending { it.second }
        val highest = sortedCat.firstOrNull() ?: Pair("None", 0.0)
        val lowest = sortedCat.lastOrNull() ?: Pair("None", 0.0)

        // Weekend vs Weekday analysis
        val cal = Calendar.getInstance()
        var weekendSum = 0.0
        var weekdaySum = 0.0
        for (e in expenseList) {
            cal.timeInMillis = e.date
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                weekendSum += e.amount
            } else {
                weekdaySum += e.amount
            }
        }
        val weekendPct = if (totalExpense > 0) (weekendSum / totalExpense) * 100.0 else 0.0
        val weekdayPct = if (totalExpense > 0) (weekdaySum / totalExpense) * 100.0 else 0.0

        // Savings rate
        val savings = (totalIncome - totalExpense).coerceAtLeast(0.0)
        val savingsRate = if (totalIncome > 0) (savings / totalIncome) * 100.0 else 0.0

        // Month-over-month category growth calculation
        val monthFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val monthsGroup = expenseList.groupBy { monthFmt.format(it.date) }.toSortedMap()
        val insights = mutableListOf<InsightCard>()

        if (monthsGroup.size >= 2) {
            val monthsList = monthsGroup.keys.toList()
            val currentMonthKey = monthsList.last()
            val prevMonthKey = monthsList[monthsList.size - 2]

            val currentMonthExpenses = monthsGroup[currentMonthKey] ?: emptyList()
            val prevMonthExpenses = monthsGroup[prevMonthKey] ?: emptyList()

            val curTopCat = currentMonthExpenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { e -> e.amount } }
            if (curTopCat != null) {
                val catName = curTopCat.key
                val curCatAmount = curTopCat.value.sumOf { it.amount }
                val prevCatAmount = prevMonthExpenses.filter { it.category == catName }.sumOf { it.amount }

                if (prevCatAmount > 0) {
                    val growth = ((curCatAmount - prevCatAmount) / prevCatAmount) * 100.0
                    val formattedGrowth = String.format("%.1f", kotlin.math.abs(growth))
                    if (growth > 10) {
                        insights.add(
                            InsightCard(
                                title = "$catName Spending Surge",
                                description = "$catName spending increased by $formattedGrowth% compared with last month.",
                                impact = "ATTENTION",
                                statValue = "+$formattedGrowth%"
                            )
                        )
                    } else if (growth < -10) {
                        insights.add(
                            InsightCard(
                                title = "$catName Spending Reduced",
                                description = "Good job! $catName spending decreased by $formattedGrowth% compared with last month.",
                                impact = "POSITIVE",
                                statValue = "-$formattedGrowth%"
                            )
                        )
                    }
                }
            }
        }

        // Weekend lifestyle insight
        if (weekendPct > 45.0) {
            insights.add(
                InsightCard(
                    title = "Weekend Spending Skew",
                    description = "Over ${String.format("%.0f", weekendPct)}% of your expenses happen on Saturday and Sunday. Consider setting weekend leisure limits.",
                    impact = "ATTENTION",
                    statValue = "${String.format("%.0f", weekendPct)}% on weekends"
                )
            )
        } else {
            insights.add(
                InsightCard(
                    title = "Balanced Weekly Spending",
                    description = "Your expenses are evenly distributed across weekdays (${String.format("%.0f", weekdayPct)}%) and weekends (${String.format("%.0f", weekendPct)}%).",
                    impact = "POSITIVE",
                    statValue = "Balanced"
                )
            )
        }

        // Top category dominance
        val topCatShare = if (totalExpense > 0) (highest.second / totalExpense) * 100.0 else 0.0
        insights.add(
            InsightCard(
                title = "Primary Cost Driver: ${highest.first}",
                description = "${highest.first} accounts for ${String.format("%.0f", topCatShare)}% of your total expenses (₹${String.format("%,.0f", highest.second)}).",
                impact = if (topCatShare > 40) "ATTENTION" else "NEUTRAL",
                statValue = "${String.format("%.0f", topCatShare)}% share"
            )
        )

        // Savings rate insight
        if (totalIncome > 0) {
            insights.add(
                InsightCard(
                    title = "Monthly Savings Efficiency",
                    description = if (savingsRate >= 20.0)
                        "Excellent financial discipline! You are saving ${String.format("%.1f", savingsRate)}% of your income."
                    else
                        "You are saving ${String.format("%.1f", savingsRate)}% of your income. The 50/30/20 rule recommends aiming for 20%.",
                    impact = if (savingsRate >= 20.0) "POSITIVE" else "ATTENTION",
                    statValue = "${String.format("%.1f", savingsRate)}% savings rate"
                )
            )
        }

        return ComprehensiveSpendingAnalysis(
            averageMonthlyExpense = if (monthsGroup.isNotEmpty()) totalExpense / monthsGroup.size else totalExpense,
            averageTransactionAmount = avgPerTransaction,
            highestCategory = highest.first,
            highestCategoryAmount = highest.second,
            lowestCategory = lowest.first,
            lowestCategoryAmount = lowest.second,
            weekendPercentage = weekendPct,
            weekdayPercentage = weekdayPct,
            savingsRatePercentage = savingsRate,
            totalExpenseCount = count,
            insightsList = insights
        )
    }
}
