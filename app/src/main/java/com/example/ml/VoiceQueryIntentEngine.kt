package com.example.ml

import java.util.Locale

enum class QueryIntent {
    GET_CATEGORY_EXPENSE,
    GET_TOTAL_EXPENSE,
    GET_TOTAL_INCOME,
    GET_BALANCE,
    GET_REMAINING_BUDGET,
    GET_SAVINGS_PROGRESS,
    GET_RECENT_TRANSACTIONS,
    UNKNOWN
}

data class ParsedVoiceQuery(
    val intent: QueryIntent,
    val targetCategory: String? = null,
    val period: String = "CURRENT_MONTH", // "CURRENT_MONTH", "TODAY", "ALL_TIME"
    val originalText: String
)

object VoiceQueryIntentEngine {

    fun parseQuery(input: String): ParsedVoiceQuery {
        val lower = input.lowercase(Locale.ROOT).trim()

        // 1. Identify category if asking about category expense
        var matchedCategory: String? = null
        for (cat in listOf(
            "food", "groceries", "transport", "petrol", "shopping", "rent",
            "electricity", "water", "internet", "mobile", "medical", "entertainment",
            "education", "travel", "subscriptions", "emi"
        )) {
            if (lower.contains(cat)) {
                matchedCategory = cat.replaceFirstChar { it.uppercase() }
                break
            }
        }

        // Tamil & Hindi category aliases
        if (matchedCategory == null) {
            when {
                lower.contains("சாப்பாடு") || lower.contains("sappadu") || lower.contains("khana") || lower.contains("खाने") -> matchedCategory = "Food"
                lower.contains("பெட்ரோல்") || lower.contains("fuel") -> matchedCategory = "Petrol"
                lower.contains("மளிகை") || lower.contains("kirana") -> matchedCategory = "Groceries"
                lower.contains("வாடகை") || lower.contains("kiraya") -> matchedCategory = "Rent"
                lower.contains("துணி") || lower.contains("kapde") -> matchedCategory = "Shopping"
            }
        }

        // Determine intent based on multilingual keywords
        val intent: QueryIntent = when {
            matchedCategory != null && (
                    lower.contains("evlo") || lower.contains("how much") || lower.contains("spend") ||
                            lower.contains("செலவு") || lower.contains("कितना") || lower.contains("खर्च") ||
                            lower.contains("kuduthen") || lower.contains("cost")
                    ) -> QueryIntent.GET_CATEGORY_EXPENSE

            lower.contains("balance") || lower.contains("மீதி") || lower.contains("இருப்பு") ||
                    lower.contains("balance evlo") || lower.contains("how much balance") || lower.contains("बैलेंस") ||
                    lower.contains("paise bache") -> QueryIntent.GET_BALANCE

            lower.contains("saving") || lower.contains("goal") || lower.contains("சேமிப்பு") ||
                    lower.contains("சேமிப்பு இலக்கு") || lower.contains("बचत") -> QueryIntent.GET_SAVINGS_PROGRESS

            lower.contains("budget") || lower.contains("பட்ஜெட்") || lower.contains("बजट") -> QueryIntent.GET_REMAINING_BUDGET

            lower.contains("income") || lower.contains("வருமானம்") || lower.contains("varavu") ||
                    lower.contains("kamaya") || lower.contains("कमाई") || lower.contains("aamdani") -> QueryIntent.GET_TOTAL_INCOME

            lower.contains("recent") || lower.contains("last") || lower.contains("history") ||
                    lower.contains("கடைசி") || lower.contains("சமீபத்திய") || lower.contains("हालिया") -> QueryIntent.GET_RECENT_TRANSACTIONS

            lower.contains("total expense") || lower.contains("how much did i spend") ||
                    lower.contains("evlo selavu") || lower.contains("kitna kharch") ||
                    lower.contains("எவ்வளவு செலவு") || lower.contains("total spend") ||
                    lower.contains("spend panniruken") || lower.contains("kharch kiya") -> QueryIntent.GET_TOTAL_EXPENSE

            matchedCategory != null -> QueryIntent.GET_CATEGORY_EXPENSE

            else -> QueryIntent.UNKNOWN
        }

        return ParsedVoiceQuery(
            intent = intent,
            targetCategory = matchedCategory,
            period = if (lower.contains("today") || lower.contains("inniku") || lower.contains("aaj")) "TODAY" else "CURRENT_MONTH",
            originalText = input
        )
    }
}
