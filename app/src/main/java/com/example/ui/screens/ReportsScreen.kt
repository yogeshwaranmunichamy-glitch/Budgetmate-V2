package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.TransactionEntity
import com.example.ml.AnomalyDetectionEngine
import com.example.ui.components.CategoryDoughnutChart
import com.example.ui.components.SpendingBarChart
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: BudgetMateViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val adUnlocked by viewModel.adUnlockedForSession.collectAsState()

    var selectedPeriod by remember { mutableStateOf("MONTHLY") } // "WEEKLY", "MONTHLY", "YEARLY"
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Summary & Category, 1 = ML Insights & Forecast

    // Calculate dates based on selected period
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()

    val filteredExpenses = remember(transactions, selectedPeriod) {
        val windowStart = Calendar.getInstance().apply {
            when (selectedPeriod) {
                "WEEKLY" -> add(Calendar.DAY_OF_YEAR, -7)
                "MONTHLY" -> add(Calendar.DAY_OF_YEAR, -30)
                "YEARLY" -> add(Calendar.DAY_OF_YEAR, -365)
            }
        }.timeInMillis

        transactions.filter { it.date >= windowStart }
    }

    val periodExpenses = filteredExpenses.filter { it.type == "EXPENSE" }
    val periodIncomes = filteredExpenses.filter { it.type == "INCOME" }
    val totalExpense = periodExpenses.sumOf { it.amount }
    val totalIncome = periodIncomes.sumOf { it.amount }
    val savings = (totalIncome - totalExpense).coerceAtLeast(0.0)

    val categoryBreakdown = periodExpenses.groupBy { it.category }.mapValues { it.value.sumOf { t -> t.amount } }

    val analysis = viewModel.getSpendingAnalysis()
    val prediction = viewModel.getSpendingPrediction()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Header & Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Financial Reports",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Deterministic ML statistical analytics & forecasts",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { exportTransactionsToCsv(context, transactions) },
                    modifier = Modifier.height(36.dp).testTag("export_csv_button")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export CSV", fontSize = 11.sp)
                }
            }
        }

        // Period Selection Chips (Weekly, Monthly, Yearly)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("WEEKLY" to "Weekly Summary", "MONTHLY" to "Monthly Summary", "YEARLY" to "Yearly Summary").forEach { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = {
                            if (!adUnlocked) {
                                viewModel.requestSummaryOrInsightsWithAd(label) {
                                    selectedPeriod = key
                                }
                            } else {
                                selectedPeriod = key
                            }
                        },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.weight(1f).testTag("period_chip_$key")
                    )
                }
            }
        }

        // Sub tabs: Summary Breakdown vs ML Insights & Prediction
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Category Summary", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        if (!adUnlocked) {
                            viewModel.requestSummaryOrInsightsWithAd("Detailed Spending Insights") {
                                selectedTab = 1
                            }
                        } else {
                            selectedTab = 1
                        }
                    },
                    text = { Text("ML Insights & Forecast", fontWeight = FontWeight.Bold) }
                )
            }
        }

        if (selectedTab == 0) {
            // Period Total Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("period_summary_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = when (selectedPeriod) {
                                "WEEKLY" -> "Past 7 Days Overview"
                                "YEARLY" -> "Past 12 Months Overview"
                                else -> "Past 30 Days Overview"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Income", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatINR(totalIncome), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatINR(totalExpense), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(CurrencyFormatter.formatINR(savings), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            }
                        }
                    }
                }
            }

            // Category Wise Expenses Doughnut Chart
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Category Spending ($selectedPeriod)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CategoryDoughnutChart(categoryData = categoryBreakdown)
                    }
                }
            }

            // Category Table List
            item {
                Text(
                    text = "Category Breakdown Table",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            items(categoryBreakdown.entries.toList().sortedByDescending { it.value }) { entry ->
                val pct = if (totalExpense > 0) (entry.value / totalExpense) * 100 else 0.0
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(entry.key, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${String.format("%.1f", pct)}% of total expenses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(CurrencyFormatter.formatINR(entry.value), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        } else {
            // ML Insights & Future Expense Forecast Tab
            if (!adUnlocked) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ML Insights & Prediction Locked", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Watch a short sponsor partner message to unlock full AI spending analysis.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    viewModel.requestSummaryOrInsightsWithAd("Spending Insights & Prediction") {
                                        selectedTab = 1
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Unlock Insights Now")
                            }
                        }
                    }
                }
            } else {
                // Next Month Spending Prediction Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("prediction_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Estimated Next Month Expense",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = CurrencyFormatter.formatINR(prediction.estimatedTotalNextMonth),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = prediction.explanation,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Category Spending Projections
                item {
                    Text("Category Forecasts (Estimates)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                items(prediction.categoryPredictions.take(5)) { cp ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(cp.category, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Trend: ${cp.trendDirection} • Confidence: ${cp.confidenceGrade}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "Est: ${CurrencyFormatter.formatINR(cp.estimatedNextMonthSpending)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                // Factual ML Observations Cards
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Spending Behavioral Insights", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                items(analysis.insightsList) { insight ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(insight.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            when (insight.impact) {
                                                "POSITIVE" -> IncomeGreen.copy(alpha = 0.15f)
                                                "ATTENTION" -> AnomalyWarning.copy(alpha = 0.15f)
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = insight.statValue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (insight.impact) {
                                            "POSITIVE" -> IncomeGreen
                                            "ATTENTION" -> AnomalyWarning
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(insight.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

private fun exportTransactionsToCsv(context: Context, transactions: List<TransactionEntity>) {
    val builder = StringBuilder()
    builder.append("ID,Type,Amount,Category,PaymentMethod,Merchant,Date,Notes\n")
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    for (tx in transactions) {
        val cleanNotes = tx.notes.replace(",", " ")
        val cleanMerchant = tx.sourceOrMerchant.replace(",", " ")
        builder.append("${tx.id},${tx.type},${tx.amount},${tx.category},${tx.paymentMethod},\"$cleanMerchant\",${sdf.format(tx.date)},\"$cleanNotes\"\n")
    }

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, builder.toString())
        putExtra(Intent.EXTRA_SUBJECT, "BudgetMate_Transactions_Export.csv")
        type = "text/csv"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Budget Report CSV"))
}
