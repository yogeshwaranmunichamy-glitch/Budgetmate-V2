package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.BudgetEntity
import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataEntryEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.MLFeedbackEntity
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
    private val shopDao = database.shopDao()
    private val paymentReminderDao = database.paymentReminderDao()

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

    suspend fun getUserByEmail(email: String): UserEntity? = userDao.getUserByEmail(email.lowercase(Locale.ROOT).trim())
    suspend fun getFirstUser(): UserEntity? = userDao.getFirstUser()

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

    // ---------------- Shop & Small Industry Management ----------------

    // Shop Profile
    fun getShopProfile(userId: Long): Flow<ShopProfileEntity?> = shopDao.getProfileFlow(userId)
    suspend fun getShopProfileSync(userId: Long): ShopProfileEntity? = shopDao.getProfile(userId)
    suspend fun saveShopProfile(profile: ShopProfileEntity): Long = shopDao.insertOrUpdateProfile(profile)

    // Khata Parties (Customers & Suppliers)
    fun getAllKhataParties(userId: Long): Flow<List<KhataPartyEntity>> = shopDao.getAllParties(userId)
    fun getKhataPartiesByType(userId: Long, type: String): Flow<List<KhataPartyEntity>> = shopDao.getPartiesByType(userId, type)
    suspend fun getKhataPartyById(partyId: Long): KhataPartyEntity? = shopDao.getPartyById(partyId)
    suspend fun saveKhataParty(party: KhataPartyEntity): Long = shopDao.insertParty(party)
    suspend fun updateKhataParty(party: KhataPartyEntity) = shopDao.updateParty(party)
    suspend fun deleteKhataParty(party: KhataPartyEntity) = shopDao.deleteParty(party)

    // Khata Entries (Transactions)
    fun getKhataEntriesForParty(partyId: Long): Flow<List<KhataEntryEntity>> = shopDao.getEntriesForParty(partyId)
    fun getRecentKhataEntries(userId: Long): Flow<List<KhataEntryEntity>> = shopDao.getRecentEntries(userId)

    suspend fun recordKhataEntry(entry: KhataEntryEntity): Long {
        val entryId = shopDao.insertKhataEntry(entry)
        val party = shopDao.getPartyById(entry.partyId)
        if (party != null) {
            // Delta calculation:
            // For Customer: GAVE (Udhar given) -> balance increases (+). GOT (Payment received) -> balance decreases (-).
            // For Supplier: GOT (Goods received on credit) -> payable increases (+). GAVE (Payment made to supplier) -> payable decreases (-).
            val delta = if (party.type == "CUSTOMER") {
                if (entry.type == "GAVE") entry.amount else -entry.amount
            } else {
                if (entry.type == "GOT") entry.amount else -entry.amount
            }
            shopDao.updatePartyBalance(party.id, delta, entry.date)
        }
        return entryId
    }

    // Inventory & Stock
    fun getAllInventoryItems(userId: Long): Flow<List<InventoryItemEntity>> = shopDao.getAllItems(userId)
    fun getLowStockInventoryItems(userId: Long): Flow<List<InventoryItemEntity>> = shopDao.getLowStockItems(userId)
    fun searchInventoryItems(userId: Long, query: String): Flow<List<InventoryItemEntity>> = shopDao.searchItems(userId, query)
    suspend fun getInventoryItemById(itemId: Long): InventoryItemEntity? = shopDao.getItemById(itemId)
    suspend fun saveInventoryItem(item: InventoryItemEntity): Long = shopDao.insertItem(item)
    suspend fun updateInventoryItem(item: InventoryItemEntity) = shopDao.updateItem(item)
    suspend fun deleteInventoryItem(item: InventoryItemEntity) = shopDao.deleteItem(item)

    suspend fun recordStockMovement(
        userId: Long,
        itemId: Long,
        type: String, // "STOCK_IN" or "STOCK_OUT"
        quantity: Double,
        unitPrice: Double = 0.0,
        reason: String = "Sale",
        note: String = ""
    ): Long {
        val deltaQty = if (type == "STOCK_IN") quantity else -quantity
        shopDao.updateItemStock(itemId, deltaQty)
        val movement = StockMovementEntity(
            userId = userId,
            itemId = itemId,
            type = type,
            quantity = quantity,
            unitPrice = unitPrice,
            reason = reason,
            note = note,
            date = System.currentTimeMillis()
        )
        return shopDao.insertMovement(movement)
    }

    fun getStockMovementsForItem(itemId: Long): Flow<List<StockMovementEntity>> = shopDao.getMovementsForItem(itemId)
    fun getRecentStockMovements(userId: Long): Flow<List<StockMovementEntity>> = shopDao.getRecentMovements(userId)

    // Shop Invoices / Billing
    fun getAllShopInvoices(userId: Long): Flow<List<ShopInvoiceEntity>> = shopDao.getAllInvoices(userId)
    suspend fun getShopInvoiceById(invoiceId: Long): ShopInvoiceEntity? = shopDao.getInvoiceById(invoiceId)
    suspend fun saveShopInvoice(
        invoice: ShopInvoiceEntity,
        autoCreateKhataEntry: Boolean = false
    ): Long {
        val invoiceId = shopDao.insertInvoice(invoice)
        // If credit payment and linked to party, record khata entry
        if (autoCreateKhataEntry && invoice.partyId != null && invoice.paymentStatus != "PAID") {
            val unpaidAmount = invoice.grandTotal - invoice.paidAmount
            if (unpaidAmount > 0) {
                recordKhataEntry(
                    KhataEntryEntity(
                        userId = invoice.userId,
                        partyId = invoice.partyId,
                        type = if (invoice.type == "SALE") "GAVE" else "GOT",
                        amount = unpaidAmount,
                        description = "Bill #${invoice.invoiceNumber}",
                        billNumber = invoice.invoiceNumber,
                        paymentMethod = invoice.paymentMode,
                        date = invoice.date
                    )
                )
            }
        }
        return invoiceId
    }

    // Daily Cash Register (Galla)
    fun getLatestCashRegister(userId: Long): Flow<DailyCashRegisterEntity?> = shopDao.getLatestRegister(userId)
    suspend fun saveOrUpdateCashRegister(register: DailyCashRegisterEntity): Long =
        shopDao.insertOrUpdateRegister(register)

    // Seed realistic starter data for Shop & Small Industry
    suspend fun seedStarterShopDataIfEmpty(userId: Long) {
        val existingProfile = shopDao.getProfile(userId)
        if (existingProfile != null) return

        // 1. Create Default Profile
        val profile = ShopProfileEntity(
            userId = userId,
            businessName = "Sri Murugan Store & Workshop",
            ownerName = "Sundar",
            phone = "9876543210",
            address = "42, Bazaar Main Road, Industrial Estate",
            gstin = "33AAAAA1234A1Z5",
            businessType = "Kirana & Retail",
            openingCash = 5000.0,
            isGstEnabled = true,
            upiId = "srimurugan@upi"
        )
        shopDao.insertOrUpdateProfile(profile)

        // 2. Customers
        val cust1 = shopDao.insertParty(
            KhataPartyEntity(
                userId = userId,
                name = "Ramesh Kumar",
                phone = "9840123456",
                type = "CUSTOMER",
                address = "12, South Street",
                currentBalance = 1450.0,
                creditLimit = 10000.0,
                notes = "Regular customer, pays weekly"
            )
        )
        val cust2 = shopDao.insertParty(
            KhataPartyEntity(
                userId = userId,
                name = "Anand Builders & Fabricators",
                phone = "9789012345",
                type = "CUSTOMER",
                address = "Plot 8, Small Industries Zone",
                currentBalance = 4200.0,
                creditLimit = 30000.0,
                notes = "Small scale construction contractor"
            )
        )
        shopDao.insertParty(
            KhataPartyEntity(
                userId = userId,
                name = "Kavitha Provisions",
                phone = "9940123789",
                type = "CUSTOMER",
                address = "Station Road",
                currentBalance = 0.0,
                creditLimit = 15000.0,
                notes = "Cash on delivery customer"
            )
        )

        // Add sample Khata entries for cust1 and cust2
        shopDao.insertKhataEntry(
            KhataEntryEntity(
                userId = userId,
                partyId = cust1,
                type = "GAVE",
                amount = 2000.0,
                description = "Provisions & Rice 25kg",
                billNumber = "BILL-101",
                paymentMethod = "Credit",
                date = System.currentTimeMillis() - 86400000L * 3
            )
        )
        shopDao.insertKhataEntry(
            KhataEntryEntity(
                userId = userId,
                partyId = cust1,
                type = "GOT",
                amount = 550.0,
                description = "GPay received",
                paymentMethod = "UPI",
                date = System.currentTimeMillis() - 86400000L * 1
            )
        )

        shopDao.insertKhataEntry(
            KhataEntryEntity(
                userId = userId,
                partyId = cust2,
                type = "GAVE",
                amount = 4200.0,
                description = "Steel bars & welding rods batch",
                billNumber = "BILL-102",
                paymentMethod = "Credit",
                date = System.currentTimeMillis() - 86400000L * 2
            )
        )

        // 3. Suppliers
        val supp1 = shopDao.insertParty(
            KhataPartyEntity(
                userId = userId,
                name = "Balaji Steel & Hardware Wholesale",
                phone = "9444112233",
                type = "SUPPLIER",
                address = "Wholesale Market, Gate 3",
                currentBalance = 8500.0, // We owe them 8,500
                creditLimit = 100000.0,
                notes = "Primary raw materials supplier"
            )
        )
        shopDao.insertParty(
            KhataPartyEntity(
                userId = userId,
                name = "Om Shakti FMCG Distributors",
                phone = "9388223344",
                type = "SUPPLIER",
                address = "Depot No. 5, Ring Road",
                currentBalance = 3200.0,
                creditLimit = 50000.0,
                notes = "Fortnightly settlement"
            )
        )

        shopDao.insertKhataEntry(
            KhataEntryEntity(
                userId = userId,
                partyId = supp1,
                type = "GOT",
                amount = 8500.0,
                description = "Steel rods & fasteners delivery",
                billNumber = "SUPP-902",
                paymentMethod = "Credit",
                date = System.currentTimeMillis() - 86400000L * 4
            )
        )

        // 4. Inventory items (for both Retail & Small Scale Industry)
        val item1 = shopDao.insertItem(
            InventoryItemEntity(
                userId = userId,
                name = "Basmati Rice 25kg Bag",
                sku = "RICE-25K",
                category = "Retail Goods",
                unit = "Bag",
                purchasePrice = 1800.0,
                sellingPrice = 2150.0,
                currentStock = 18.0,
                minStockAlert = 5.0,
                taxRate = 5.0
            )
        )
        val item2 = shopDao.insertItem(
            InventoryItemEntity(
                userId = userId,
                name = "Steel Bar 12mm TMT",
                sku = "STL-12MM",
                category = "Raw Materials",
                unit = "Pcs",
                purchasePrice = 450.0,
                sellingPrice = 520.0,
                currentStock = 45.0,
                minStockAlert = 10.0,
                taxRate = 18.0
            )
        )
        val item3 = shopDao.insertItem(
            InventoryItemEntity(
                userId = userId,
                name = "Copper Wire 2.5mm Coil",
                sku = "CPR-25M",
                category = "Spares & Tools",
                unit = "Box",
                purchasePrice = 1200.0,
                sellingPrice = 1450.0,
                currentStock = 3.0, // Low stock!
                minStockAlert = 5.0,
                taxRate = 18.0
            )
        )
        val item4 = shopDao.insertItem(
            InventoryItemEntity(
                userId = userId,
                name = "Refined Cooking Oil 15L Tin",
                sku = "OIL-15L",
                category = "Retail Goods",
                unit = "Tin",
                purchasePrice = 1650.0,
                sellingPrice = 1850.0,
                currentStock = 14.0,
                minStockAlert = 4.0,
                taxRate = 5.0
            )
        )
        val item5 = shopDao.insertItem(
            InventoryItemEntity(
                userId = userId,
                name = "Fabricated MS Bracket (Finished)",
                sku = "MS-BRK-01",
                category = "Finished Goods",
                unit = "Pcs",
                purchasePrice = 140.0,
                sellingPrice = 220.0,
                currentStock = 60.0,
                minStockAlert = 15.0,
                taxRate = 18.0
            )
        )

        // Record initial stock movements
        shopDao.insertMovement(StockMovementEntity(userId = userId, itemId = item1, type = "STOCK_IN", quantity = 20.0, unitPrice = 1800.0, reason = "Purchase", note = "Initial stock intake"))
        shopDao.insertMovement(StockMovementEntity(userId = userId, itemId = item2, type = "STOCK_IN", quantity = 50.0, unitPrice = 450.0, reason = "Purchase", note = "Supplier batch #481"))

        // 5. Sample Invoices
        shopDao.insertInvoice(
            ShopInvoiceEntity(
                userId = userId,
                invoiceNumber = "INV-2026-001",
                partyId = cust1,
                partyName = "Ramesh Kumar",
                partyPhone = "9840123456",
                type = "SALE",
                subtotal = 2150.0,
                discountAmount = 50.0,
                taxRate = 5.0,
                taxAmount = 105.0,
                grandTotal = 2205.0,
                paidAmount = 755.0,
                paymentStatus = "PARTIAL",
                paymentMode = "UPI",
                itemsJson = """[{"name":"Basmati Rice 25kg Bag","qty":1.0,"unit":"Bag","rate":2150.0,"total":2150.0}]""",
                notes = "Delivered to shop, remaining on credit",
                date = System.currentTimeMillis() - 86400000L * 1
            )
        )

        // 6. Today's Cash Register
        shopDao.insertOrUpdateRegister(
            DailyCashRegisterEntity(
                userId = userId,
                date = System.currentTimeMillis(),
                openingCash = 5000.0,
                cashSales = 4850.0,
                cashKhataCollected = 1200.0,
                cashShopExpenses = 650.0,
                cashSupplierPaid = 2000.0,
                upiCollected = 3400.0,
                bankCollected = 0.0,
                actualClosingCash = 8400.0,
                notes = "Drawer tallies with physical cash count"
            )
        )
    }

    // ---------------- Payment & Loan Reminders ----------------
    fun getAllPaymentReminders(userId: Long): Flow<List<PaymentReminderEntity>> =
        paymentReminderDao.getAllReminders(userId)

    fun getPendingPaymentReminders(userId: Long): Flow<List<PaymentReminderEntity>> =
        paymentReminderDao.getPendingReminders(userId)

    suspend fun savePaymentReminder(reminder: PaymentReminderEntity): Long =
        paymentReminderDao.insertReminder(reminder)

    suspend fun updatePaymentReminder(reminder: PaymentReminderEntity) =
        paymentReminderDao.updateReminder(reminder)

    suspend fun deletePaymentReminder(reminder: PaymentReminderEntity) =
        paymentReminderDao.deleteReminder(reminder)

    suspend fun markPaymentReminderCompleted(id: Long) =
        paymentReminderDao.markCompleted(id)

    suspend fun snoozePaymentReminder(id: Long, newDueDate: Long) =
        paymentReminderDao.snoozeReminder(id, newDueDate)

    suspend fun togglePaymentReminderAlarm(id: Long, enabled: Boolean) =
        paymentReminderDao.toggleAlarm(id, enabled)

    suspend fun seedStarterPaymentRemindersIfEmpty(userId: Long) {
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        val list = listOf(
            PaymentReminderEntity(
                userId = userId,
                title = "Ramesh Udhar Repayment",
                personOrEntity = "Ramesh Kumar",
                amount = 500.0,
                reminderType = "TO_COLLECT",
                dueDate = now + (dayMillis * 1), // Tomorrow
                reminderTime = "10:00 AM",
                isAlarmEnabled = true,
                priority = "HIGH",
                phoneNumber = "9840123456",
                notes = "Borrowed 500 for emergency grocery, promised to repay by tomorrow",
                status = "PENDING"
            ),
            PaymentReminderEntity(
                userId = userId,
                title = "HDFC Car Loan EMI",
                personOrEntity = "HDFC Bank",
                amount = 12500.0,
                reminderType = "TO_PAY",
                dueDate = now + (dayMillis * 3), // In 3 days
                reminderTime = "09:00 AM",
                isAlarmEnabled = true,
                priority = "URGENT",
                phoneNumber = "18002026161",
                notes = "Auto-debit from HDFC Salary Account (Account ending 4892)",
                status = "PENDING",
                repeatInterval = "MONTHLY"
            ),
            PaymentReminderEntity(
                userId = userId,
                title = "Suresh Goods Bill Payment",
                personOrEntity = "Suresh Sharma",
                amount = 1800.0,
                reminderType = "TO_COLLECT",
                dueDate = now + (dayMillis * 2), // In 2 days
                reminderTime = "04:30 PM",
                isAlarmEnabled = true,
                priority = "NORMAL",
                phoneNumber = "9840987654",
                notes = "Retail goods delivered on credit, pending UPI payment",
                status = "PENDING"
            ),
            PaymentReminderEntity(
                userId = userId,
                title = "Shop Electricity Bill",
                personOrEntity = "TNEB Power Dept",
                amount = 2450.0,
                reminderType = "BILL",
                dueDate = now + (dayMillis * 5),
                reminderTime = "11:00 AM",
                isAlarmEnabled = true,
                priority = "NORMAL",
                phoneNumber = "",
                notes = "Consumer No: 04-239-112-984. Pay before due date to avoid surcharge",
                status = "PENDING"
            )
        )
        for (rem in list) {
            paymentReminderDao.insertReminder(rem)
        }
    }
}
