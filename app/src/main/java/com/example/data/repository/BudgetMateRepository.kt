package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.MLFeedbackEntity
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.RecurringTransactionEntity
import com.example.data.local.entities.SavingsGoalEntity
import com.example.data.local.entities.TransactionEntity
import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.data.local.entities.TripPlanItemEntity
import com.example.data.local.entities.UserEntity
import com.example.ml.AnomalyDetectionEngine
import com.example.ml.MultilingualNLP
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetMateRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val recurringDao = database.recurringDao()
    private val mlFeedbackDao = database.mlFeedbackDao()
    private val receiptDao = database.receiptDao()
    private val tripDao = database.tripDao()
    private val tripExpenseDao = database.tripExpenseDao()
    private val tripPlanItemDao = database.tripPlanItemDao()

    // ---------------- Password & Security ----------------
    fun hashPassword(password: String, salt: String = "BudgetMateSalt2026"): String {
        val bytes = (password + salt).toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }

    suspend fun registerUser(name: String, email: String, password: String): Long {
        val existing = userDao.getUserByEmail(email.lowercase(Locale.ROOT).trim())
        if (existing != null) {
            throw IllegalArgumentException("An account with this email already exists.")
        }
        val user = UserEntity(
            name = name.trim(),
            email = email.lowercase(Locale.ROOT).trim(),
            passwordHash = hashPassword(password)
        )
        val userId = userDao.insertUser(user)

        // Seed default starter budgets for this new user so they immediately have a useful setup
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
        val defaultBudgets = listOf(
            BudgetEntity(userId = userId, category = "Food", monthlyLimit = 8000.0, monthYear = currentMonthYear),
            BudgetEntity(userId = userId, category = "Transport", monthlyLimit = 4000.0, monthYear = currentMonthYear),
            BudgetEntity(userId = userId, category = "Shopping", monthlyLimit = 5000.0, monthYear = currentMonthYear),
            BudgetEntity(userId = userId, category = "Electricity", monthlyLimit = 2000.0, monthYear = currentMonthYear),
            BudgetEntity(userId = userId, category = "Entertainment", monthlyLimit = 3000.0, monthYear = currentMonthYear)
        )
        for (b in defaultBudgets) {
            budgetDao.insertOrUpdateBudget(b)
        }

        return userId
    }

    suspend fun loginUser(email: String, password: String): UserEntity? {
        val user = userDao.getUserByEmail(email.lowercase(Locale.ROOT).trim()) ?: return null
        if (user.passwordHash == hashPassword(password)) {
            return user
        }
        return null
    }

    suspend fun getUserById(userId: Long): UserEntity? = userDao.getUserById(userId)
    fun observeUser(userId: Long): Flow<UserEntity?> = userDao.observeUserById(userId)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun updatePassword(userId: Long, newPass: String) = userDao.updatePassword(userId, hashPassword(newPass))

    suspend fun resetPasswordByEmail(email: String, newPass: String): Boolean {
        val user = userDao.getUserByEmail(email.lowercase(Locale.ROOT).trim()) ?: return false
        userDao.updatePassword(user.id, hashPassword(newPass))
        return true
    }

    // ---------------- Transactions CRUD & Anomaly Check ----------------
    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        // Run anomaly detection if it's an expense
        var checkedTx = transaction
        if (transaction.type == "EXPENSE") {
            val history = transactionDao.getExpensesByCategoryList(transaction.userId, transaction.category)
            val anomalyResult = AnomalyDetectionEngine.checkAnomaly(transaction.amount, transaction.category, history)
            checkedTx = transaction.copy(
                isAnomaly = anomalyResult.isAnomaly,
                anomalyReason = anomalyResult.reason
            )
        }
        return transactionDao.insertTransaction(checkedTx)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = transactionDao.updateTransaction(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.deleteTransaction(transaction)
    suspend fun deleteTransactionById(id: Long, userId: Long) = transactionDao.deleteTransactionById(id, userId)

    fun getAllTransactions(userId: Long): Flow<List<TransactionEntity>> = transactionDao.getAllTransactions(userId)
    fun getRecentTransactions(userId: Long, limit: Int = 10): Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(userId, limit)
    fun getTransactionsByType(userId: Long, type: String): Flow<List<TransactionEntity>> = transactionDao.getTransactionsByType(userId, type)
    fun searchTransactions(userId: Long, query: String): Flow<List<TransactionEntity>> = transactionDao.searchTransactions(userId, query)
    fun getTransactionsByDateRange(userId: Long, start: Long, end: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(userId, start, end)

    suspend fun getAllExpensesList(userId: Long): List<TransactionEntity> = transactionDao.getAllExpensesList(userId)
    suspend fun getAllIncomeList(userId: Long): List<TransactionEntity> = transactionDao.getAllIncomeList(userId)

    fun getTotalIncome(userId: Long): Flow<Double> = transactionDao.getTotalIncome(userId)
    fun getTotalExpenses(userId: Long): Flow<Double> = transactionDao.getTotalExpenses(userId)

    fun getPeriodIncome(userId: Long, start: Long, end: Long): Flow<Double> = transactionDao.getPeriodIncome(userId, start, end)
    fun getPeriodExpenses(userId: Long, start: Long, end: Long): Flow<Double> = transactionDao.getPeriodExpenses(userId, start, end)

    // ---------------- Budgets ----------------
    suspend fun saveBudget(budget: BudgetEntity): Long = budgetDao.insertOrUpdateBudget(budget)
    suspend fun deleteBudget(budget: BudgetEntity) = budgetDao.deleteBudget(budget)
    fun getBudgetsForMonth(userId: Long, monthYear: String): Flow<List<BudgetEntity>> = budgetDao.getBudgetsForMonth(userId, monthYear)
    suspend fun getBudgetsForMonthList(userId: Long, monthYear: String): List<BudgetEntity> = budgetDao.getBudgetsForMonthList(userId, monthYear)

    // ---------------- Savings Goals ----------------
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Long = savingsGoalDao.insertGoal(goal)
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.updateGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = savingsGoalDao.deleteGoal(goal)
    suspend fun addFundsToGoal(id: Long, userId: Long, amount: Double) = savingsGoalDao.addFundsToGoal(id, userId, amount)
    fun getAllSavingsGoals(userId: Long): Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals(userId)

    // ---------------- Recurring Transactions ----------------
    suspend fun saveRecurring(recurring: RecurringTransactionEntity): Long = recurringDao.insertRecurring(recurring)
    suspend fun updateRecurring(recurring: RecurringTransactionEntity) = recurringDao.updateRecurring(recurring)
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity) = recurringDao.deleteRecurring(recurring)
    fun getAllRecurring(userId: Long): Flow<List<RecurringTransactionEntity>> = recurringDao.getAllRecurring(userId)

    // Process due recurring transactions and create actual transactions
    suspend fun processDueRecurring(userId: Long) {
        val now = System.currentTimeMillis()
        val dueList = recurringDao.getDueRecurring(userId, now)
        for (rec in dueList) {
            val tx = TransactionEntity(
                userId = userId,
                type = rec.type,
                amount = rec.amount,
                category = rec.category,
                paymentMethod = rec.paymentMethod,
                sourceOrMerchant = rec.sourceOrMerchant,
                date = rec.nextDueDate,
                notes = "Auto recurring: ${rec.notes}"
            )
            insertTransaction(tx)

            // Update next due date based on frequency
            val cal = Calendar.getInstance().apply { timeInMillis = rec.nextDueDate }
            when (rec.frequency) {
                "DAILY" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "WEEKLY" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "MONTHLY" -> cal.add(Calendar.MONTH, 1)
                "YEARLY" -> cal.add(Calendar.YEAR, 1)
            }
            recurringDao.updateRecurring(rec.copy(nextDueDate = cal.timeInMillis))
        }
    }

    // ---------------- ML Feedback & Learning ----------------
    suspend fun recordUserCorrection(userId: Long, rawText: String, predicted: String, corrected: String) {
        val feedback = MLFeedbackEntity(
            userId = userId,
            rawText = rawText,
            predictedCategory = predicted,
            correctedCategory = corrected
        )
        mlFeedbackDao.insertFeedback(feedback)
        MultilingualNLP.learnFeedback(rawText, corrected)
    }

    suspend fun reloadLearnedFeedback(userId: Long) {
        val list = mlFeedbackDao.getAllFeedbackList(userId)
        for (item in list) {
            MultilingualNLP.learnFeedback(item.rawText, item.correctedCategory)
        }
    }

    fun getFeedbackCount(userId: Long): Flow<Int> = mlFeedbackDao.getFeedbackCount(userId)

    // ---------------- Receipt Scans ----------------
    suspend fun saveReceipt(receipt: ReceiptScanEntity): Long = receiptDao.insertReceipt(receipt)
    fun getAllReceipts(userId: Long): Flow<List<ReceiptScanEntity>> = receiptDao.getAllReceipts(userId)

    // ---------------- Trip Planning & Expenses ----------------
    fun getTripsForUser(userId: Long): Flow<List<TripEntity>> = tripDao.getTripsForUser(userId)
    fun getActiveTrip(userId: Long): Flow<TripEntity?> = tripDao.getActiveTrip(userId)
    suspend fun getTripById(tripId: Long): TripEntity? = tripDao.getTripById(tripId)
    suspend fun saveTrip(trip: TripEntity): Long = tripDao.insertTrip(trip)
    suspend fun updateTrip(trip: TripEntity) = tripDao.updateTrip(trip)
    suspend fun deleteTrip(trip: TripEntity) {
        tripExpenseDao.deleteAllExpensesForTrip(trip.id)
        tripPlanItemDao.deleteAllPlanItemsForTrip(trip.id)
        tripDao.deleteTrip(trip)
    }
    suspend fun markTripCompleted(tripId: Long) = tripDao.markTripCompleted(tripId)

    fun getExpensesForTrip(tripId: Long): Flow<List<TripExpenseEntity>> = tripExpenseDao.getExpensesForTrip(tripId)
    fun getTotalSpentForTrip(tripId: Long): Flow<Double> = tripExpenseDao.getTotalSpentForTrip(tripId)
    suspend fun saveTripExpense(expense: TripExpenseEntity): Long = tripExpenseDao.insertExpense(expense)
    suspend fun updateTripExpense(expense: TripExpenseEntity) = tripExpenseDao.updateExpense(expense)
    suspend fun deleteTripExpense(expense: TripExpenseEntity) = tripExpenseDao.deleteExpense(expense)

    fun getPlanItemsForTrip(tripId: Long): Flow<List<TripPlanItemEntity>> = tripPlanItemDao.getPlanItemsForTrip(tripId)
    suspend fun savePlanItem(item: TripPlanItemEntity): Long = tripPlanItemDao.insertPlanItem(item)
    suspend fun togglePlanItemDone(id: Long, isDone: Boolean) = tripPlanItemDao.toggleDone(id, isDone)
    suspend fun deletePlanItem(item: TripPlanItemEntity) = tripPlanItemDao.deletePlanItem(item)
}
