package com.example.ml

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceiptData(
    val merchant: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val items: List<String>,
    val rawText: String,
    val confidence: Float
)

object ReceiptOCRParser {

    /**
     * Parses raw OCR text lines from a physical receipt using pattern matching,
     * header recognition, currency extraction, date heuristics, and category inference.
     */
    fun parseReceiptText(rawText: String): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var merchant = "Store / Merchant"
        var totalAmount = 0.0
        var receiptDate = System.currentTimeMillis()
        val detectedItems = mutableListOf<String>()

        // 1. Merchant Detection: First non-trivial line often contains store/restaurant name
        for (line in lines.take(4)) {
            val lower = line.lowercase(Locale.ROOT)
            if (!lower.contains("tax") && !lower.contains("invoice") && !lower.contains("cash") &&
                !lower.contains("bill") && !lower.contains("gst") && line.length >= 3) {
                merchant = line
                break
            }
        }

        // 2. Total Amount Extraction: look for keywords like "total", "net amount", "grand total", "subtotal", "amount", "rs", "₹"
        val totalPatterns = listOf(
            Pattern.compile("(?:total|grand\\s*total|net\\s*amount|final\\s*amount|bal\\s*due|amount\\s*paid|rs\\.?|₹)\\s*[:=-]?\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*(?:total|paid)", Pattern.CASE_INSENSITIVE)
        )

        // Reverse search lines for TOTAL (usually near the bottom)
        var foundAmount = false
        for (line in lines.reversed()) {
            for (p in totalPatterns) {
                val m = p.matcher(line)
                if (m.find()) {
                    val rawVal = m.group(1)?.replace(",", "")
                    val parsed = rawVal?.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        totalAmount = parsed
                        foundAmount = true
                        break
                    }
                }
            }
            if (foundAmount) break
        }

        // Fallback amount: largest numeric value found in document
        if (!foundAmount) {
            val numPattern = Pattern.compile("\\b(\\d{2,6}\\.\\d{2})\\b")
            var maxVal = 0.0
            for (line in lines) {
                val m = numPattern.matcher(line)
                while (m.find()) {
                    val v = m.group(1)?.toDoubleOrNull() ?: 0.0
                    if (v > maxVal) maxVal = v
                }
            }
            if (maxVal > 0) totalAmount = maxVal
        }

        // 3. Date extraction (e.g. 20/09/2026, 2026-09-20, 20-Sep-2026)
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        for (line in lines) {
            val m = datePattern.matcher(line)
            if (m.find()) {
                val dateStr = m.group(1)
                try {
                    val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yy")
                    for (fmt in formats) {
                        try {
                            val sdf = SimpleDateFormat(fmt, Locale.US)
                            val d = sdf.parse(dateStr ?: "")
                            if (d != null) {
                                receiptDate = d.time
                                break
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
                break
            }
        }

        // 4. Extract possible items
        for (line in lines) {
            if (line.matches(".*\\d+\\.\\d{2}$".toRegex()) && !line.lowercase().contains("total")) {
                detectedItems.add(line)
            }
        }

        // 5. Categorize using NLP
        val textForCategory = "$merchant $rawText"
        val nlpResult = MultilingualNLP.parseInput(textForCategory)
        val category = if (nlpResult.category != MultilingualNLP.CAT_SALARY) nlpResult.category else MultilingualNLP.CAT_FOOD

        val confidence = if (foundAmount && merchant.isNotBlank()) 0.88f else 0.55f

        return ParsedReceiptData(
            merchant = merchant,
            amount = totalAmount,
            date = receiptDate,
            category = category,
            items = detectedItems,
            rawText = rawText,
            confidence = confidence
        )
    }
}
