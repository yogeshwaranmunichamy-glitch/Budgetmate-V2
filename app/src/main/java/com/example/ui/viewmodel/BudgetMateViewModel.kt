package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataEntryEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.PaymentReminderEntity
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.RecurringTransactionEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.ShopInvoiceEntity
import com.example.data.local.entities.ShopProfileEntity
import com.example.data.local.entities.StockMovementEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.data.local.entities.UserEntity
import com.example.data.repository.BudgetMateRepository
import com.example.util.AlarmReminderManager
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

    private val defaultUser = UserEntity(
        id = 1L,
        name = "Rahul Sharma",
        email = "demo@budgetmate.com",
        passwordHash = "password123",
        currencySymbol = "₹",
        monthlyIncomeTarget = 50000.0
    )

    // Current Logged in User (Defaulted to demo user so app preview immediately loads Dashboard)
    private val _currentUser = MutableStateFlow<UserEntity?>(defaultUser)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Auth screen feedback
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _authSuccessMessage = MutableStateFlow<String?>(null)
    val authSuccessMessage: StateFlow<String?> = _authSuccessMessage.asStateFlow()

    // Active bottom navigation destination
    private val _currentScreen = MutableStateFlow("dashboard") // dashboard, transactions, voice, budget, savings, reports, profile
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Mode Switch: "PERSONAL" vs "BUSINESS"
    private val _appMode = MutableStateFlow("PERSONAL")
    val appMode: StateFlow<String> = _appMode.asStateFlow()

    fun setAppMode(mode: String) {
        _appMode.value = mode
        _isShopModeActive.value = (mode == "BUSINESS")
    }

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

    // ---------------- Shop & Small Industry Management ----------------
    private val _isShopModeActive = MutableStateFlow(false)
    val isShopModeActive: StateFlow<Boolean> = _isShopModeActive.asStateFlow()

    private val _shopProfile = MutableStateFlow<ShopProfileEntity?>(null)
    val shopProfile: StateFlow<ShopProfileEntity?> = _shopProfile.asStateFlow()

    private val _khataParties = MutableStateFlow<List<KhataPartyEntity>>(emptyList())
    val khataParties: StateFlow<List<KhataPartyEntity>> = _khataParties.asStateFlow()

    private val _selectedKhataParty = MutableStateFlow<KhataPartyEntity?>(null)
    val selectedKhataParty: StateFlow<KhataPartyEntity?> = _selectedKhataParty.asStateFlow()

    private val _selectedPartyEntries = MutableStateFlow<List<KhataEntryEntity>>(emptyList())
    val selectedPartyEntries: StateFlow<List<KhataEntryEntity>> = _selectedPartyEntries.asStateFlow()

    private val _inventoryItems = MutableStateFlow<List<InventoryItemEntity>>(emptyList())
    val inventoryItems: StateFlow<List<InventoryItemEntity>> = _inventoryItems.asStateFlow()

    private val _lowStockItems = MutableStateFlow<List<InventoryItemEntity>>(emptyList())
    val lowStockItems: StateFlow<List<InventoryItemEntity>> = _lowStockItems.asStateFlow()

    private val _shopInvoices = MutableStateFlow<List<ShopInvoiceEntity>>(emptyList())
    val shopInvoices: StateFlow<List<ShopInvoiceEntity>> = _shopInvoices.asStateFlow()

    private val _dailyCashRegister = MutableStateFlow<DailyCashRegisterEntity?>(null)
    val dailyCashRegister: StateFlow<DailyCashRegisterEntity?> = _dailyCashRegister.asStateFlow()

    val khataTypeFilter = MutableStateFlow("ALL") // "ALL", "CUSTOMER", "SUPPLIER"
    val inventoryCategoryFilter = MutableStateFlow("ALL") // "ALL", "Raw Materials", "Finished Goods", "Retail Goods", "Spares & Tools"

    // ---------------- Payment Reminders & Loan Alerts ----------------
    private val _paymentReminders = MutableStateFlow<List<PaymentReminderEntity>>(emptyList())
    val paymentReminders: StateFlow<List<PaymentReminderEntity>> = _paymentReminders.asStateFlow()

    private val _activeAlarmReminder = MutableStateFlow<PaymentReminderEntity?>(null)
    val activeAlarmReminder: StateFlow<PaymentReminderEntity?> = _activeAlarmReminder.asStateFlow()

    init {
        // Ensure user is immediately loaded on app start so preview & app never gets stuck on auth
        viewModelScope.launch {
            try {
                var user = repository.loginUser("demo@budgetmate.com", "password123")
                if (user == null) {
                    val existing = repository.getUserByEmail("demo@budgetmate.com")
                    if (existing != null) {
                        user = existing
                    } else {
                        val first = repository.getFirstUser()
                        if (first != null) {
                            user = first
                        } else {
                            val uid = repository.registerUser("Rahul Sharma", "demo@budgetmate.com", "password123")
                            user = repository.getUserById(uid)
                            user?.let { seedSampleData(it.id) }
                        }
                    }
                }
                _currentUser.value = user
                user?.let { loadUserData(it.id) }
            } catch (e: Exception) {
                try {
                    val fallback = repository.getFirstUser()
                    _currentUser.value = fallback
                    fallback?.let { loadUserData(it.id) }
                } catch (_: Exception) {}
            }
        }
    }

    fun loginAsDemo() {
        viewModelScope.launch {
            try {
                var user = repository.getUserByEmail("demo@budgetmate.com") ?: repository.getFirstUser()
                if (user == null) {
                    val uid = repository.registerUser("Rahul Sharma", "demo@budgetmate.com", "password123")
                    user = repository.getUserById(uid)
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

            // Seed and load Shop & Small Industry Data
            launch {
                repository.seedStarterShopDataIfEmpty(userId)

                launch {
                    repository.getShopProfile(userId).collect { profile ->
                        _shopProfile.value = profile
                    }
                }

                launch {
                    repository.getAllKhataParties(userId).collect { parties ->
                        _khataParties.value = parties
                        // Auto-update selected party reference if it changed
                        val currentSelectedId = _selectedKhataParty.value?.id
                        if (currentSelectedId != null) {
                            _selectedKhataParty.value = parties.firstOrNull { it.id == currentSelectedId }
                        }
                    }
                }

                launch {
                    _selectedKhataParty.collect { party ->
                        if (party != null) {
                            launch {
                                repository.getKhataEntriesForParty(party.id).collect { entries ->
                                    _selectedPartyEntries.value = entries
                                }
                            }
                        } else {
                            _selectedPartyEntries.value = emptyList()
                        }
                    }
                }

                launch {
                    repository.getAllInventoryItems(userId).collect { items ->
                        _inventoryItems.value = items
                    }
                }

                launch {
                    repository.getLowStockInventoryItems(userId).collect { lowItems ->
                        _lowStockItems.value = lowItems
                    }
                }

                launch {
                    repository.getAllShopInvoices(userId).collect { invoices ->
                        _shopInvoices.value = invoices
                    }
                }

                launch {
                    repository.getLatestCashRegister(userId).collect { register ->
                        _dailyCashRegister.value = register
                    }
                }

                // Seed and load Payment & Loan Reminders
                launch {
                    repository.seedStarterPaymentRemindersIfEmpty(userId)
                    repository.getAllPaymentReminders(userId).collect { reminders ->
                        _paymentReminders.value = reminders
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

    fun addMultipleTransactions(txs: List<TransactionEntity>, onSaved: ((List<TransactionEntity>) -> Unit)? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val savedList = mutableListOf<TransactionEntity>()
            for (tx in txs) {
                val insertedId = repository.insertTransaction(tx.copy(userId = user.id))
                savedList.add(tx.copy(id = insertedId, userId = user.id))
            }
            onSaved?.invoke(savedList)
        }
    }

    suspend fun saveTransactionDirect(tx: TransactionEntity): TransactionEntity? {
        val user = _currentUser.value ?: return null
        val insertedId = repository.insertTransaction(tx.copy(userId = user.id))
        return tx.copy(id = insertedId, userId = user.id)
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

    fun trainCustomKeyword(keyword: String, category: String) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordUserCorrection(user.id, keyword, "Unclassified", category)
            com.example.ml.MultilingualNLP.trainCustomKeyword(keyword, category)
        }
    }

    fun clearCustomLearnedMemory() {
        com.example.ml.MultilingualNLP.clearLearnedMemory()
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

    // ---------------- Shop & Small Industry Actions ----------------

    fun toggleShopMode(active: Boolean? = null) {
        _isShopModeActive.value = active ?: !_isShopModeActive.value
    }

    fun saveShopProfile(profile: ShopProfileEntity) {
        viewModelScope.launch {
            repository.saveShopProfile(profile)
        }
    }

    fun saveKhataParty(party: KhataPartyEntity) {
        viewModelScope.launch {
            repository.saveKhataParty(party)
        }
    }

    fun updateKhataParty(party: KhataPartyEntity) {
        viewModelScope.launch {
            repository.updateKhataParty(party)
        }
    }

    fun deleteKhataParty(party: KhataPartyEntity) {
        viewModelScope.launch {
            if (_selectedKhataParty.value?.id == party.id) {
                _selectedKhataParty.value = null
            }
            repository.deleteKhataParty(party)
        }
    }

    fun selectKhataParty(party: KhataPartyEntity?) {
        _selectedKhataParty.value = party
    }

    fun recordKhataEntry(
        partyId: Long,
        type: String, // "GAVE" or "GOT"
        amount: Double,
        description: String = "",
        billNumber: String = "",
        paymentMethod: String = "Cash"
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val entry = KhataEntryEntity(
                userId = user.id,
                partyId = partyId,
                type = type,
                amount = amount,
                description = description,
                billNumber = billNumber,
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis()
            )
            repository.recordKhataEntry(entry)
        }
    }

    fun settleKhataParty(party: KhataPartyEntity, paymentMethod: String = "Cash") {
        val user = _currentUser.value ?: return
        if (party.currentBalance <= 0) return
        viewModelScope.launch {
            val entry = KhataEntryEntity(
                userId = user.id,
                partyId = party.id,
                type = if (party.type == "CUSTOMER") "GOT" else "GAVE",
                amount = party.currentBalance,
                description = "Full Balance Settlement",
                paymentMethod = paymentMethod,
                date = System.currentTimeMillis()
            )
            repository.recordKhataEntry(entry)
        }
    }

    fun saveInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.saveInventoryItem(item)
        }
    }

    fun updateInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.updateInventoryItem(item)
        }
    }

    fun deleteInventoryItem(item: InventoryItemEntity) {
        viewModelScope.launch {
            repository.deleteInventoryItem(item)
        }
    }

    fun adjustStock(
        itemId: Long,
        type: String, // "STOCK_IN" or "STOCK_OUT"
        quantity: Double,
        unitPrice: Double = 0.0,
        reason: String = "Adjustment",
        note: String = ""
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.recordStockMovement(
                userId = user.id,
                itemId = itemId,
                type = type,
                quantity = quantity,
                unitPrice = unitPrice,
                reason = reason,
                note = note
            )
        }
    }

    fun createShopInvoice(
        invoice: ShopInvoiceEntity,
        autoKhata: Boolean = true,
        autoDeductInventory: Boolean = true
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.saveShopInvoice(invoice, autoCreateKhataEntry = autoKhata)

            // If auto deduct inventory and this is a SALE
            if (autoDeductInventory && invoice.type == "SALE") {
                try {
                    val jsonArray = org.json.JSONArray(invoice.itemsJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val name = obj.optString("name", "")
                        val qty = obj.optDouble("qty", 0.0)
                        val rate = obj.optDouble("rate", 0.0)
                        val matched = _inventoryItems.value.firstOrNull { it.name.equals(name, ignoreCase = true) }
                        if (matched != null && qty > 0) {
                            repository.recordStockMovement(
                                userId = user.id,
                                itemId = matched.id,
                                type = "STOCK_OUT",
                                quantity = qty,
                                unitPrice = rate,
                                reason = "Sale Bill #${invoice.invoiceNumber}",
                                note = "Auto deducted on invoice"
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun saveDailyCashRegister(register: DailyCashRegisterEntity) {
        viewModelScope.launch {
            repository.saveOrUpdateCashRegister(register)
        }
    }

    fun getTotalKhataReceivable(): Double {
        return _khataParties.value
            .filter { it.type == "CUSTOMER" && it.currentBalance > 0 }
            .sumOf { it.currentBalance }
    }

    fun getTotalKhataPayable(): Double {
        return _khataParties.value
            .filter { it.type == "SUPPLIER" && it.currentBalance > 0 }
            .sumOf { it.currentBalance }
    }

    fun getTotalStockValuation(): Double {
        return _inventoryItems.value.sumOf { it.currentStock * it.purchasePrice }
    }

    fun getTotalRetailStockValuation(): Double {
        return _inventoryItems.value.sumOf { it.currentStock * it.sellingPrice }
    }

    fun generatePaymentReminderMessage(party: KhataPartyEntity): String {
        val shopName = _shopProfile.value?.businessName ?: "Our Store"
        val phone = _shopProfile.value?.phone ?: ""
        val upi = _shopProfile.value?.upiId ?: ""
        val amt = CurrencyFormatter.formatINR(party.currentBalance)
        return """
            Namaste ${party.name},
            This is a polite reminder from $shopName regarding your outstanding dues of $amt.
            ${if (upi.isNotBlank()) "You can pay via UPI to: $upi" else ""}
            Kindly clear the balance at your earliest convenience.
            Thank you for your business!
            — $shopName ${if (phone.isNotBlank()) "(Ph: $phone)" else ""}
        """.trimIndent()
    }

    fun generateInvoiceSlipText(invoice: ShopInvoiceEntity): String {
        val shop = _shopProfile.value
        val shopName = shop?.businessName ?: "BudgetMate Business"
        val shopGst = if (!shop?.gstin.isNullOrBlank()) "GSTIN: ${shop?.gstin}" else ""
        val shopPhone = if (!shop?.phone.isNullOrBlank()) "Ph: ${shop?.phone}" else ""
        val dateStr = CurrencyFormatter.formatShortDate(invoice.date)

        val itemsBuilder = StringBuilder()
        try {
            val arr = org.json.JSONArray(invoice.itemsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "Item")
                val qty = obj.optDouble("qty", 1.0)
                val unit = obj.optString("unit", "Pcs")
                val rate = obj.optDouble("rate", 0.0)
                val total = obj.optDouble("total", qty * rate)
                itemsBuilder.append("- $name: $qty $unit @ ${CurrencyFormatter.formatINR(rate)} = ${CurrencyFormatter.formatINR(total)}\n")
            }
        } catch (_: Exception) {}

        return """
            ==============================
            $shopName
            ${if (shopGst.isNotBlank()) "$shopGst\n" else ""}${if (shopPhone.isNotBlank()) "$shopPhone\n" else ""}
            INVOICE: ${invoice.invoiceNumber}
            Date: $dateStr
            Customer: ${invoice.partyName} ${if (invoice.partyPhone.isNotBlank()) "(${invoice.partyPhone})" else ""}
            ==============================
            ITEMS:
            $itemsBuilder
            ------------------------------
            Subtotal: ${CurrencyFormatter.formatINR(invoice.subtotal)}
            Discount: -${CurrencyFormatter.formatINR(invoice.discountAmount)}
            GST (${invoice.taxRate}%): +${CurrencyFormatter.formatINR(invoice.taxAmount)}
            GRAND TOTAL: ${CurrencyFormatter.formatINR(invoice.grandTotal)}
            Payment: ${invoice.paymentMode} (${invoice.paymentStatus})
            Paid: ${CurrencyFormatter.formatINR(invoice.paidAmount)}
            Balance Due: ${CurrencyFormatter.formatINR(invoice.balanceDue)}
            ==============================
            Thank you for your business!
        """.trimIndent()
    }

    // ---------------- Payment & Loan Reminders Actions ----------------

    fun savePaymentReminder(reminder: PaymentReminderEntity) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val id = repository.savePaymentReminder(reminder.copy(userId = user.id))
            val saved = reminder.copy(id = if (reminder.id != 0L) reminder.id else id, userId = user.id)
            AlarmReminderManager.scheduleAlarm(getApplication(), saved)
        }
    }

    fun markPaymentReminderCompleted(reminder: PaymentReminderEntity) {
        viewModelScope.launch {
            repository.markPaymentReminderCompleted(reminder.id)
            AlarmReminderManager.cancelAlarm(getApplication(), reminder.id)
        }
    }

    fun snoozePaymentReminder(reminder: PaymentReminderEntity, additionalHours: Int = 24) {
        viewModelScope.launch {
            val newDue = System.currentTimeMillis() + (additionalHours * 3600000L)
            repository.snoozePaymentReminder(reminder.id, newDue)
            AlarmReminderManager.scheduleAlarm(getApplication(), reminder.copy(dueDate = newDue, status = "SNOOZED"))
        }
    }

    fun deletePaymentReminder(reminder: PaymentReminderEntity) {
        viewModelScope.launch {
            repository.deletePaymentReminder(reminder)
            AlarmReminderManager.cancelAlarm(getApplication(), reminder.id)
        }
    }

    fun togglePaymentReminderAlarm(reminder: PaymentReminderEntity) {
        viewModelScope.launch {
            val newEnabled = !reminder.isAlarmEnabled
            repository.togglePaymentReminderAlarm(reminder.id, newEnabled)
            if (newEnabled) {
                AlarmReminderManager.scheduleAlarm(getApplication(), reminder.copy(isAlarmEnabled = true))
            } else {
                AlarmReminderManager.cancelAlarm(getApplication(), reminder.id)
            }
        }
    }

    fun triggerAlarmTest(reminder: PaymentReminderEntity) {
        _activeAlarmReminder.value = reminder
        AlarmReminderManager.playTestAlarm(getApplication())
    }

    fun dismissAlarmTest() {
        _activeAlarmReminder.value = null
        AlarmReminderManager.stopTestAlarm()
    }
}
