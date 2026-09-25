package com.example.ml

import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

data class TripCategorySpend(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int,
    val iconEmoji: String
)

data class CompanionBalance(
    val name: String,
    val totalPaid: Double,
    val totalShare: Double,
    val netBalance: Double // Positive = gets back money, Negative = owes money
)

data class DebtSettlement(
    val from: String, // Owes money
    val to: String,   // Receives money
    val amount: Double
)

data class TripBudgetAnalytics(
    val totalBudget: Double,
    val totalSpent: Double,
    val remainingBudget: Double,
    val percentSpent: Float,
    val isOverBudget: Boolean,
    val dailyBurnRate: Double,
    val projectedTotal: Double,
    val daysTotal: Int,
    val daysElapsed: Int,
    val categorySpends: List<TripCategorySpend>,
    val balances: List<CompanionBalance>,
    val settlements: List<DebtSettlement>
)

data class ParsedTripVoiceExpense(
    val title: String,
    val amount: Double,
    val category: String,
    val paidBy: String,
    val paymentMethod: String,
    val confidenceScore: Float,
    val rawText: String
)

enum class TripVoiceQueryType {
    REMAINING_BUDGET,
    CATEGORY_SPEND,
    TOTAL_EXPENSE,
    WHO_OWES_WHOM,
    DAILY_PACE,
    UNKNOWN
}

data class ParsedTripVoiceQuery(
    val queryType: TripVoiceQueryType,
    val targetCategory: String? = null,
    val rawText: String
)

object TripAnalyticsEngine {

    val TRIP_CATEGORIES = listOf(
        "Stay & Hotel",
        "Flights & Travel",
        "Food & Dining",
        "Activities & Sightseeing",
        "Local Commute",
        "Shopping & Souvenirs",
        "Fuel",
        "Drinks & Party",
        "Emergency / Misc"
    )

    fun getCategoryEmoji(cat: String): String {
        return when (cat) {
            "Stay & Hotel" -> "🏨"
            "Flights & Travel" -> "✈️"
            "Food & Dining" -> "🍽️"
            "Activities & Sightseeing" -> "🎟️"
            "Local Commute" -> "🚕"
            "Shopping & Souvenirs" -> "🛍️"
            "Fuel" -> "⛽"
            "Drinks & Party" -> "🍹"
            else -> "💼"
        }
    }

    /**
     * Compute comprehensive category-wise spending, budget utilization, and splitwise settlements
     */
    fun analyzeTrip(trip: TripEntity, expenses: List<TripExpenseEntity>): TripBudgetAnalytics {
        val totalSpent = expenses.sumOf { it.amount }
        val remainingBudget = (trip.budget - totalSpent).coerceAtLeast(0.0)
        val percentSpent = if (trip.budget > 0) ((totalSpent / trip.budget) * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f
        val isOverBudget = totalSpent > trip.budget

        // Time calculations
        val now = System.currentTimeMillis()
        val tripStart = trip.startDate
        val tripEnd = trip.endDate.coerceAtLeast(tripStart + 86400000L)
        val totalDays = ((tripEnd - tripStart) / 86400000L).toInt().coerceAtLeast(1)
        val elapsedDays = if (now < tripStart) 1 else (((now - tripStart) / 86400000L).toInt() + 1).coerceIn(1, totalDays)

        val dailyBurnRate = if (elapsedDays > 0) totalSpent / elapsedDays else totalSpent
        val projectedTotal = dailyBurnRate * totalDays

        // Category-wise Breakdown
        val categorySpends = TRIP_CATEGORIES.mapNotNull { cat ->
            val catExpenses = expenses.filter { it.category.equals(cat, ignoreCase = true) }
            val amount = catExpenses.sumOf { it.amount }
            if (amount > 0) {
                val pct = if (totalSpent > 0) ((amount / totalSpent) * 100.0).toFloat() else 0f
                TripCategorySpend(
                    category = cat,
                    amount = amount,
                    percentage = pct,
                    count = catExpenses.size,
                    iconEmoji = getCategoryEmoji(cat)
                )
            } else null
        }.sortedByDescending { it.amount }

        // Companions & Balances
        val companionsList = trip.companions.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf("Me") }

        val paidMap = mutableMapOf<String, Double>()
        val shareMap = mutableMapOf<String, Double>()

        companionsList.forEach {
            paidMap[it] = 0.0
            shareMap[it] = 0.0
        }

        for (exp in expenses) {
            val payer = companionsList.firstOrNull { it.equals(exp.paidBy, ignoreCase = true) } ?: exp.paidBy
            paidMap[payer] = (paidMap[payer] ?: 0.0) + exp.amount

            // Compute split
            val splitMembers = if (exp.splitAmong.equals("All", ignoreCase = true) || exp.splitAmong.isBlank()) {
                companionsList
            } else {
                exp.splitAmong.split(",").map { it.trim() }.filter { it.isNotBlank() }
            }.ifEmpty { companionsList }

            val sharePerPerson = exp.amount / splitMembers.size
            for (m in splitMembers) {
                val matched = companionsList.firstOrNull { it.equals(m, ignoreCase = true) } ?: m
                shareMap[matched] = (shareMap[matched] ?: 0.0) + sharePerPerson
            }
        }

        val balances = companionsList.map { name ->
            val paid = paidMap[name] ?: 0.0
            val share = shareMap[name] ?: 0.0
            CompanionBalance(
                name = name,
                totalPaid = paid,
                totalShare = share,
                netBalance = paid - share
            )
        }

        // Simplify Debts (Splitwise greedy balance algorithm)
        val settlements = simplifyDebts(balances)

        return TripBudgetAnalytics(
            totalBudget = trip.budget,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            percentSpent = percentSpent,
            isOverBudget = isOverBudget,
            dailyBurnRate = dailyBurnRate,
            projectedTotal = projectedTotal,
            daysTotal = totalDays,
            daysElapsed = elapsedDays,
            categorySpends = categorySpends,
            balances = balances,
            settlements = settlements
        )
    }

    /**
     * Greedy algorithm to settle debts with minimum number of payments
     */
    private fun simplifyDebts(balances: List<CompanionBalance>): List<DebtSettlement> {
        val debtors = mutableListOf<Pair<String, Double>>()   // balance < 0 (owes money)
        val creditors = mutableListOf<Pair<String, Double>>() // balance > 0 (gets back)

        for (b in balances) {
            val net = kotlin.math.round(b.netBalance * 100.0) / 100.0
            if (net < -0.01) debtors.add(b.name to abs(net))
            else if (net > 0.01) creditors.add(b.name to net)
        }

        val settlements = mutableListOf<DebtSettlement>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val (debtor, dAmt) = debtors[dIdx]
            val (creditor, cAmt) = creditors[cIdx]

            val settleAmount = minOf(dAmt, cAmt)
            settlements.add(DebtSettlement(from = debtor, to = creditor, amount = settleAmount))

            val remainingDebit = dAmt - settleAmount
            val remainingCredit = cAmt - settleAmount

            if (remainingDebit <= 0.01) dIdx++ else debtors[dIdx] = debtor to remainingDebit
            if (remainingCredit <= 0.01) cIdx++ else creditors[cIdx] = creditor to remainingCredit
        }

        return settlements
    }

    /**
     * Traditional NLP extraction of trip expense from voice transcript in English, Tamil, Hindi, Tanglish, Hinglish
     */
    fun parseTripVoiceExpense(rawText: String, companions: List<String>): ParsedTripVoiceExpense {
        val lower = rawText.lowercase(Locale.ROOT).trim()

        // 1. Extract Amount
        var amount = 0.0
        val numPattern = Regex("""(?:\b|₹|rs\.?|rupees|inr)\s*(\d+(?:[.,]\d+)?)(?:\s*(?:k|thousand|rupees|rs\.?|roobai|rubai))?""", RegexOption.IGNORE_CASE)
        val match = numPattern.find(lower)
        if (match != null) {
            val rawNum = match.groupValues[1].replace(",", "")
            val parsedVal = rawNum.toDoubleOrNull() ?: 0.0
            amount = if (lower.contains("k") && !lower.contains("cab") && !lower.contains("snack")) parsedVal * 1000.0 else parsedVal
        }
        if (amount <= 0.0) {
            val simpleDigit = Regex("""\b\d+\b""").find(lower)
            amount = simpleDigit?.value?.toDoubleOrNull() ?: 500.0
        }

        // 2. Extract Category
        var matchedCat = "Food & Dining"
        var bestWeight = 0

        val categoryKeywords = mapOf(
            "Stay & Hotel" to listOf("hotel", "resort", "room", "stay", "villa", "lodge", "cottage", "airbnb", "hostel", "checkin", "stayed", "தங்குமிடம்", "விடுதி", "होटल", "कमरा"),
            "Flights & Travel" to listOf("flight", "ticket", "airline", "indigo", "air india", "train", "tatkal", "bus", "sleeper", "irctc", "விமானம்", "ரயில்", "டிக்கெட்", "फ्लाइट", "हवाई जहाज"),
            "Food & Dining" to listOf("food", "dinner", "lunch", "breakfast", "cafe", "restaurant", "seafood", "maggi", "tea", "coffee", "beer", "sappadu", "hotel-la", "khana", "dhaba", "சாப்பாடு", "உணவு", "खाना"),
            "Activities & Sightseeing" to listOf("scuba", "diving", "safari", "trek", "trekking", "monument", "entry fee", "museum", "boating", "sports", "water sports", "sightseeing", "guide", "விளையாட்டு", "டிக்கெட்"),
            "Local Commute" to listOf("cab", "taxi", "uber", "ola", "auto", "bike rent", "scooter", "jeep rent", "ferry", "rented bike", "vadagai", "किराया"),
            "Shopping & Souvenirs" to listOf("shopping", "souvenir", "gift", "market", "t-shirt", "handicraft", "bazaar", "ஷாப்பிங்", "खरीदारी"),
            "Fuel" to listOf("petrol", "diesel", "fuel", "gas", "petrol bunk", "bunk", "பெட்ரோல்", "पेट्रोल"),
            "Drinks & Party" to listOf("drinks", "party", "club", "pub", "bar", "cocktail", "liquor", "beverage", "shack", "beach shack")
        )

        for ((cat, keywords) in categoryKeywords) {
            for (kw in keywords) {
                if (lower.contains(kw)) {
                    val weight = kw.length
                    if (weight > bestWeight) {
                        bestWeight = weight
                        matchedCat = cat
                    }
                }
            }
        }

        // 3. Extract Companion who Paid
        var paidBy = "Me"
        for (comp in companions) {
            val cLower = comp.lowercase(Locale.ROOT)
            if (lower.contains("$cLower paid") ||
                lower.contains("$cLower kuduthan") ||
                lower.contains("$cLower kudutharu") ||
                lower.contains("$cLower ne diya") ||
                lower.contains("paid by $cLower") ||
                lower.contains("by $cLower") ||
                lower.contains(cLower)
            ) {
                paidBy = comp
                break
            }
        }

        // 4. Extract Payment Method
        val paymentMethod = when {
            lower.contains("cash") || lower.contains("kaasu") || lower.contains("रोकड़ा") -> "Cash"
            lower.contains("card") || lower.contains("credit") || lower.contains("debit") -> "Credit Card"
            lower.contains("gpay") || lower.contains("phonepe") || lower.contains("paytm") || lower.contains("upi") -> "UPI"
            else -> "UPI"
        }

        // 5. Generate descriptive Title
        val title = when {
            lower.contains("dinner") -> "Dinner"
            lower.contains("lunch") -> "Lunch"
            lower.contains("breakfast") -> "Breakfast"
            lower.contains("resort") -> "Resort Stay"
            lower.contains("hotel") -> "Hotel Room"
            lower.contains("cab") -> "Cab Travel"
            lower.contains("scuba") -> "Scuba Diving"
            lower.contains("fuel") || lower.contains("petrol") -> "Fuel Refill"
            lower.contains("flight") -> "Flight Tickets"
            lower.contains("bike") -> "Bike Rental"
            else -> "$matchedCat Expense"
        }

        val confidence = if (bestWeight > 0) 0.88f else 0.65f

        return ParsedTripVoiceExpense(
            title = title,
            amount = amount,
            category = matchedCat,
            paidBy = paidBy,
            paymentMethod = paymentMethod,
            confidenceScore = confidence,
            rawText = rawText
        )
    }

    /**
     * Process trip voice queries
     */
    fun parseTripVoiceQuery(query: String): ParsedTripVoiceQuery {
        val lower = query.lowercase(Locale.ROOT).trim()

        // Check for specific category
        var matchedCat: String? = null
        for (cat in TRIP_CATEGORIES) {
            val keyword = cat.split("&").first().trim().lowercase(Locale.ROOT)
            if (lower.contains(keyword) || lower.contains(cat.lowercase(Locale.ROOT))) {
                matchedCat = cat
                break
            }
        }
        if (matchedCat == null) {
            when {
                lower.contains("food") || lower.contains("sappadu") || lower.contains("khana") || lower.contains("dinner") -> matchedCat = "Food & Dining"
                lower.contains("hotel") || lower.contains("room") || lower.contains("stay") -> matchedCat = "Stay & Hotel"
                lower.contains("cab") || lower.contains("travel") || lower.contains("commute") -> matchedCat = "Local Commute"
                lower.contains("fuel") || lower.contains("petrol") -> matchedCat = "Fuel"
            }
        }

        val queryType = when {
            lower.contains("who owes") || lower.contains("split") || lower.contains("settle") ||
                    lower.contains("yaaru kudukanum") || lower.contains("kisko dena hai") || lower.contains("balances") -> TripVoiceQueryType.WHO_OWES_WHOM

            matchedCat != null -> TripVoiceQueryType.CATEGORY_SPEND

            lower.contains("budget") || lower.contains("remaining") || lower.contains("meethi") ||
                    lower.contains("bacha") || lower.contains("balance") -> TripVoiceQueryType.REMAINING_BUDGET

            lower.contains("daily") || lower.contains("pace") || lower.contains("per day") || lower.contains("burn") -> TripVoiceQueryType.DAILY_PACE

            lower.contains("total") || lower.contains("how much spent") || lower.contains("evlo selavu") ||
                    lower.contains("kitna kharch") || lower.contains("cost") -> TripVoiceQueryType.TOTAL_EXPENSE

            else -> TripVoiceQueryType.UNKNOWN
        }

        return ParsedTripVoiceQuery(
            queryType = queryType,
            targetCategory = matchedCat,
            rawText = query
        )
    }
}
