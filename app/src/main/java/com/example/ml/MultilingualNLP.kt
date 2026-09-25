package com.example.ml

import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedTransactionResult(
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val category: String,
    val paymentMethod: String,
    val merchantOrSource: String,
    val date: Long,
    val description: String,
    val confidenceScore: Float,
    val uncertainFields: List<String>
)

object MultilingualNLP {

    // Categories
    const val CAT_FOOD = "Food"
    const val CAT_GROCERIES = "Groceries"
    const val CAT_TRANSPORT = "Transport"
    const val CAT_PETROL = "Petrol"
    const val CAT_SHOPPING = "Shopping"
    const val CAT_RENT = "Rent"
    const val CAT_ELECTRICITY = "Electricity"
    const val CAT_WATER = "Water"
    const val CAT_INTERNET = "Internet"
    const val CAT_MOBILE = "Mobile Recharge"
    const val CAT_MEDICAL = "Medical"
    const val CAT_EDUCATION = "Education"
    const val CAT_ENTERTAINMENT = "Entertainment"
    const val CAT_TRAVEL = "Travel"
    const val CAT_INSURANCE = "Insurance"
    const val CAT_EMI = "EMI"
    const val CAT_SUBSCRIPTIONS = "Subscriptions"
    const val CAT_OTHER = "Other"

    // Income Categories
    const val CAT_SALARY = "Salary"
    const val CAT_FREELANCE = "Freelance"
    const val CAT_BUSINESS = "Business"
    const val CAT_INTEREST = "Interest"
    const val CAT_BONUS = "Bonus"

    // Multi-lingual vocabulary keyword mappings (English, Tamil, Hindi, Tanglish, Hinglish)
    private val categoryKeywords = mapOf(
        CAT_FOOD to listOf(
            "food", "swiggy", "zomato", "restaurant", "hotel", "dinner", "lunch", "breakfast",
            "tea", "coffee", "chai", "snack", "snacks", "pizza", "burger", "biryani",
            "sappadu", "saapadu", "hotel-la", "thindi", "oota", "khana", "nashta", "bhojan",
            "சாப்பாடு", "உணவு", "ஹோட்டல்", "खाना", "भोजन", "नाश्ता", "रोटी"
        ),
        CAT_GROCERIES to listOf(
            "groceries", "grocery", "supermarket", "dmart", "blinkit", "zepto", "instamart",
            "vegetables", "veggies", "fruits", "milk", "curd", "maligai", "provisions", "ration",
            "kirana", "sabji", "doodh", "fal", "காய்கறி", "மளிகை", "பால்", "किराना", "सब्जी", "दूध"
        ),
        CAT_PETROL to listOf(
            "petrol", "diesel", "fuel", "gas", "bunk", "shell", "hp petrol", "indian oil",
            "bharat petroleum", "cng", "பெட்ரோல்", "டீசல்", "पेट्रोल", "डीजल", "ईंधन"
        ),
        CAT_TRANSPORT to listOf(
            "transport", "uber", "ola", "rapido", "auto", "cab", "taxi", "bus", "train",
            "metro", "flight ticket", "vadagai", "fare", "ticket", "கட்டணம்", "பயணம்", "ऑटो", "किराया"
        ),
        CAT_SHOPPING to listOf(
            "shopping", "amazon", "flipkart", "myntra", "meesho", "zudio", "clothes",
            "dress", "shirt", "shoes", "cloth", "purchase", "kadai", "mall", "thuni",
            "துணி", "ஷாப்பிங்", "कपड़े", "खरीदारी"
        ),
        CAT_RENT to listOf(
            "rent", "house rent", "room rent", "flat rent", "vadagai", "kiraya",
            "வாடகை", "வீட்டு வாடகை", "किराया", "मकान किराया"
        ),
        CAT_ELECTRICITY to listOf(
            "electricity", "eb", "eb bill", "current bill", "tneb", "bescom", "power bill",
            "bijli", "bijli bill", "மின்சாரம்", "மின் கட்டணம்", "बिजली", "बिजली बिल"
        ),
        CAT_WATER to listOf(
            "water", "water bill", "tanker", "can water", "pani", "தண்ணீர்", "குடிநீர்", "पानी"
        ),
        CAT_INTERNET to listOf(
            "internet", "wifi", "broadband", "fiber", "act fibernet", "airtel xtream", "jio fiber",
            "இன்டர்நெட்", "वाइफ़ाई"
        ),
        CAT_MOBILE to listOf(
            "mobile", "recharge", "phone recharge", "airtel", "jio", "vi", "vodafone",
            "ரீசார்ஜ்", "रिचार्ज"
        ),
        CAT_MEDICAL to listOf(
            "medical", "doctor", "hospital", "medicine", "tablets", "pharmacy", "apollo",
            "clinic", "health", "marundhu", "dawa", "dawai", "மருந்து", "மருத்துவமனை", "दवा", "इलाज"
        ),
        CAT_EDUCATION to listOf(
            "school", "college", "fees", "books", "tuition", "course", "exam", "education",
            "padipu", "padhai", "பள்ளி", "படிப்பு", "शिक्षा", "फीस", "किताब"
        ),
        CAT_ENTERTAINMENT to listOf(
            "entertainment", "movie", "cinema", "theatre", "pvr", "inox", "game", "outing",
            "thirai", "திரைப்படம்", "सिनेमा", "फिल्म", "मनोरंजन"
        ),
        CAT_TRAVEL to listOf(
            "travel", "trip", "tour", "hotel booking", "vacation", "makemytrip", "goibibo",
            "irctc", "சுற்றுலா", "यात्रा", "सफर"
        ),
        CAT_INSURANCE to listOf(
            "insurance", "lic", "policy", "premium", "health insurance", "காப்பீடு", "बीमा"
        ),
        CAT_EMI to listOf(
            "emi", "loan", "installment", "car emi", "home loan", "கடன்", "किस्त"
        ),
        CAT_SUBSCRIPTIONS to listOf(
            "netflix", "prime", "spotify", "youtube", "hotstar", "subscription", "apple music",
            "சந்தா", "सब्सक्रिप्शन"
        ),
        CAT_SALARY to listOf(
            "salary", "sambalam", "monthly pay", "stipend", "வேலை சம்பளம்", "சம்பளம்", "वेतन", "तनख्वाह"
        ),
        CAT_FREELANCE to listOf(
            "freelance", "upwork", "fiverr", "client payment", "gig", "பகுதி நேரம்", "फ्रीलांस"
        ),
        CAT_BUSINESS to listOf(
            "business", "shop revenue", "sales", "profit", "வியாபாரம்", "व्यापार", "दुकान"
        ),
        CAT_BONUS to listOf(
            "bonus", "incentive", "gift", "பரிசு", "बोनस", "उपहार"
        ),
        CAT_INTEREST to listOf(
            "interest", "dividend", "fd interest", "வட்டி", "ब्याज"
        )
    )

    private val incomeKeywords = listOf(
        "salary", "income", "credit", "credited", "received", "earned", "vandhudhu",
        "vandhadhu", "varavu", "mila", "aaya", "kamaya", "stipend", "profit",
        "வரவு", "வந்தது", "கிடைத்தது", "सैलरी", "आया", "मिला", "कमाई"
    )

    private val paymentMethodKeywords = mapOf(
        "UPI" to listOf("gpay", "google pay", "phonepe", "paytm", "upi", "bhim", "qr code", "scanned"),
        "Cash" to listOf("cash", "panam", "nagad", "kaasu", "பணம்", "நொடி", "नकद", "कैश"),
        "Credit Card" to listOf("credit card", "cc", "hdfc card", "icici card", "க்ரெடிட் கார்டு"),
        "Debit Card" to listOf("debit card", "atm card", "டெபிட் கார்டு"),
        "Bank Transfer" to listOf("bank transfer", "neft", "imps", "rtgs", "account transfer", "net banking")
    )

    // User correction dynamic memory for local learning
    private val learnedWeights = mutableMapOf<String, String>()

    fun learnFeedback(rawText: String, correctedCategory: String) {
        val tokens = tokenize(rawText)
        for (token in tokens) {
            if (token.length > 2) {
                learnedWeights[token.lowercase(Locale.ROOT)] = correctedCategory
            }
        }
    }

    fun getLearnedMemoryCount(): Int = learnedWeights.size

    fun parseInput(input: String): ParsedTransactionResult {
        val cleanInput = input.trim()
        val tokens = tokenize(cleanInput)
        val lowerText = cleanInput.lowercase(Locale.ROOT)

        // 1. Extract Amount
        val extractedAmount = extractAmount(cleanInput)
        val amount = extractedAmount ?: 0.0

        // 2. Classify Transaction Type (INCOME vs EXPENSE)
        var isIncome = false
        for (kw in incomeKeywords) {
            if (lowerText.contains(kw)) {
                isIncome = true
                break
            }
        }
        val type = if (isIncome) "INCOME" else "EXPENSE"

        // 3. Classify Category with keyword scoring & learned feedback weights
        var bestCategory = if (isIncome) CAT_SALARY else CAT_FOOD
        var highestScore = 0.0
        val uncertainFields = mutableListOf<String>()

        // Check user learned weights first (Direct reinforcement)
        var learnedMatchFound = false
        for (token in tokens) {
            val correctedCat = learnedWeights[token]
            if (correctedCat != null) {
                bestCategory = correctedCat
                highestScore = 3.5
                learnedMatchFound = true
                break
            }
        }

        if (!learnedMatchFound) {
            for ((category, keywords) in categoryKeywords) {
                // If it's income, prefer income categories
                val isIncomeCategory = category in listOf(CAT_SALARY, CAT_FREELANCE, CAT_BUSINESS, CAT_BONUS, CAT_INTEREST)
                if (isIncome && !isIncomeCategory) continue
                if (!isIncome && isIncomeCategory) continue

                var score = 0.0
                for (kw in keywords) {
                    if (lowerText.contains(kw)) {
                        score += if (lowerText.split("\\s+".toRegex()).contains(kw)) 2.0 else 1.0
                    }
                }
                if (score > highestScore) {
                    highestScore = score
                    bestCategory = category
                }
            }
        }

        // 4. Classify Payment Method
        var paymentMethod = "UPI" // Default in modern mobile users
        for ((method, keywords) in paymentMethodKeywords) {
            for (kw in keywords) {
                if (lowerText.contains(kw)) {
                    paymentMethod = method
                    break
                }
            }
        }

        // 5. Extract Merchant or Source
        var merchant = ""
        val commonMerchants = listOf(
            "swiggy", "zomato", "amazon", "flipkart", "uber", "ola", "rapido", "dmart",
            "blinkit", "zepto", "netflix", "spotify", "hp petrol", "shell", "myntra"
        )
        for (m in commonMerchants) {
            if (lowerText.contains(m)) {
                merchant = m.replaceFirstChar { it.uppercase() }
                break
            }
        }
        if (merchant.isEmpty()) {
            merchant = bestCategory
        }

        // 6. Extract Date
        val dateMillis = extractDate(lowerText)

        // 7. Calculate Confidence Score
        var confidence = 0.50f
        if (extractedAmount != null && extractedAmount > 0.0) confidence += 0.25f else uncertainFields.add("Amount")
        if (highestScore > 0) confidence += 0.20f else uncertainFields.add("Category")
        if (merchant.isNotEmpty()) confidence += 0.05f

        val finalConfidence = confidence.coerceIn(0.20f, 0.98f)

        return ParsedTransactionResult(
            type = type,
            amount = amount,
            category = bestCategory,
            paymentMethod = paymentMethod,
            merchantOrSource = merchant,
            date = dateMillis,
            description = cleanInput,
            confidenceScore = finalConfidence,
            uncertainFields = uncertainFields
        )
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase(Locale.ROOT)
            .replace("[^a-zA-Z0-9\\u0B80-\\u0BFF\\u0900-\\u097F\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
    }

    private fun extractAmount(text: String): Double? {
        // Pattern 1: e.g. 25k, 2.5k
        val kPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*k\\b", Pattern.CASE_INSENSITIVE)
        val kMatcher = kPattern.matcher(text)
        if (kMatcher.find()) {
            val num = kMatcher.group(1)?.toDoubleOrNull()
            if (num != null) return num * 1000.0
        }

        // Pattern 2: Currency symbols or words: ₹ 250, rs. 500, 250 rupees, 500 ரூபாய், २५० रुपये
        val regexPatterns = listOf(
            Pattern.compile("(?:₹|rs\\.?|rupees?|ரூபாய்|रुपये|inr)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(?:₹|rs\\.?|rupees?|ரூபாய்|रुपये|inr)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:spend|paid|cost|spent|கொடுத்தேன்|செலவு|खर्च|दिया)\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d+)?)\\s*(?:spend|paid|spent|vandhudhu|aaya)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{2,7}(?:\\.\\d{1,2})?)\\b") // Fallback general number (exclude single digit numbers like dates)
        )

        for (p in regexPatterns) {
            val matcher = p.matcher(text)
            if (matcher.find()) {
                val rawNum = matcher.group(1)?.replace(",", "")
                val parsed = rawNum?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractDate(lowerText: String): Long {
        val calendar = Calendar.getInstance()
        when {
            lowerText.contains("yesterday") || lowerText.contains("netru") || lowerText.contains("kal") ||
                    lowerText.contains("நேற்று") || lowerText.contains("कल") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            lowerText.contains("tomorrow") || lowerText.contains("naalai") || lowerText.contains("நாளை") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            // default is today / inniku / aaj / இன்று / आज
            else -> {
                // today
            }
        }
        return calendar.timeInMillis
    }
}
