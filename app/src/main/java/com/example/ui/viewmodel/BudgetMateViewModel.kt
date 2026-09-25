package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.RecurringTransactionEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.data.local.entities.UserEntity
import com.example.data.repository.BudgetMateRepository
import com.example.ml.ComprehensiveSpendingAnalysis
import com.example.ml.DebtSettlement
import com.example.ml.MultilingualNLP
import com.example.ml.OverallSpendingPrediction
import com.example.ml.ParsedTripVoiceExpense
import com.example.ml.ParsedVoiceQuery
import com.example.ml.QueryIntent
import com.example.ml.SpendingInsightsEngine
import com.example.ml.SpendingPredictionEngine
import com.example.ml.TripAnalyticsEngine
import com.example.ml.TripBudgetAnalytics
import com.example.ml.TripVoiceQueryType
import com.example.ml.VoiceQueryIntentEngine
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BudgetMateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BudgetMateRepository(AppDatabase.getDatabase(application))

    // Current Logged in User
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Auth screen feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    // Active bottom navigation destination
    private val _currentScreen = MutableStateFlow("dashboard") // dashboard, transactions, voice, budget, savings, reports, profile
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Transactions
    private val _transactions = MutableStateFlow<List<TransactionEntity>>(emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = _transactions.asStateFlow()

    // Filter & Search states for Transactions screen
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val filterCategory = MutableStateFlow("ALL")
    val filterPaymentMethod = MutableStateFlow("ALL")
    val sortBy = MutableStateFlow("DATE_DESC") // "DATE_DESC", "DATE_ASC", "AMOUNT_DESC", "AMOUNT_ASC"

    // Budgets
    private val _budgets = MutableStateFlow<List<BudgetEntity>>(emptyList())
    val budgets: StateFlow<List<BudgetEntity>> = _budgets.asStateFlow()

    // Savings Goals
    private val _savingsGoals = MutableStateFlow<List<SavingsGoalEntity>>(emptyList())
    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = _savingsGoals.asStateFlow()

    // Recurring
    private val _recurringList = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    val recurringList: StateFlow<List<RecurringTransactionEntity>> = _recurringList.asStateFlow()

    // ---------------- Trip Planning Mode ----------------
    private val _trips = MutableStateFlow<List<TripEntity>>(emptyList())
    val trips: StateFlow<List<TripEntity>> = _trips.asStateFlow()

    private val _activeTrip = MutableStateFlow<TripEntity?>(null)
    val activeTrip: StateFlow<TripEntity?> = _activeTrip.asStateFlow()

    private val _selectedTrip = MutableStateFlow<TripEntity?>(null)
    val selectedTrip: StateFlow<TripEntity?> = _selectedTrip.asStateFlow()

    private val _tripExpenses = MutableStateFlow<List<TripExpenseEntity>>(emptyList())
    val tripExpenses: StateFlow<List<TripExpenseEntity>> = _tripExpenses.asStateFlow()

    private val _tripPlanItems = MutableStateFlow<List<TripPlanItemEntity>>(emptyList())
    val tripPlanItems: StateFlow<List<TripPlanItemEntity>> = _tripPlanItems.asStateFlow()

    private val _tripAnalytics = MutableStateFlow<TripBudgetAnalytics?>(null)
    val tripAnalytics: StateFlow<TripBudgetAnalytics?> = _tripAnalytics.asStateFlow()

    private val _isTripModeActive = MutableStateFlow(false)
    val isTripModeActive: StateFlow<Boolean> = _isTripModeActive.asStateFlow()

    private val _tripVoiceResponse = MutableStateFlow<String?>(null)
    val tripVoiceResponse: StateFlow<String?> = _tripVoiceResponse.asStateFlow()

    private val _pendingTripVoiceExpense = MutableStateFlow<ParsedTripVoiceExpense?>(null)
    val pendingTripVoiceExpense: StateFlow<ParsedTripVoiceExpense?> = _pendingTripVoiceExpense.asStateFlow()

    // ML Feedback Count
    private val _learnedSamplesCount = MutableStateFlow(0)
    val learnedSamplesCount: StateFlow<Int> = _learnedSamplesCount.asStateFlow()

    // Voice query response
    private val _voiceQueryResult = MutableStateFlow<String?>(null)
    val voiceQueryResult: StateFlow<String?> = _voiceQueryResult.asStateFlow()

    // Sponsored ad dialog trigger (for summaries & insights gate)
    private val _showAdDialog = MutableStateFlow(false)
    val showAdDialog: StateFlow<Boolean> = _showAdDialog.asStateFlow()

    private val _pendingAdPurpose = MutableStateFlow("")
    val pendingAdPurpose: StateFlow<String> = _pendingAdPurpose.asStateFlow()

    private val _adUnlockedForSession = MutableStateFlow(false)
    val adUnlockedForSession: StateFlow<Boolean> = _adUnlockedForSession.asStateFlow()

    // Reports period filter
    val reportPeriod = MutableStateFlow("MONTHLY") // "DAILY", "WEEKLY", "MONTHLY", "YEARLY"

    init {
        // Create demo account automatically if none exists so users can instantly test
        viewModelScope.launch {
            try {
                var user = repository.loginUser("demo@budgetmate.com", "password123")
                if (user == null) {
                    val uid = repository.registerUser("Rahul Sharma", "demo@budgetmate.com", "password123")
                    user = repository.getUserById(uid)
                    // Seed initial sample transactions for demonstration
                    user?.let { seedSampleData(it.id) }
                }
                _currentUser.value = user
                user?.let { loadUserData(it.id) }
            } catch (_: Exception) {}
        }
    }

    private suspend fun seedSampleData(userId: Long) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // Salary income
        repository.insertTransaction(
            TransactionEntity(
                userId = userId,
                type = "INCOME",
                amount = 65000.0,
                category = "Salary",
                paymentMethod = "Bank Transfer",
                sourceOrMerchant = "Infosys Ltd",
                date = now - (86400000L * 15),
                notes = "Monthly Salary"
            )
        )

        // Normal expenses
        val samples = listOf(
            Triple(450.0, "Food", "Swiggy"),
            Triple(700.0, "Petrol", "HP Fuel Station"),
            Triple(2200.0, "Shopping", "Amazon"),
            Triple(1450.0, "Electricity", "TNEB Electricity Bill"),
            Triple(649.0, "Subscriptions", "Netflix"),
            Triple(320.0, "Food", "Saravana Bhavan"),
            Triple(180.0, "Transport", "Uber Auto"),
            Triple(1800.0, "Groceries", "DMart Supermarket")
        )

        for ((idx, item) in samples.withIndex()) {
            repository.insertTransaction(
                TransactionEntity(
                    userId = userId,
                    type = "EXPENSE",
                    amount = item.first,
                    category = item.second,
                    paymentMethod = if (idx % 2 == 0) "UPI" else "Credit Card",
                    sourceOrMerchant = item.third,
                    date = now - (86400000L * (idx + 1)),
                    notes = item.third
                )
            )
        }

        // Add Savings Goal
        repository.saveSavingsGoal(
            SavingsGoalEntity(
                userId = userId,
                title = "Emergency Fund",
                targetAmount = 100000.0,
                currentAmount = 45000.0,
                targetDate = now + (86400000L * 90),
                notes = "6 months living expenses"
            )
        )

        // Add Recurring Transaction
        repository.saveRecurring(
            RecurringTransactionEntity(
                userId = userId,
                type = "EXPENSE",
                amount = 12000.0,
                category = "Rent",
                paymentMethod = "Bank Transfer",
                sourceOrMerchant = "Landlord",
                frequency = "MONTHLY",
                nextDueDate = now + (86400000L * 5),
                notes = "Flat rent"
            )
        )

        // Seed Starter Trip: Goa Beach Getaway with friends
        val tripId = repository.saveTrip(
            TripEntity(
                userId = userId,
                name = "Goa Beach Getaway",
                destination = "Goa, India",
                startDate = now - (86400000L * 2),
                endDate = now + (86400000L * 3),
                budget = 35000.0,
                currency = "INR",
                companions = "Me, Rahul, Priya, Arun",
                status = "ACTIVE",
                notes = "Annual friends beach vacation in North Goa",
                coverEmoji = "🏖️"
            )
        )

        // Seed trip expenses with real split & companion data
        val tripExpenses = listOf(
            TripExpenseEntity(
                tripId = tripId,
                userId = userId,
                title = "Beach Villa Stay (3 Nights)",
                amount = 14500.0,
                category = "Stay & Hotel",
                paidBy = "Me",
                splitAmong = "All",
                date = now - (86400000L * 2),
                paymentMethod = "Credit Card",
                notes = "Ocean view villa in Anjuna"
            ),
            TripExpenseEntity(
                tripId = tripId,
                userId = userId,
                title = "Airport to Villa Cab",
                amount = 1800.0,
                category = "Local Commute",
                paidBy = "Rahul",
                splitAmong = "All",
                date = now - (86400000L * 2),
                paymentMethod = "UPI",
                notes = "Innova cab from Dabolim airport"
            ),
            TripExpenseEntity(
                tripId = tripId,
                userId = userId,
                title = "Seafood Dinner at Thalassa",
                amount = 4200.0,
                category = "Food & Dining",
                paidBy = "Priya",
                splitAmong = "All",
                date = now - (86400000L * 1),
                paymentMethod = "UPI",
                notes = "Sunset dinner and Greek salad"
            ),
            TripExpenseEntity(
                tripId = tripId,
                userId = userId,
                title = "Scuba Diving & Jet Ski",
                amount = 6000.0,
                category = "Activities & Sightseeing",
                paidBy = "Me",
                splitAmong = "All",
                date = now,
                paymentMethod = "Cash",
                notes = "Grand Island diving package"
            ),
            TripExpenseEntity(
                tripId = tripId,
                userId = userId,
                title = "Scooter Rentals & Fuel",
                amount = 1600.0,
                category = "Fuel",
                paidBy = "Arun",
                splitAmong = "All",
                date = now,
                paymentMethod = "UPI",
                notes = "2 Activas for 3 days"
            )
        )
        for (exp in tripExpenses) {
            repository.saveTripExpense(exp)
        }

        // Seed Trip Itinerary Checklist
        val planItems = listOf(
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Book Villa in Anjuna", estimatedCost = 15000.0, isDone = true, dayNumber = 0),
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Rent 2 Scooters", estimatedCost = 2000.0, isDone = true, dayNumber = 1),
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Scuba Diving at Grand Island", estimatedCost = 6500.0, isDone = true, dayNumber = 2),
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Sunset Cruise in Mandovi River", estimatedCost = 3000.0, isDone = false, dayNumber = 3),
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Visit Chapora Fort & Vagator Beach", estimatedCost = 500.0, isDone = false, dayNumber = 4),
            TripPlanItemEntity(tripId = tripId, userId = userId, title = "Buy Cashews and Feni Souvenirs", estimatedCost = 2500.0, isDone = false, dayNumber = 5)
        )
        for (item in planItems) {
            repository.savePlanItem(item)
        }
    }

    fun loadUserData(userId: Long) {
        viewModelScope.launch {
            repository.processDueRecurring(userId)
            repository.reloadLearnedFeedback(userId)

            launch {
                repository.getAllTransactions(userId).collect {
                    _transactions.value = it
                }
            }

            launch {
                val currentMonth = CurrencyFormatter.getCurrentMonthYearString()
                repository.getBudgetsForMonth(userId, currentMonth).collect {
                    _budgets.value = it
                }
            }

            launch {
                repository.getAllSavingsGoals(userId).collect {
                    _savingsGoals.value = it
                }
            }

            launch {
                repository.getAllRecurring(userId).collect {
                    _recurringList.value = it
                }
            }

            launch {
                repository.getFeedbackCount(userId).collect {
                    _learnedSamplesCount.value = it
                }
            }

            launch {
                repository.getTripsForUser(userId).collect { tripList ->
                    _trips.value = tripList
                    val currentActive = tripList.firstOrNull { it.status == "ACTIVE" } ?: tripList.firstOrNull()
                    _activeTrip.value = currentActive
                    if (_selectedTrip.value == null) {
                        _selectedTrip.value = currentActive
                    }
                    if (currentActive != null) {
                        _isTripModeActive.value = true
                    }
                }
            }

            launch {
                _selectedTrip.collect { selected ->
                    if (selected != null) {
                        launch {
                            repository.getExpensesForTrip(selected.id).collect { exps ->
                                _tripExpenses.value = exps
                                _tripAnalytics.value = TripAnalyticsEngine.analyzeTrip(selected, exps)
                            }
                        }
                        launch {
                            repository.getPlanItemsForTrip(selected.id).collect { items ->
                                _tripPlanItems.value = items
                            }
                        }
                    } else {
                        _tripExpenses.value = emptyList()
                        _tripPlanItems.value = emptyList()
                        _tripAnalytics.value = null
                    }
                }
            }
        }
    }

    // ---------------- Auth Methods ----------------
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            val user = repository.loginUser(email, pass)
            if (user != null) {
                _currentUser.value = user
                loadUserData(user.id)
            } else {
                _authError.value = "Invalid email or password."
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        viewModelScope.launch {
            _authError.value = null
            try {
                val uid = repository.registerUser(name, email, pass)
                val user = repository.getUserById(uid)
                _currentUser.value = user
                user?.let { loadUserData(it.id) }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed."
            }
        }
    }

    fun forgotPassword(email: String, newPass: String) {
        viewModelScope.launch {
            _authError.value = null
            val success = repository.resetPasswordByEmail(email, newPass)
            if (success) {
                _authSuccessMessage.value = "Password successfully reset! Please log in."
            } else {
                _authError.value = "No account found with this email."
            }
        }
    }

    fun changePassword(newPass: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.updatePassword(user.id, newPass)
            _authSuccessMessage.value = "Password changed successfully."
        }
    }

    fun logout() {
        _currentUser.value = null
        _transactions.value = emptyList()
        _budgets.value = emptyList()
        _savingsGoals.value = emptyList()
        _recurringList.value = emptyList()
        _currentScreen.value = "dashboard"
    }

    fun setScreen(screen: String) {
        _currentScreen.value = screen
    }

    // ---------------- Transactions Management ----------------
    fun addTransaction(tx: TransactionEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertTransaction(tx.copy(userId = user.id))
        }
    }

    fun updateTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun recordVoiceCorrection(rawText: String, predicted: String, corrected: String) {
        val user = _currentUser.value ?: return
        if (predicted != corrected) {
            viewModelScope.launch {
                repository.recordUserCorrection(user.id, rawText, predicted, corrected)
            }
        }
    }

    // ---------------- Budget Management ----------------
    fun setBudget(category: String, limit: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val currentMonth = CurrencyFormatter.getCurrentMonthYearString()
            val entity = BudgetEntity(
                userId = user.id,
                category = category,
                monthlyLimit = limit,
                monthYear = currentMonth
            )
            repository.saveBudget(entity)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // ---------------- Savings Goals Management ----------------
    fun addSavingsGoal(title: String, targetAmount: Double, targetDate: Long, notes: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                userId = user.id,
                title = title,
                targetAmount = targetAmount,
                currentAmount = 0.0,
                targetDate = targetDate,
                notes = notes
            )
            repository.saveSavingsGoal(goal)
        }
    }

    fun addFundsToGoal(goalId: Long, amount: Double) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.addFundsToGoal(goalId, user.id, amount)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // ---------------- Recurring Transactions Management ----------------
    fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveRecurring(recurring.copy(userId = user.id))
        }
    }

    fun deleteRecurring(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurring(recurring)
        }
    }

    // ---------------- Receipt Scan ----------------
    fun saveReceipt(tx: TransactionEntity, scan: ReceiptScanEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.insertTransaction(tx.copy(userId = user.id))
            repository.saveReceipt(scan.copy(userId = user.id))
        }
    }

    // ---------------- Voice Query Intent Processing ----------------
    fun processVoiceQuery(queryText: String) {
        val user = _currentUser.value ?: return
        val parsed = VoiceQueryIntentEngine.parseQuery(queryText)
        val allTx = _transactions.value

        val cal = Calendar.getInstance()
        val currentMonthFmt = SimpleDateFormat("yyyy-MM", Locale.US)
        val curMonthStr = currentMonthFmt.format(cal.time)

        val monthExpenses = allTx.filter { it.type == "EXPENSE" && currentMonthFmt.format(it.date) == curMonthStr }
        val monthIncome = allTx.filter { it.type == "INCOME" && currentMonthFmt.format(it.date) == curMonthStr }

        val totalMonthExpense = monthExpenses.sumOf { it.amount }
        val totalMonthIncome = monthIncome.sumOf { it.amount }
        val balance = allTx.filter { it.type == "INCOME" }.sumOf { it.amount } - allTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val answer = when (parsed.intent) {
            QueryIntent.GET_CATEGORY_EXPENSE -> {
                val cat = parsed.targetCategory ?: "Food"
                val catSpent = monthExpenses.filter { it.category.equals(cat, ignoreCase = true) }.sumOf { it.amount }
                "You have spent ${CurrencyFormatter.formatINR(catSpent)} on $cat this month."
            }
            QueryIntent.GET_TOTAL_EXPENSE -> {
                "Your total spending for this month is ${CurrencyFormatter.formatINR(totalMonthExpense)} across ${monthExpenses.size} transactions."
            }
            QueryIntent.GET_TOTAL_INCOME -> {
                "Your total recorded income this month is ${CurrencyFormatter.formatINR(totalMonthIncome)}."
            }
            QueryIntent.GET_BALANCE -> {
                "Your current net balance is ${CurrencyFormatter.formatINR(balance)}."
            }
            QueryIntent.GET_REMAINING_BUDGET -> {
                val totalBudget = _budgets.value.sumOf { it.monthlyLimit }
                val remaining = (totalBudget - totalMonthExpense).coerceAtLeast(0.0)
                "Total budget: ${CurrencyFormatter.formatINR(totalBudget)}. Remaining: ${CurrencyFormatter.formatINR(remaining)}."
            }
            QueryIntent.GET_SAVINGS_PROGRESS -> {
                val goals = _savingsGoals.value
                if (goals.isEmpty()) {
                    "You have no active savings goals set. Create one in the Savings section!"
                } else {
                    val completed = goals.sumOf { it.currentAmount }
                    val target = goals.sumOf { it.targetAmount }
                    val pct = if (target > 0) (completed / target) * 100 else 0.0
                    "You have saved ${CurrencyFormatter.formatINR(completed)} out of ${CurrencyFormatter.formatINR(target)} across ${goals.size} goals (${String.format("%.0f", pct)}% achieved)."
                }
            }
            QueryIntent.GET_RECENT_TRANSACTIONS -> {
                val recents = allTx.take(3)
                if (recents.isEmpty()) "No recent transactions found."
                else "Last 3 transactions: " + recents.joinToString("; ") { "${it.sourceOrMerchant} (${CurrencyFormatter.formatINR(it.amount)})" }
            }
            QueryIntent.UNKNOWN -> {
                "Could not recognize question intent. Try asking: 'How much did I spend on food this month?' or 'Current balance?'"
            }
        }

        _voiceQueryResult.value = answer
    }

    fun clearVoiceQueryResult() {
        _voiceQueryResult.value = null
    }

    // ---------------- Ad Gating for Summaries & Insights ----------------
    fun requestSummaryOrInsightsWithAd(purpose: String, onGranted: () -> Unit) {
        if (_adUnlockedForSession.value) {
            onGranted()
        } else {
            _pendingAdPurpose.value = purpose
            _showAdDialog.value = true
        }
    }

    fun dismissAdDialog() {
        _showAdDialog.value = false
    }

    fun unlockAdReward() {
        _adUnlockedForSession.value = true
        _showAdDialog.value = false
    }

    // ---------------- Calculations for UI ----------------
    fun getSpendingAnalysis(): ComprehensiveSpendingAnalysis {
        val expenses = _transactions.value.filter { it.type == "EXPENSE" }
        val incomes = _transactions.value.filter { it.type == "INCOME" }
        return SpendingInsightsEngine.generateAnalysis(expenses, incomes)
    }

    fun getSpendingPrediction(): OverallSpendingPrediction {
        val expenses = _transactions.value.filter { it.type == "EXPENSE" }
        return SpendingPredictionEngine.predictNextMonthSpending(expenses)
    }

    fun getCurrentMonthExpenseSum(): Double {
        val curMonth = CurrencyFormatter.getCurrentMonthYearString()
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        return _transactions.value
            .filter { it.type == "EXPENSE" && fmt.format(it.date) == curMonth }
            .sumOf { it.amount }
    }

    fun getCurrentMonthIncomeSum(): Double {
        val curMonth = CurrencyFormatter.getCurrentMonthYearString()
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        return _transactions.value
            .filter { it.type == "INCOME" && fmt.format(it.date) == curMonth }
            .sumOf { it.amount }
    }

    fun getTotalBalance(): Double {
        val income = _transactions.value.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = _transactions.value.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        return income - expense
    }

    fun getCategoryExpensesForCurrentMonth(): Map<String, Double> {
        val curMonth = CurrencyFormatter.getCurrentMonthYearString()
        val fmt = SimpleDateFormat("yyyy-MM", Locale.US)
        return _transactions.value
            .filter { it.type == "EXPENSE" && fmt.format(it.date) == curMonth }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
    }

    // ---------------- Trip Mode & Operations ----------------
    fun selectTrip(trip: TripEntity) {
        _selectedTrip.value = trip
    }

    fun toggleTripMode(enabled: Boolean) {
        _isTripModeActive.value = enabled
    }

    fun saveTrip(trip: TripEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val id = repository.saveTrip(trip.copy(userId = user.id))
            if (_selectedTrip.value == null || _selectedTrip.value?.id == trip.id) {
                _selectedTrip.value = trip.copy(id = if (trip.id == 0L) id else trip.id, userId = user.id)
            }
        }
    }

    fun deleteTrip(trip: TripEntity) {
        viewModelScope.launch {
            repository.deleteTrip(trip)
            if (_selectedTrip.value?.id == trip.id) {
                _selectedTrip.value = _trips.value.firstOrNull { it.id != trip.id }
            }
        }
    }

    fun markTripCompleted(tripId: Long) {
        viewModelScope.launch {
            repository.markTripCompleted(tripId)
        }
    }

    fun addTripExpense(expense: TripExpenseEntity) {
        val user = _currentUser.value ?: return
        val currentTrip = _selectedTrip.value ?: return
        viewModelScope.launch {
            repository.saveTripExpense(expense.copy(tripId = currentTrip.id, userId = user.id))
        }
    }

    fun deleteTripExpense(expense: TripExpenseEntity) {
        viewModelScope.launch {
            repository.deleteTripExpense(expense)
        }
    }

    fun addTripPlanItem(title: String, cost: Double, dayNumber: Int) {
        val user = _currentUser.value ?: return
        val currentTrip = _selectedTrip.value ?: return
        viewModelScope.launch {
            repository.savePlanItem(
                TripPlanItemEntity(
                    tripId = currentTrip.id,
                    userId = user.id,
                    title = title,
                    estimatedCost = cost,
                    dayNumber = dayNumber
                )
            )
        }
    }

    fun toggleTripPlanItem(item: TripPlanItemEntity) {
        viewModelScope.launch {
            repository.togglePlanItemDone(item.id, !item.isDone)
        }
    }

    fun deleteTripPlanItem(item: TripPlanItemEntity) {
        viewModelScope.launch {
            repository.deletePlanItem(item)
        }
    }

    fun settleUpDebt(settlement: DebtSettlement) {
        val user = _currentUser.value ?: return
        val currentTrip = _selectedTrip.value ?: return
        viewModelScope.launch {
            // Record an expense that marks the settlement
            repository.saveTripExpense(
                TripExpenseEntity(
                    tripId = currentTrip.id,
                    userId = user.id,
                    title = "Settlement: ${settlement.from} paid ${settlement.to}",
                    amount = settlement.amount,
                    category = "Emergency / Misc",
                    paidBy = settlement.from,
                    splitAmong = settlement.to,
                    date = System.currentTimeMillis(),
                    paymentMethod = "UPI",
                    notes = "Debt settled between ${settlement.from} and ${settlement.to}"
                )
            )
        }
    }

    fun processTripVoiceInput(rawText: String) {
        val currentTrip = _selectedTrip.value ?: return
        val companions = currentTrip.companions.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val parsed = TripAnalyticsEngine.parseTripVoiceExpense(rawText, companions)
        _pendingTripVoiceExpense.value = parsed
    }

    fun confirmTripVoiceExpense(title: String, amount: Double, category: String, paidBy: String, paymentMethod: String) {
        val user = _currentUser.value ?: return
        val currentTrip = _selectedTrip.value ?: return
        viewModelScope.launch {
            repository.saveTripExpense(
                TripExpenseEntity(
                    tripId = currentTrip.id,
                    userId = user.id,
                    title = title,
                    amount = amount,
                    category = category,
                    paidBy = paidBy,
                    splitAmong = "All",
                    date = System.currentTimeMillis(),
                    paymentMethod = paymentMethod,
                    isVoiceLogged = true
                )
            )
            _pendingTripVoiceExpense.value = null
        }
    }

    fun clearPendingTripVoiceExpense() {
        _pendingTripVoiceExpense.value = null
    }

    fun processTripVoiceQuery(query: String) {
        val currentTrip = _selectedTrip.value
        val analytics = _tripAnalytics.value

        if (currentTrip == null || analytics == null) {
            _tripVoiceResponse.value = "Please select or create an active trip first to query trip expenses."
            return
        }

        val parsed = TripAnalyticsEngine.parseTripVoiceQuery(query)
        val answer = when (parsed.queryType) {
            TripVoiceQueryType.REMAINING_BUDGET -> {
                val budget = CurrencyFormatter.formatINR(currentTrip.budget)
                val spent = CurrencyFormatter.formatINR(analytics.totalSpent)
                val rem = CurrencyFormatter.formatINR(analytics.remainingBudget)
                val pct = String.format(Locale.US, "%.1f", analytics.percentSpent)
                if (analytics.isOverBudget) {
                    "⚠️ Alert! You have exceeded the trip budget by ${CurrencyFormatter.formatINR(analytics.totalSpent - currentTrip.budget)}! Total spent: $spent (Budget: $budget)."
                } else {
                    "Trip Budget: $budget. Spent: $spent ($pct%). Remaining: $rem."
                }
            }
            TripVoiceQueryType.CATEGORY_SPEND -> {
                val cat = parsed.targetCategory ?: "Food & Dining"
                val found = analytics.categorySpends.firstOrNull { it.category.equals(cat, ignoreCase = true) }
                if (found != null) {
                    "${found.iconEmoji} You spent ${CurrencyFormatter.formatINR(found.amount)} on ${found.category} (${String.format(Locale.US, "%.1f", found.percentage)}% of total trip spend across ${found.count} items)."
                } else {
                    "No expenses logged under $cat yet for ${currentTrip.name}."
                }
            }
            TripVoiceQueryType.TOTAL_EXPENSE -> {
                "Total spent on ${currentTrip.name}: ${CurrencyFormatter.formatINR(analytics.totalSpent)} across ${_tripExpenses.value.size} items."
            }
            TripVoiceQueryType.WHO_OWES_WHOM -> {
                if (analytics.settlements.isEmpty()) {
                    "All debts are settled! Everyone has paid their fair share."
                } else {
                    val summary = analytics.settlements.joinToString(", ") {
                        "${it.from} owes ${it.to} ${CurrencyFormatter.formatINR(it.amount)}"
                    }
                    "Settlements needed: $summary."
                }
            }
            TripVoiceQueryType.DAILY_PACE -> {
                val pace = CurrencyFormatter.formatINR(analytics.dailyBurnRate)
                val proj = CurrencyFormatter.formatINR(analytics.projectedTotal)
                "Current pace: $pace per day (${analytics.daysElapsed} of ${analytics.daysTotal} days elapsed). Projected trip total: $proj."
            }
            TripVoiceQueryType.UNKNOWN -> {
                "Could not recognize trip query. Try asking: 'How much spent on food?', 'Remaining budget?', 'Who owes whom?', or 'Daily spending pace?'"
            }
        }
        _tripVoiceResponse.value = answer
    }

    fun clearTripVoiceResponse() {
        _tripVoiceResponse.value = null
    }
}
