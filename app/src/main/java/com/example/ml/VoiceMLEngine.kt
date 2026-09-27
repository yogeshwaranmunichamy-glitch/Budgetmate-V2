package com.example.ml

import java.util.Locale
import java.util.UUID
import java.util.regex.Pattern

/**
 * Clean, modular On-Device Voice ML & Semantic NLP Engine.
 * Supports English, Tamil, Hindi, Tanglish, Hinglish, and mixed dialect speech.
 * Follows the Voice -> STT -> Normalization -> Intent Detection -> Multi-Input Parsing ->
 * Category/Amount/Target Extraction -> Structured ADD/UPDATE/DELETE -> State -> UI architecture.
 */
object VoiceMLEngine {

    // Standard Canonical Categories
    const val CAT_GROCERY = "Grocery"
    const val CAT_ELECTRICITY = "Electricity"
    const val CAT_FUEL = "Fuel"
    const val CAT_FOOD = "Food"
    const val CAT_PARKING = "Parking"
    const val CAT_HOTEL = "Hotel"
    const val CAT_TOLL = "Toll"
    const val CAT_TRAVEL = "Travel"
    const val CAT_SHOPPING = "Shopping"
    const val CAT_RENT = "Rent"
    const val CAT_WATER = "Water"
    const val CAT_INTERNET = "Internet"
    const val CAT_MOBILE = "Mobile Recharge"
    const val CAT_MEDICAL = "Medical"
    const val CAT_EDUCATION = "Education"
    const val CAT_ENTERTAINMENT = "Entertainment"
    const val CAT_SUBSCRIPTIONS = "Subscriptions"
    const val CAT_SALARY = "Salary"
    const val CAT_OTHER = "Other"

    // Configurable Category Aliases Dictionary (Multilingual Tamil, Hindi, English, Tanglish, Hinglish)
    private val categoryAliases = mutableMapOf<String, MutableList<String>>(
        CAT_GROCERY to mutableListOf(
            "grocery", "groceries", "supermarket", "provisions", "ration", "veggies", "vegetables",
            "fruits", "milk", "curd", "dmart", "blinkit", "zepto", "instamart",
            "மளிகை", "மளிகைக்கு", "மளிகையை", "காய்கறி", "பால்", "பல்பொருள்",
            "maligai", "maligaiku", "maligai ku", "maligaiyai", "provisions",
            "किराना", "किराने", "किराने के लिए", "किराना को", "सब्जी", "दूध", "राशन",
            "kirana", "kirane", "kirane ke liye", "sabji", "doodh", "rashan"
        ),
        CAT_ELECTRICITY to mutableListOf(
            "electricity", "current", "power", "eb", "current bill", "eb bill", "power bill", "tneb", "bescom",
            "கரண்ட்", "கரண்டு", "கரண்ட்டை", "கரண்டுக்கு", "மின்சாரம்", "மின் கட்டணம்", "மின்சார கட்டணம்", "கரண்ட் பில்",
            "current bill", "current", "currentku",
            "बिजली", "बिजली बिल", "बिजली का बिल", "करंट", "पावर",
            "bijli", "bijli bill", "power bill"
        ),
        CAT_FUEL to mutableListOf(
            "fuel", "petrol", "diesel", "gas", "cng", "shell", "bunk", "hp petrol", "indian oil", "bharat petroleum",
            "பெட்ரோல்", "பெட்ரோலுக்கு", "பெட்ரோலை", "டீசல்", "எரிபொருள்", "டீசலுக்கு",
            "petrol", "petrolku", "petrol ku", "petrol-ku", "petrolக்கு", "diesel", "fuel", "gas",
            "पेट्रोल", "डीजल", "ईंधन", "तेल", "पेट्रोल के लिए", "डीजल के लिए",
            "petrol ke liye", "diesel ke liye", "indhan"
        ),
        CAT_FOOD to mutableListOf(
            "food", "lunch", "dinner", "breakfast", "snacks", "tea", "coffee", "chai", "restaurant", "cafe", "biryani",
            "swiggy", "zomato", "pizza", "burger",
            "சாப்பாடு", "உணவு", "சாப்பாட்டுக்கு", "சாப்பாட்டை", "டிபன்", "டீ", "காபி",
            "sappadu", "saapadu", "thindi", "oota", "tiffin", "chai",
            "खाना", "भोजन", "नाश्ता", "रोटी", "चाय", "कॉफ़ी", "बिरयानी",
            "khana", "nashta", "bhojan", "khana ke liye", "roti"
        ),
        CAT_PARKING to mutableListOf(
            "parking", "park", "car parking", "bike parking", "vehicle parking", "valet",
            "பார்ங்கிங்", "பார்ங்கிங்குக்கு", "பார்ங்கிங்கை", "வாகன நிறுத்தம்",
            "parking", "parking ku", "parkingku",
            "पार्किंग", "गाड़ी पार्किंग", "पार्किंग के लिए", "parking ke liye"
        ),
        CAT_HOTEL to mutableListOf(
            "hotel", "lodge", "room", "stay", "resort", "accommodation", "lodging", "hostel", "oyo",
            "hotel room", "room stay", "room rent",
            "ஹோட்டல்", "விடுதி", "தங்குமிடம்", "ரூம்", "hotel stay",
            "होटल", "कमरा", "लॉज", "होटल रूम"
        ),
        CAT_TOLL to mutableListOf(
            "toll", "tollgate", "fastag", "toll gate", "toll tax",
            "டோல்", "டோல்கேட்", "சுங்கச்சாவடி", "பாஸ்டேக்",
            "toll", "toll ku", "fastag",
            "टोल", "टोल गेट", "फास्टैग", "टोल टैक्स"
        ),
        CAT_TRAVEL to mutableListOf(
            "travel", "transport", "trip", "cab", "taxi", "uber", "ola", "rapido", "auto", "bus", "train", "metro", "flight",
            "பயணம்", "டிராவல்", "வாடகை", "ஆட்டோ", "கட்டணம்", "பஸ்", "ரயில்",
            "travel", "vadagai", "auto", "bus fare",
            "सफर", "यात्रा", "किराया", "टैक्सी", "ऑटो", "बस"
        ),
        CAT_SHOPPING to mutableListOf(
            "shopping", "amazon", "flipkart", "myntra", "meesho", "clothes", "dress", "shoes", "cloth",
            "துணி", "ஷாப்பிங்", "ஆடைகள்", "துணிக்கு",
            "thuni", "shopping", "kapde", "खरीदारी", "कपड़े"
        ),
        CAT_RENT to mutableListOf(
            "rent", "house rent", "room rent", "flat rent",
            "வாடகை", "வீட்டு வாடகை", "வாடகைக்கு",
            "vadagai", "kiraya", "किराया", "मकान किराया"
        ),
        CAT_MEDICAL to mutableListOf(
            "medical", "doctor", "hospital", "medicine", "tablets", "pharmacy", "clinic", "health",
            "மருந்து", "மருத்துவமனை", "டாக்டர்", "marundhu",
            "दवा", "इलाज", "अस्पताल", "डॉक्टर", "dawa", "dawai"
        )
    )

    fun registerCustomAlias(category: String, alias: String) {
        val clean = alias.trim().lowercase(Locale.ROOT)
        if (clean.isNotBlank()) {
            val list = categoryAliases.getOrPut(category) { mutableListOf() }
            if (!list.contains(clean)) list.add(clean)
        }
    }

    /**
     * Step 1: Language Normalization
     * Strips case endings, postpositions, and handles Tamil/Hindi/Tanglish inflections.
     */
    fun normalizeLanguage(text: String): String {
        var clean = text.trim()
            // Normalize currency representations
            .replace("₹", " ₹ ")
            .replace("rs\\.?", " rs ", ignoreCase = true)
            .replace("rupees?", " rupees ", ignoreCase = true)
            .replace("inr", " inr ", ignoreCase = true)
            .replace("(\\d+),(\\d+)".toRegex(), "$1$2") // 1,000 -> 1000

        // Normalize Tamil inflected suffixes
        // e.g. "மளிகைக்கு" -> "மளிகை ", "மளிகையை" -> "மளிகை ", "petrolக்கு" -> "petrol "
        clean = clean
            .replace("([\\u0B80-\\u0BFFa-zA-Z]+)க்கு(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("([\\u0B80-\\u0BFFa-zA-Z]+)யை(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("([\\u0B80-\\u0BFFa-zA-Z]+)ஐ(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("(?i)([a-zA-Z]+)ku(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("(?i)([a-zA-Z]+)kku(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("(?i)([a-zA-Z]+)yai(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("(?i)([a-zA-Z]+)-la(?=[\\s,;!?]|$)".toRegex(), "$1 ")
            .replace("(?i)([a-zA-Z]+)-ku(?=[\\s,;!?]|$)".toRegex(), "$1 ")

        // Normalize Hindi postpositions
        clean = clean
            .replace("के लिए(?=[\\s,;!?]|$)".toRegex(), " ")
            .replace("(?i)ke liye(?=[\\s,;!?]|$)".toRegex(), " ")
            .replace("को(?=[\\s,;!?]|$)".toRegex(), " ")
            .replace("में(?=[\\s,;!?]|$)".toRegex(), " ")

        // Collapse multiple spaces
        return clean.replace("\\s+".toRegex(), " ").trim()
    }

    /**
     * Resolves a spoken word or phrase semantically to a canonical category.
     */
    fun resolveCategory(token: String): String? {
        val clean = token.trim().trim('.', ',', ';', '!', '?', ':').lowercase(Locale.ROOT)
        if (clean.isBlank()) return null

        // 1. Direct match with canonical categories
        for (cat in categoryAliases.keys) {
            if (clean == cat.lowercase(Locale.ROOT)) return cat
        }

        // 2. Exact match with aliases
        for ((cat, list) in categoryAliases) {
            for (alias in list) {
                val aliasClean = alias.lowercase(Locale.ROOT)
                if (clean == aliasClean) {
                    return cat
                }
            }
        }

        // 3. Match normalized form (handles Tamil/Hindi inflections e.g. மளிகைக்கு -> மளிகை)
        val norm = normalizeLanguage(clean).trim()
        if (norm != clean && norm.isNotBlank()) {
            for ((cat, list) in categoryAliases) {
                for (alias in list) {
                    val aliasClean = alias.lowercase(Locale.ROOT)
                    if (norm == aliasClean) {
                        return cat
                    }
                }
            }
        }

        return null
    }

    /**
     * Resolves a clause semantically to a category by checking multi-word phrases and word tokens.
     */
    fun resolveCategoryInClause(clause: String): String? {
        val cleanClause = clause.lowercase(Locale.ROOT).trim()
        val words = cleanClause.split("\\s+".toRegex()).map { it.trim('.', ',', ';', '!', '?', ':') }

        // Match longest multi-word phrases first (e.g. "current bill", "hotel room", "petrol ke liye")
        val allEntries = categoryAliases.flatMap { (cat, list) ->
            list.map { alias -> Pair(cat, alias.lowercase(Locale.ROOT)) }
        }.sortedByDescending { it.second.length }

        for ((cat, aliasClean) in allEntries) {
            if (aliasClean.contains(" ")) {
                if (cleanClause.contains(aliasClean)) return cat
            } else {
                if (words.contains(aliasClean)) return cat
            }
        }

        // Also check if any word resolves after language normalization
        for (w in words) {
            val res = resolveCategory(w)
            if (res != null) return res
        }

        return null
    }

    /**
     * Checks if two category names or tokens refer to the same category semantically.
     */
    fun isSameCategory(catA: String, catB: String): Boolean {
        if (catA.equals(catB, ignoreCase = true)) return true
        val resolvedA = resolveCategory(catA) ?: catA
        val resolvedB = resolveCategory(catB) ?: catB
        if (resolvedA.equals(resolvedB, ignoreCase = true)) return true

        // Treat Fuel and Petrol as equivalent
        if ((resolvedA == CAT_FUEL || resolvedA == "Petrol") && (resolvedB == CAT_FUEL || resolvedB == "Petrol")) return true
        // Treat Grocery and Groceries as equivalent
        if ((resolvedA == CAT_GROCERY || resolvedA == "Groceries") && (resolvedB == CAT_GROCERY || resolvedB == "Groceries")) return true
        // Treat Travel and Transport as equivalent
        if ((resolvedA == CAT_TRAVEL || resolvedA == "Transport") && (resolvedB == CAT_TRAVEL || resolvedB == "Transport")) return true

        return false
    }

    /**
     * Extracts numerical amount from clause.
     */
    fun extractAmount(text: String): Double? {
        // Pattern 1: e.g. 25k, 2.5k
        val kPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*k\\b", Pattern.CASE_INSENSITIVE)
        val km = kPattern.matcher(text)
        if (km.find()) {
            val num = km.group(1)?.toDoubleOrNull()
            if (num != null) return num * 1000.0
        }

        // Pattern 2: Regex for digits with currency symbols or prefix/suffix words
        val patterns = listOf(
            Pattern.compile("(?:₹|rs\\.?|rupees?|inr|bucks?|\\$)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:/-|/|₹|rs\\.?|rupees?|inr|bucks?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:spend|spent|paid|cost|for|around|approx|about|be|actually|bill|மாற்று|மாத்து|ஆக்கு|करो|बदलो)\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:ஆ\\s*(?:மாற்று|மாத்து|ஆக்கு)|ஆக்கு|करो|कर\\s*दो)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b(\\d{2,7}(?:\\.\\d{1,2})?)\\b")
        )

        for (p in patterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val amt = m.group(1)?.toDoubleOrNull()
                if (amt != null && amt > 0.0) return amt
            }
        }

        // Word numbers fallback
        val wordMap = mapOf(
            "hundred" to 100.0, "five hundred" to 500.0, "thousand" to 1000.0, "two thousand" to 2000.0,
            "nooru" to 100.0, "ainooru" to 500.0, "aayiram" to 1000.0, "rendayiram" to 2000.0,
            "sau" to 100.0, "paanch sau" to 500.0, "hazaar" to 1000.0, "do hazaar" to 2000.0,
            "நூறு" to 100.0, "ஐந்நூறு" to 500.0, "ஆயிரம்" to 1000.0,
            "सौ" to 100.0, "पांच सौ" to 500.0, "हजार" to 1000.0
        )
        val lower = text.lowercase(Locale.ROOT)
        for ((w, v) in wordMap) {
            if (lower.contains(w)) return v
        }

        return null
    }

    /**
     * Splits multi-input sentence into individual transaction clauses.
     * e.g. "மளிகைக்கு 500, கரண்ட் 2000, petrol 1000"
     */
    fun splitIntoClauses(input: String): List<String> {
        val normalized = normalizeLanguage(input)
        if (normalized.isBlank()) return emptyList()

        // Split on punctuation (, ; \n) and multilingual conjunctions:
        // and, and also, plus, also, then, aur, tatha, matrum, apram, appuram, kooda, மற்றும், மேலும், கூட, और, तथा
        val delimiterPattern = "(?i)[,;\\n]+|\\b(?:and\\s+also|and|plus|also|then|aur|tatha|matrum|apram|appuram|kooda)\\b|(?:மற்றும்|மேலும்|கூட|அப்புறம்|அப்றம்|और|तथा)"
        val initialSegments = normalized.split(delimiterPattern.toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val finalSegments = mutableListOf<String>()

        for (seg in initialSegments) {
            // Check if multiple amounts exist in a single segment without conjunctions
            val amountRegex = Pattern.compile("\\b(\\d{2,7}(?:\\.\\d{1,2})?)\\b")
            val m = amountRegex.matcher(seg)
            val spans = mutableListOf<Pair<Int, Int>>()
            while (m.find()) {
                spans.add(Pair(m.start(), m.end()))
            }

            if (spans.size <= 1) {
                finalSegments.add(seg)
            } else {
                // Split contiguous segments like "மளிகைக்கு 500 கரண்ட் 2000 petrol 1000"
                var prev = 0
                for (i in 0 until spans.size - 1) {
                    val splitPoint = spans[i].second + (spans[i + 1].first - spans[i].second) / 2
                    val part = seg.substring(prev, splitPoint).trim()
                    if (part.isNotBlank()) finalSegments.add(part)
                    prev = splitPoint
                }
                val lastPart = seg.substring(prev).trim()
                if (lastPart.isNotBlank()) finalSegments.add(lastPart)
            }
        }

        return if (finalSegments.isNotEmpty()) finalSegments else listOf(normalized)
    }

    /**
     * Parses a single voice input containing multiple categories and amounts.
     */
    fun parseMultiInput(input: String): List<ParsedExpenseItem> {
        val clauses = splitIntoClauses(input)
        val items = mutableListOf<ParsedExpenseItem>()

        for (clause in clauses) {
            val amt = extractAmount(clause)
            if (amt != null && amt > 0.0) {
                // Determine category using semantic phrase matching and token resolution
                var matchedCategory = resolveCategoryInClause(clause)

                if (matchedCategory == null) {
                    val tokens = clause.split("\\s+".toRegex()).map { it.trim('.', ',', ';', '!', '?', ':') }
                    for (token in tokens) {
                        val resolved = resolveCategory(token)
                        if (resolved != null) {
                            matchedCategory = resolved
                            break
                        }
                    }
                }

                val finalCategory = matchedCategory ?: CAT_OTHER
                val confidence = if (finalCategory != CAT_OTHER) 0.96f else 0.65f

                items.add(
                    ParsedExpenseItem(
                        id = UUID.randomUUID().toString(),
                        category = finalCategory,
                        amount = amt,
                        rawText = clause,
                        paymentMethod = "UPI",
                        merchant = finalCategory,
                        confidenceScore = confidence
                    )
                )
            }
        }

        return items
    }

    /**
     * Structured Intent Detection & Execution.
     * Handles:
     * - CONFIRM (Finish & Save)
     * - CLEAR_ALL (Explicit list reset)
     * - UPDATE (Modifies ONLY target item, strictly preserves all other items)
     * - DELETE (Removes ONLY target item, strictly preserves all other items)
     * - ADD (Appends new items, strictly preserves all other items)
     * - AMBIGUOUS (Asks for clarification without modifying state)
     */
    fun processVoiceCommand(
        spokenText: String,
        currentItems: List<ParsedExpenseItem>
    ): VoiceExecutionResult {
        val clean = spokenText.trim()
        if (clean.isBlank()) {
            return VoiceExecutionResult(
                updatedItems = currentItems,
                feedbackMessage = "No speech detected. Please try again.",
                speechPrompt = "I didn't catch that. Please speak clearly.",
                isConfirmed = false,
                isClearAll = false
            )
        }

        val lower = clean.lowercase(Locale.ROOT)
        val normalized = normalizeLanguage(clean).lowercase(Locale.ROOT)

        // 1. Check EXPLICIT CLEAR_ALL intent
        val clearAllKeywords = listOf(
            "clear all", "delete everything", "start over", "reset all", "clear list", "remove all",
            "எல்லாத்தையும் நீக்கு", "அனைத்தையும் நீக்கு", "முதல்ல இருந்து", "முழுவதும் நீக்கு",
            "सब हटा दो", "सब डिलीट करो", "शुरू से", "पूरा हटाओ"
        )
        if (clearAllKeywords.any { lower.contains(it) }) {
            return VoiceExecutionResult(
                updatedItems = emptyList(),
                feedbackMessage = "Cleared all expenses as requested.",
                speechPrompt = "All expenses have been cleared. What would you like to record?",
                isConfirmed = false,
                isClearAll = true
            )
        }

        // 2. Check CONFIRM intent
        val confirmKeywords = listOf(
            "ok", "okay", "yes", "correct", "confirm", "confirmed", "that's right", "thats right",
            "right", "sure", "save", "done", "perfect", "good", "all good", "sari", "haan",
            "theek hai", "sahi hai", "சரி", "ஆம்", "ஆமா", "கரெக்ட்", "சேமி", "हाँ", "ठीक है", "सही है"
        )
        val correctionMarkers = listOf(
            "no", "not", "change", "remove", "add", "delete", "instead", "should be", "actually",
            "மாற்று", "மாத்து", "நீக்கு", "வேண்டாம்", "வேணாம்", "करो", "हटाओ", "जोड़ो"
        )
        val isExplicitConfirm = confirmKeywords.any { kw ->
            lower == kw || lower.startsWith("$kw ") || lower.endsWith(" $kw") || lower.contains(" $kw ")
        }
        val hasCorrection = correctionMarkers.any { m -> lower.contains(m) }

        if (isExplicitConfirm && !hasCorrection) {
            return VoiceExecutionResult(
                updatedItems = currentItems,
                feedbackMessage = "Confirmed all ${currentItems.size} expenses.",
                speechPrompt = "Confirmed.",
                isConfirmed = true,
                isClearAll = false
            )
        }

        // 3. Check for multiple corrections joined by commas or conjunctions
        // e.g. "மளிகையை 700 ஆ மாற்று, கரண்ட் 1800 ஆ மாற்று"
        val clauses = splitIntoClauses(clean)
        if (clauses.size > 1) {
            var workingList = currentItems.toMutableList()
            val feedbackList = mutableListOf<String>()

            for (clause in clauses) {
                val subResult = processSingleCommand(clause, workingList)
                workingList = subResult.updatedItems.toMutableList()
                feedbackList.add(subResult.feedbackMessage)
            }

            val readback = formatReadback(workingList, isUpdate = true)
            return VoiceExecutionResult(
                updatedItems = workingList,
                feedbackMessage = feedbackList.joinToString("; "),
                speechPrompt = readback,
                isConfirmed = false,
                isClearAll = false
            )
        }

        // Process single clause command
        return processSingleCommand(clean, currentItems)
    }

    private fun processSingleCommand(
        clean: String,
        currentItems: List<ParsedExpenseItem>
    ): VoiceExecutionResult {
        val lower = clean.lowercase(Locale.ROOT)
        val normalized = normalizeLanguage(clean)

        // A. DELETE INTENT
        // e.g. "remove parking", "delete food", "டிராவல் வேண்டாம்", "பார்ங்கிங் நீக்கு", "पार्किंग हटा दो"
        val deleteKeywords = listOf(
            "remove", "delete", "drop", "cancel", "omit", "exclude", "erase", "no ",
            "வேண்டாம்", "வேணாம்", "நீக்கு", "அழி", "எடுத்துரு",
            "हटाओ", "हटा दो", "डिलीट", "मत रखो", "निकालो"
        )
        val isDeleteIntent = deleteKeywords.any { kw -> lower.contains(kw) }

        if (isDeleteIntent) {
            // Find target item among current items using category resolution
            val targetCategory = resolveCategoryInClause(clean)
            val target = currentItems.firstOrNull { item ->
                (targetCategory != null && isSameCategory(item.category, targetCategory)) ||
                lower.contains(item.category.lowercase(Locale.ROOT))
            }

            if (target != null) {
                // DELETE: remove only target, PRESERVE everything else!
                val updated = currentItems.filter { it.id != target.id }
                val readback = formatReadback(updated, isUpdate = true)
                return VoiceExecutionResult(
                    updatedItems = updated,
                    feedbackMessage = "Removed ${target.category}.",
                    speechPrompt = readback,
                    isConfirmed = false,
                    isClearAll = false
                )
            } else {
                return VoiceExecutionResult(
                    updatedItems = currentItems,
                    feedbackMessage = "Could not find expense to delete.",
                    speechPrompt = "Which expense would you like to remove? You have: ${currentItems.joinToString { it.category }}.",
                    isConfirmed = false,
                    isClearAll = false
                )
            }
        }

        // B. ADD INTENT
        // e.g. "add 200 for toll", "include petrol 500", "டோல் 200 சேர்த்துக்கோ", "टोल 200 जोड़ो"
        val addKeywords = listOf(
            "add", "include", "plus", "also", "also add",
            "சேர்த்துக்கோ", "சேர்", "போடு", "கூட்டு", "கூட",
            "जोड़ो", "डालो", "और"
        )
        val isAddIntent = addKeywords.any { kw ->
            lower.startsWith(kw) || lower.contains(" $kw ") || lower.endsWith(kw)
        }

        if (isAddIntent) {
            val parsedNew = parseMultiInput(clean)
            if (parsedNew.isNotEmpty()) {
                // ADD: existing items + new items!
                val updated = currentItems + parsedNew
                val readback = formatReadback(updated, isUpdate = true)
                val addedDesc = parsedNew.joinToString(", ") { "${it.category} ₹${it.amount.toInt()}" }
                return VoiceExecutionResult(
                    updatedItems = updated,
                    feedbackMessage = "Added $addedDesc.",
                    speechPrompt = readback,
                    isConfirmed = false,
                    isClearAll = false
                )
            }
        }

        // C. UPDATE INTENT
        // e.g. "மளிகையை 700 ஆ மாற்று", "grocery should be 700", "petrol 600 karo", "food is actually 350", "petrol should be 600"
        val updateKeywords = listOf(
            "மாற்று", "மாத்து", "ஆக்கு", "ஆ மாற்று", "ஆ மாத்து", "மாத்தவும்",
            "करो", "कर दो", "बदलो", "होना चाहिए",
            "change", "should be", "actually", "make", "instead", "to "
        )
        val hasUpdateKeyword = updateKeywords.any { kw -> lower.contains(kw) }

        // Find target item in current list using semantic clause matching
        val targetCategory = resolveCategoryInClause(clean)
        val targetItem = currentItems.firstOrNull { item ->
            (targetCategory != null && isSameCategory(item.category, targetCategory)) ||
            clean.contains(item.category, ignoreCase = true)
        }

        val extractedAmt = extractAmount(clean)

        if (targetItem != null && extractedAmt != null) {
            // UPDATE: modify only target and preserve everything else!
            val updated = currentItems.map {
                if (it.id == targetItem.id) it.copy(amount = extractedAmt) else it
            }
            val readback = formatReadback(updated, isUpdate = true)
            return VoiceExecutionResult(
                updatedItems = updated,
                feedbackMessage = "Updated ${targetItem.category} to ₹${extractedAmt.toInt()}.",
                speechPrompt = readback,
                isConfirmed = false,
                isClearAll = false
            )
        }

        // Target found but category change (e.g. "change petrol to diesel")
        if (targetItem != null && hasUpdateKeyword) {
            val newCategory = resolveCategory(clean)
            if (newCategory != null && newCategory != targetItem.category) {
                val updated = currentItems.map {
                    if (it.id == targetItem.id) it.copy(category = newCategory) else it
                }
                val readback = formatReadback(updated, isUpdate = true)
                return VoiceExecutionResult(
                    updatedItems = updated,
                    feedbackMessage = "Changed ${targetItem.category} to $newCategory.",
                    speechPrompt = readback,
                    isConfirmed = false,
                    isClearAll = false
                )
            }
        }

        // Ambiguous target check
        if (extractedAmt != null && hasUpdateKeyword && targetItem == null) {
            val categoriesList = currentItems.joinToString(", ") { it.category }
            return VoiceExecutionResult(
                updatedItems = currentItems,
                feedbackMessage = "Which expense should be changed to ₹${extractedAmt.toInt()}?",
                speechPrompt = "Which expense should be changed to ₹${extractedAmt.toInt()}? ($categoriesList)",
                isConfirmed = false,
                isClearAll = false
            )
        }

        // D. PARSE AS NEW MULTI-INPUT (If no existing items or initial input)
        val freshParsed = parseMultiInput(clean)
        if (freshParsed.isNotEmpty() && freshParsed.all { it.amount > 0 }) {
            // If current list is empty, initialize. If current list already had items, check if user is adding or correcting
            if (currentItems.isEmpty()) {
                val readback = formatReadback(freshParsed, isUpdate = false)
                return VoiceExecutionResult(
                    updatedItems = freshParsed,
                    feedbackMessage = "Interpreted ${freshParsed.size} expenses.",
                    speechPrompt = readback,
                    isConfirmed = false,
                    isClearAll = false
                )
            } else {
                // If items already exist, by default PRESERVE and update or add!
                var workingList = currentItems.toMutableList()
                for (newItem in freshParsed) {
                    val existingIdx = workingList.indexOfFirst { isSameCategory(it.category, newItem.category) }
                    if (existingIdx != -1) {
                        workingList[existingIdx] = workingList[existingIdx].copy(amount = newItem.amount)
                    } else {
                        workingList.add(newItem)
                    }
                }
                val readback = formatReadback(workingList, isUpdate = true)
                return VoiceExecutionResult(
                    updatedItems = workingList,
                    feedbackMessage = "Updated expenses list.",
                    speechPrompt = readback,
                    isConfirmed = false,
                    isClearAll = false
                )
            }
        }

        // E. AMBIGUOUS / UNCLEAR
        return VoiceExecutionResult(
            updatedItems = currentItems,
            feedbackMessage = "Could not understand command.",
            speechPrompt = "I didn't understand. Say 'Yes' to confirm, or 'மளிகையை 700 ஆ மாற்று', or 'Remove parking'.",
            isConfirmed = false,
            isClearAll = false
        )
    }

    fun formatReadback(items: List<ParsedExpenseItem>, isUpdate: Boolean): String {
        if (items.isEmpty()) {
            return "All items have been cleared. You can speak new expenses."
        }
        val summary = items.joinToString(", ") { "${it.category} ₹${it.amount.toInt()}" }
        return if (isUpdate) {
            "Updated list: $summary. Is this correct?"
        } else {
            "I understood: $summary. Is this correct?"
        }
    }
}

data class VoiceExecutionResult(
    val updatedItems: List<ParsedExpenseItem>,
    val feedbackMessage: String,
    val speechPrompt: String,
    val isConfirmed: Boolean,
    val isClearAll: Boolean
)
