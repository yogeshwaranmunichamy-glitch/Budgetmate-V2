package com.example.ml

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ReceiptLineItem(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val category: String = "Groceries"
)

data class ParsedReceiptData(
    val merchant: String,
    val amount: Double,
    val date: Long,
    val category: String,
    val items: List<String>,
    val parsedItems: List<ReceiptLineItem> = emptyList(),
    val rawText: String,
    val confidence: Float
)

object ReceiptOCRParser {

    /**
     * Extracts structured line items (name, price, quantity, category) from receipt text lines.
     */
    fun extractStructuredItems(lines: List<String>): List<ReceiptLineItem> {
        val structured = mutableListOf<ReceiptLineItem>()
        val skipKeywords = listOf(
            "total", "subtotal", "sub total", "grand total", "net amount", "tax", "gst", "cgst", "sgst",
            "igst", "vat", "round off", "cash", "change", "bal due", "balance", "discount", "invoice",
            "bill", "date", "time", "welcome", "thank", "card", "upi", "phone", "tel"
        )

        // Pattern 1: e.g. "1x Special Meals 180.00" or "2 x Masala Dosa 140.00"
        val qtyFirstPattern = Pattern.compile("^(?:\\d+[\\.\\)]\\s*)?(\\d+)\\s*[xX*@]\\s*(.+?)\\s+(?:₹|rs\\.?)?\\s*(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE)

        // Pattern 2: e.g. "Bread 1 x 40.00"
        val qtyMidPattern = Pattern.compile("^(?:\\d+[\\.\\)]\\s*)?(.+?)\\s+(\\d+)\\s*[xX*@]\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE)

        // Pattern 3: standard item + price e.g. "Atta 5kg ₹280.00" or "Filter Coffee 40.00"
        val standardItemPattern = Pattern.compile("^(?:\\d+[\\.\\)]\\s*)?(.+?)\\s+(?:₹|rs\\.?)?\\s*(\\d+(?:\\.\\d{1,2})?)$", Pattern.CASE_INSENSITIVE)

        for (line in lines) {
            val trimmed = line.trim()
            val lower = trimmed.lowercase(Locale.ROOT)
            if (skipKeywords.any { lower.contains(it) } || trimmed.length < 3) continue

            val mQty1 = qtyFirstPattern.matcher(trimmed)
            if (mQty1.find()) {
                val qty = mQty1.group(1)?.toIntOrNull() ?: 1
                val name = mQty1.group(2)?.trim() ?: ""
                val price = mQty1.group(3)?.toDoubleOrNull() ?: 0.0
                if (name.length >= 2 && price > 0) {
                    val itemCat = MultilingualNLP.parseInput(name).category
                    val finalCat = if (itemCat != MultilingualNLP.CAT_SALARY) itemCat else "Food"
                    structured.add(ReceiptLineItem(name = name, price = price, quantity = qty, category = finalCat))
                    continue
                }
            }

            val mQty2 = qtyMidPattern.matcher(trimmed)
            if (mQty2.find()) {
                val name = mQty2.group(1)?.trim() ?: ""
                val qty = mQty2.group(2)?.toIntOrNull() ?: 1
                val price = mQty2.group(3)?.toDoubleOrNull() ?: 0.0
                if (name.length >= 2 && price > 0) {
                    val itemCat = MultilingualNLP.parseInput(name).category
                    val finalCat = if (itemCat != MultilingualNLP.CAT_SALARY) itemCat else "Food"
                    structured.add(ReceiptLineItem(name = name, price = price, quantity = qty, category = finalCat))
                    continue
                }
            }

            val mStd = standardItemPattern.matcher(trimmed)
            if (mStd.find()) {
                val name = mStd.group(1)?.trim() ?: ""
                val price = mStd.group(2)?.toDoubleOrNull() ?: 0.0
                if (name.length >= 2 && price > 0 && !name.matches("^[\\d\\W]+$".toRegex())) {
                    val itemCat = MultilingualNLP.parseInput(name).category
                    val finalCat = if (itemCat != MultilingualNLP.CAT_SALARY) itemCat else "Food"
                    structured.add(ReceiptLineItem(name = name, price = price, quantity = 1, category = finalCat))
                }
            }
        }
        return structured
    }

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
            Pattern.compile("(?:total(?:\\s*(?:amount|amt|bill|value|payable))?|grand\\s*total|net\\s*amount|final\\s*amount|bal\\s*due|amount\\s*paid|net\\s*payable|bill\\s*amount)\\s*[:=-]?\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*(?:total|paid|grand\\s*total)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:subtotal|sub\\s*total)\\s*[:=-]?\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
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

        // 4. Extract structured items (multiple items breakdown)
        val structuredItems = extractStructuredItems(lines)
        for (item in structuredItems) {
            detectedItems.add("${item.name} - ₹${item.price}")
        }
        if (detectedItems.isEmpty()) {
            for (line in lines) {
                if (line.matches(".*\\d+\\.\\d{2}$".toRegex()) && !line.lowercase().contains("total")) {
                    detectedItems.add(line)
                }
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
            parsedItems = structuredItems,
            rawText = rawText,
            confidence = confidence
        )
    }

    /**
     * Process image bitmap from Camera or File Upload through the PaddleOCR pipeline.
     * Uses on-device neural DBNet recognition + optional PaddleOCR Hub/server,
     * applying PaddleOCR reading-order sorting and receipt financial entity parsing.
     */
    suspend fun processReceiptBitmap(
        bitmap: android.graphics.Bitmap,
        customServerUrl: String? = null
    ): ParsedReceiptData {
        return try {
            val ocrResult = PaddleOCREngine.recognize(bitmap, customServerUrl)
            if (ocrResult.lines.isNotEmpty() && ocrResult.fullText.isNotBlank()) {
                PaddleOCREngine.parsePaddleOCRReceipt(ocrResult)
            } else {
                // If camera photo was empty/dark, provide clean placeholder
                ParsedReceiptData(
                    merchant = "Store / Merchant",
                    amount = 0.0,
                    date = System.currentTimeMillis(),
                    category = "Groceries",
                    items = emptyList(),
                    rawText = "No clear text detected in image. Please ensure good lighting and text focus.",
                    confidence = 0.30f
                )
            }
        } catch (e: Exception) {
            // Graceful error fallback
            ParsedReceiptData(
                merchant = "Scanned Receipt",
                amount = 0.0,
                date = System.currentTimeMillis(),
                category = "Groceries",
                items = emptyList(),
                rawText = "PaddleOCR processing error: ${e.localizedMessage ?: "Unknown error"}",
                confidence = 0.30f
            )
        }
    }
}
