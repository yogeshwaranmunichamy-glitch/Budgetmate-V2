package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.ml.TripAnalyticsEngine
import com.example.ui.components.AddEditTripExpenseDialog
import com.example.ui.components.AddTripPlanItemDialog
import com.example.ui.components.CategoryDoughnutChart
import com.example.ui.components.CreateEditTripDialog
import com.example.ui.components.TripVoiceEntryDialog
import com.example.ui.components.TripVoiceQueryDialog
import com.example.ui.theme.BudgetExceededRed
import com.example.ui.theme.BudgetNearOrange
import com.example.ui.theme.BudgetSafeGreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.BudgetMateViewModel
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripScreen(viewModel: BudgetMateViewModel) {
    val context = LocalContext.current
    val trips by viewModel.trips.collectAsState()
    val selectedTrip by viewModel.selectedTrip.collectAsState()
    val tripExpenses by viewModel.tripExpenses.collectAsState()
    val tripPlanItems by viewModel.tripPlanItems.collectAsState()
    val tripAnalytics by viewModel.tripAnalytics.collectAsState()
    val isTripModeActive by viewModel.isTripModeActive.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var tripSwitcherExpanded by remember { mutableStateOf(false) }

    // Dialogs state
    var showCreateEditTripDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<TripEntity?>(null) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<TripExpenseEntity?>(null) }

    var showVoiceEntryDialog by remember { mutableStateOf(false) }
    var showVoiceQueryDialog by remember { mutableStateOf(false) }
    var showAddPlanItemDialog by remember { mutableStateOf(false) }

    var expenseFilterCategory by remember { mutableStateOf("ALL") }
    var expenseSearchQuery by remember { mutableStateOf("") }

    val tabs = listOf("Expenses", "Category Spend", "Split & Settle", "Itinerary")

    Box(modifier = Modifier.fillMaxSize().testTag("trip_screen")) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Top Action Bar with Trip Selector & Trip Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trip dropdown selector
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { tripSwitcherExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedTrip?.coverEmoji ?: "🌴",
                                fontSize = 16.sp
                            )
                            Text(
                                text = selectedTrip?.name ?: "Select Trip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1
                            )
                            Text(text = "▼", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        DropdownMenu(
                            expanded = tripSwitcherExpanded,
                            onDismissRequest = { tripSwitcherExpanded = false }
                        ) {
                            trips.forEach { trip ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${trip.coverEmoji} ${trip.name} (${trip.status})")
                                    },
                                    onClick = {
                                        viewModel.selectTrip(trip)
                                        tripSwitcherExpanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("➕ Create New Trip", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    tripToEdit = null
                                    showCreateEditTripDialog = true
                                    tripSwitcherExpanded = false
                                }
                            )
                        }
                    }

                    // Trip Focus Mode Switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isTripModeActive) "Trip Mode ON" else "Trip Mode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isTripModeActive) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = isTripModeActive,
                            onCheckedChange = { viewModel.toggleTripMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldPrimary,
                                checkedTrackColor = EmeraldPrimary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            if (selectedTrip == null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No trips created yet", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Create a vacation trip to track shared expenses, split bills, and plan itinerary.", fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    tripToEdit = null
                                    showCreateEditTripDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Create Trip Now")
                            }
                        }
                    }
                }
            } else {
                val trip = selectedTrip!!
                val analytics = tripAnalytics

                // Trip Hero Banner & Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth().testTag("trip_hero_card")
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            // Background Travel Illustration
                            Image(
                                painter = painterResource(id = R.drawable.trip_travel_banner),
                                contentDescription = "Trip Destination Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )

                            // Gradient Overlay for readable text
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Black.copy(alpha = 0.35f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )

                            // Trip Card Content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${trip.coverEmoji} ${trip.name}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "📍 ${trip.destination}",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            tripToEdit = trip
                                            showCreateEditTripDialog = true
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Trip", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Companions tag row
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    trip.companions.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { comp ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.25f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text("👤 $comp", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Budget Bar
                                val spent = analytics?.totalSpent ?: 0.0
                                val budget = trip.budget
                                val pct = if (budget > 0) ((spent / budget) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
                                val isOver = spent > budget

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Spent: ${CurrencyFormatter.formatINR(spent)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOver) BudgetExceededRed else Color.White
                                    )
                                    Text(
                                        text = "Budget: ${CurrencyFormatter.formatINR(budget)}",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = if (isOver) BudgetExceededRed else if (pct > 80f) BudgetNearOrange else BudgetSafeGreen,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isOver) "⚠️ Overbudget by ${CurrencyFormatter.formatINR(spent - budget)}" else "Remaining: ${CurrencyFormatter.formatINR(budget - spent)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOver) BudgetExceededRed else BudgetSafeGreen
                                    )
                                    if (analytics != null) {
                                        Text(
                                            text = "Pace: ${CurrencyFormatter.formatINR(analytics.dailyBurnRate)}/day",
                                            fontSize = 11.sp,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Action Bar: Add Expense, Voice Log, Voice Query, Share
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                expenseToEdit = null
                                showAddExpenseDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("trip_add_expense_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Expense", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showVoiceEntryDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("trip_voice_log_button")
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Voice Log", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(
                            onClick = { showVoiceQueryDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(44.dp)
                        ) {
                            Icon(Icons.Default.QuestionAnswer, contentDescription = "Voice Query", tint = EmeraldPrimary)
                        }

                        IconButton(
                            onClick = {
                                if (analytics != null) {
                                    val summary = buildTripSummaryText(trip, analytics, tripExpenses)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Trip Summary", summary))
                                    Toast.makeText(context, "Trip Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(44.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Summary", tint = EmeraldPrimary)
                        }
                    }
                }

                // Sub-tabs row
                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        edgePadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }
                }

                // Sub-tab Contents
                when (selectedTabIndex) {
                    0 -> {
                        // Expenses Tab
                        item {
                            // Search and Category Filters
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = expenseSearchQuery,
                                    onValueChange = { expenseSearchQuery = it },
                                    placeholder = { Text("Search expenses...", fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }

                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                (listOf("ALL") + TripAnalyticsEngine.TRIP_CATEGORIES).forEach { cat ->
                                    val isSel = expenseFilterCategory == cat
                                    FilterChip(
                                        selected = isSel,
                                        onClick = { expenseFilterCategory = cat },
                                        label = {
                                            Text(
                                                text = if (cat == "ALL") "All Categories" else "${TripAnalyticsEngine.getCategoryEmoji(cat)} $cat",
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                            selectedLabelColor = EmeraldPrimary
                                        )
                                    )
                                }
                            }
                        }

                        val filteredExpenses = tripExpenses.filter {
                            (expenseFilterCategory == "ALL" || it.category.equals(expenseFilterCategory, ignoreCase = true)) &&
                                    (expenseSearchQuery.isBlank() || it.title.contains(expenseSearchQuery, ignoreCase = true) || it.paidBy.contains(expenseSearchQuery, ignoreCase = true))
                        }

                        if (filteredExpenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No trip expenses match your filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                        } else {
                            items(filteredExpenses, key = { it.id }) { exp ->
                                TripExpenseItemCard(
                                    expense = exp,
                                    onEdit = {
                                        expenseToEdit = exp
                                        showAddExpenseDialog = true
                                    },
                                    onDelete = { viewModel.deleteTripExpense(exp) }
                                )
                            }
                        }
                    }

                    1 -> {
                        // Category-Wise Spending Tab
                        val categoryMap = tripExpenses.groupBy { it.category }
                            .mapValues { it.value.sumOf { exp -> exp.amount } }

                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📊 Category Spending Breakdown",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    CategoryDoughnutChart(categoryData = categoryMap)
                                }
                            }
                        }

                        if (analytics != null && analytics.categorySpends.isNotEmpty()) {
                            val topCat = analytics.categorySpends.first()
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(topCat.iconEmoji, fontSize = 28.sp)
                                        Column {
                                            Text(
                                                text = "Highest Expense: ${topCat.category}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = EmeraldPrimary
                                            )
                                            Text(
                                                text = "${CurrencyFormatter.formatINR(topCat.amount)} (${String.format(Locale.US, "%.1f", topCat.percentage)}% of trip spend across ${topCat.count} items)",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            items(analytics.categorySpends) { catSpend ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(catSpend.iconEmoji, fontSize = 20.sp)
                                                Column {
                                                    Text(catSpend.category, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                                    Text("${catSpend.count} transactions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    CurrencyFormatter.formatINR(catSpend.amount),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "${String.format(Locale.US, "%.1f", catSpend.percentage)}%",
                                                    fontSize = 11.sp,
                                                    color = EmeraldPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        LinearProgressIndicator(
                                            progress = { (catSpend.percentage / 100f).coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = EmeraldPrimary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Group Split & Settle Up Tab
                        if (analytics != null) {
                            item {
                                Text(
                                    text = "👥 Individual Companion Balances",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            items(analytics.balances) { balance ->
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "👤 ${balance.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Paid: ${CurrencyFormatter.formatINR(balance.totalPaid)} | Share: ${CurrencyFormatter.formatINR(balance.totalShare)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            val isPositive = balance.netBalance > 0.01
                                            val isNegative = balance.netBalance < -0.01
                                            Text(
                                                text = if (isPositive) "+${CurrencyFormatter.formatINR(balance.netBalance)}"
                                                else if (isNegative) "-${CurrencyFormatter.formatINR(kotlin.math.abs(balance.netBalance))}"
                                                else "₹0",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isPositive) BudgetSafeGreen else if (isNegative) BudgetExceededRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = if (isPositive) "Gets back" else if (isNegative) "Owes" else "Settled",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isPositive) BudgetSafeGreen else if (isNegative) BudgetExceededRed else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "⚡ Simplified Debt Settlements",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Fair minimum payment transactions to balance all group costs",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (analytics.settlements.isEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎉 All debts are settled! No payments required.", color = BudgetSafeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                items(analytics.settlements) { settlement ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${settlement.from} ➔ ${settlement.to}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    text = "Amount: ${CurrencyFormatter.formatINR(settlement.amount)}",
                                                    fontSize = 12.sp,
                                                    color = EmeraldPrimary,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.settleUpDebt(settlement)
                                                    Toast.makeText(context, "Settlement of ${CurrencyFormatter.formatINR(settlement.amount)} recorded!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Settle Up", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Itinerary & Checklist Tab
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📋 Trip Itinerary & Checklist",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Button(
                                    onClick = { showAddPlanItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Task", fontSize = 11.sp)
                                }
                            }
                        }

                        if (tripPlanItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No itinerary items added. Plan flights, resort bookings, and activities!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            val groupedItems = tripPlanItems.groupBy { it.dayNumber }
                            groupedItems.keys.sorted().forEach { day ->
                                item {
                                    Text(
                                        text = if (day == 0) "Pre-Trip Preparations" else "Day $day Itinerary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }

                                items(groupedItems[day] ?: emptyList(), key = { it.id }) { item ->
                                    Card(
                                        shape = RoundedCornerShape(10.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = item.isDone,
                                                onCheckedChange = { viewModel.toggleTripPlanItem(item) }
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium
                                                )
                                                if (item.estimatedCost > 0) {
                                                    Text(
                                                        text = "Est. Cost: ${CurrencyFormatter.formatINR(item.estimatedCost)}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteTripPlanItem(item) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialogs
    if (showCreateEditTripDialog) {
        CreateEditTripDialog(
            tripToEdit = tripToEdit,
            onDismiss = { showCreateEditTripDialog = false },
            onSave = { viewModel.saveTrip(it) }
        )
    }

    if (showAddExpenseDialog && selectedTrip != null) {
        AddEditTripExpenseDialog(
            trip = selectedTrip!!,
            expenseToEdit = expenseToEdit,
            onDismiss = { showAddExpenseDialog = false },
            onSave = { viewModel.addTripExpense(it) }
        )
    }

    if (showVoiceEntryDialog && selectedTrip != null) {
        TripVoiceEntryDialog(
            trip = selectedTrip!!,
            viewModel = viewModel,
            onDismiss = { showVoiceEntryDialog = false }
        )
    }

    if (showVoiceQueryDialog && selectedTrip != null) {
        TripVoiceQueryDialog(
            trip = selectedTrip!!,
            viewModel = viewModel,
            onDismiss = { showVoiceQueryDialog = false }
        )
    }

    if (showAddPlanItemDialog && selectedTrip != null) {
        AddTripPlanItemDialog(
            tripId = selectedTrip!!.id,
            onDismiss = { showAddPlanItemDialog = false },
            onSave = { title, cost, day ->
                viewModel.addTripPlanItem(title, cost, day)
            }
        )
    }
}

@Composable
fun TripExpenseItemCard(
    expense: TripExpenseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)
    val emoji = TripAnalyticsEngine.getCategoryEmoji(expense.category)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = expense.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        if (expense.isVoiceLogged) {
                            Text(" 🎙️", fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "Paid by ${expense.paidBy} • Split: ${expense.splitAmong}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${dateFmt.format(Date(expense.date))} • ${expense.paymentMethod}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.formatINR(expense.amount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

private fun buildTripSummaryText(trip: TripEntity, analytics: com.example.ml.TripBudgetAnalytics, expenses: List<TripExpenseEntity>): String {
    val sb = StringBuilder()
    sb.appendLine("🌴 *${trip.name}* (${trip.destination})")
    sb.appendLine("💰 Total Budget: ${CurrencyFormatter.formatINR(trip.budget)}")
    sb.appendLine("💸 Total Spent: ${CurrencyFormatter.formatINR(analytics.totalSpent)}")
    sb.appendLine("💼 Remaining: ${CurrencyFormatter.formatINR(analytics.remainingBudget)}")
    sb.appendLine()
    sb.appendLine("📊 *Category-wise Spending:*")
    for (cat in analytics.categorySpends) {
        sb.appendLine("• ${cat.iconEmoji} ${cat.category}: ${CurrencyFormatter.formatINR(cat.amount)} (${String.format(Locale.US, "%.1f", cat.percentage)}%)")
    }
    sb.appendLine()
    sb.appendLine("👥 *Splitwise Balances:*")
    for (b in analytics.balances) {
        val status = if (b.netBalance > 0.01) "+${CurrencyFormatter.formatINR(b.netBalance)} (Gets back)"
        else if (b.netBalance < -0.01) "-${CurrencyFormatter.formatINR(kotlin.math.abs(b.netBalance))} (Owes)"
        else "Settled"
        sb.appendLine("• ${b.name}: $status")
    }
    if (analytics.settlements.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("⚡ *Fair Settlements:*")
        for (s in analytics.settlements) {
            sb.appendLine("• ${s.from} pays ${s.to} ${CurrencyFormatter.formatINR(s.amount)}")
        }
    }
    sb.appendLine()
    sb.appendLine("Generated via BudgetMate Offline ML Finance App")
    return sb.toString()
}
