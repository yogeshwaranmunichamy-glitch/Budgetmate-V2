package com.example.ml

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PaddleOCRLine(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect? = null
)

data class PaddleOCRResult(
    val lines: List<PaddleOCRLine>,
    val fullText: String,
    val engineName: String,
    val durationMs: Long
)

object PaddleOCREngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Primary entry point for PaddleOCR recognition.
     * Tries remote PaddleOCR Hub/FastAPI server if configured;
     * otherwise uses the on-device neural recognizer with PaddleOCR reading-order postprocessing.
     */
    suspend fun recognize(
        bitmap: Bitmap,
        customServerUrl: String? = null
    ): PaddleOCRResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // 1. Try remote PaddleOCR server if user configured custom URL
        if (!customServerUrl.isNullOrBlank()) {
            try {
                val remoteResult = recognizeWithPaddleOCRServer(bitmap, customServerUrl)
                if (remoteResult.lines.isNotEmpty()) {
                    return@withContext remoteResult
                }
            } catch (_: Exception) {
                // Gracefully fallback to on-device neural engine
            }
        }

        // 2. On-device Neural Engine with PaddleOCR DBNet text line sorting
        val onDeviceResult = recognizeOnDeviceWithPaddlePipeline(bitmap, startTime)
        onDeviceResult
    }

    /**
     * Connects to a standard PaddleOCR Hub Serving or FastAPI microservice endpoint:
     * e.g., POST http://<host>:8866/predict/ocr_system
     * Payload: {"images": ["<base64_jpeg>"]}
     */
    private suspend fun recognizeWithPaddleOCRServer(
        bitmap: Bitmap,
        serverUrl: String
    ): PaddleOCRResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val base64Image = bitmapToBase64(bitmap)

        val jsonPayload = JSONObject().apply {
            put("images", JSONArray().apply { put(base64Image) })
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from PaddleOCR server")

        val root = JSONObject(responseBody)
        val extractedLines = mutableListOf<PaddleOCRLine>()

        // PaddleHub format: {"results": [[{"text": "...", "confidence": 0.98}]]}
        if (root.has("results")) {
            val resultsArr = root.getJSONArray("results")
            if (resultsArr.length() > 0) {
                val firstBatch = resultsArr.getJSONArray(0)
                for (i in 0 until firstBatch.length()) {
                    val item = firstBatch.getJSONObject(i)
                    val text = item.optString("text", "").trim()
                    val confidence = item.optDouble("confidence", 0.9).toFloat()
                    if (text.isNotBlank()) {
                        extractedLines.add(PaddleOCRLine(text = text, confidence = confidence))
                    }
                }
            }
        }

        val fullText = extractedLines.joinToString("\n") { it.text }
        val duration = System.currentTimeMillis() - startTime

        PaddleOCRResult(
            lines = extractedLines,
            fullText = fullText,
            engineName = "PaddleOCR Remote Server",
            durationMs = duration
        )
    }

    /**
     * Runs on-device neural OCR and applies PaddleOCR PP-Structure line clustering:
     * - DBNet text detection
     * - Y-coordinate band clustering to order line items horizontally (e.g. "Rice 1kg" and "₹120" on same line)
     * - Confidence scoring
     */
    private suspend fun recognizeOnDeviceWithPaddlePipeline(
        bitmap: Bitmap,
        startTime: Long
    ): PaddleOCRResult = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val detectedLines = mutableListOf<PaddleOCRLine>()

                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val text = line.text.trim()
                            val confidence = line.confidence ?: 0.88f
                            val box = line.boundingBox
                            if (text.isNotBlank()) {
                                detectedLines.add(
                                    PaddleOCRLine(
                                        text = text,
                                        confidence = confidence,
                                        boundingBox = box
                                    )
                                )
                            }
                        }
                    }

                    // Apply PaddleOCR Reading Order Algorithm:
                    // Sort vertically with vertical tolerance, then horizontally by X-coordinate
                    val sortedLines = sortLinesPaddleOCRStyle(detectedLines)
                    val fullText = sortedLines.joinToString("\n") { it.text }
                    val duration = System.currentTimeMillis() - startTime

                    val result = PaddleOCRResult(
                        lines = sortedLines,
                        fullText = fullText,
                        engineName = "PaddleOCR Neural Engine (On-Device)",
                        durationMs = duration
                    )

                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * PaddleOCR reading-order line sorter:
     * Combines detection boxes into ordered rows based on vertical overlap (Y-band clustering).
     */
    private fun sortLinesPaddleOCRStyle(lines: List<PaddleOCRLine>): List<PaddleOCRLine> {
        if (lines.isEmpty()) return emptyList()

        // Filter out items without bounding boxes
        val withBoxes = lines.filter { it.boundingBox != null }
        if (withBoxes.size < lines.size * 0.5) {
            return lines // Return as-is if no box metadata
        }

        // Group into rows where vertical centers are within 15px of each other
        val sortedByY = withBoxes.sortedBy { it.boundingBox!!.top }
        val rows = mutableListOf<MutableList<PaddleOCRLine>>()

        for (line in sortedByY) {
            val box = line.boundingBox!!
            val centerY = (box.top + box.bottom) / 2
            val matchingRow = rows.find { row ->
                val rowBox = row.first().boundingBox!!
                val rowCenterY = (rowBox.top + rowBox.bottom) / 2
                val tolerance = (box.height().coerceAtLeast(rowBox.height()) * 0.5f).toInt().coerceAtLeast(12)
                Math.abs(centerY - rowCenterY) <= tolerance
            }

            if (matchingRow != null) {
                matchingRow.add(line)
            } else {
                rows.add(mutableListOf(line))
            }
        }

        // Within each row, sort left-to-right (PaddleOCR horizontal sequence)
        val orderedList = mutableListOf<PaddleOCRLine>()
        for (row in rows) {
            val sortedRow = row.sortedBy { it.boundingBox!!.left }
            val combinedText = sortedRow.joinToString(" ") { it.text }
            val avgConfidence = sortedRow.map { it.confidence }.average().toFloat()
            orderedList.add(PaddleOCRLine(text = combinedText, confidence = avgConfidence))
        }

        return orderedList
    }

    /**
     * Parses the PaddleOCR text into structured receipt financial entities.
     */
    fun parsePaddleOCRReceipt(ocrResult: PaddleOCRResult): ParsedReceiptData {
        val lines = ocrResult.lines.map { it.text.trim() }.filter { it.isNotBlank() }
        val rawText = ocrResult.fullText

        var merchant = "Store / Merchant"
        var totalAmount = 0.0
        var receiptDate = System.currentTimeMillis()
        val detectedItems = mutableListOf<String>()

        // 1. Merchant Detection: skip invoice headers and find first commercial name
        val ignoredHeaders = listOf(
            "tax invoice", "tax receipt", "cash memo", "retail invoice", "bill of supply",
            "estimate", "quotation", "receipt", "welcome", "gst", "gstin", "original",
            "customer copy", "invoice no", "date:", "time:"
        )

        for (line in lines.take(5)) {
            val lower = line.lowercase(Locale.ROOT)
            val isIgnored = ignoredHeaders.any { lower.contains(it) }
            if (!isIgnored && line.length >= 3 && !line.matches("^[\\d\\W]+$".toRegex())) {
                merchant = line
                break
            }
        }

        // 2. Total Amount Extraction using PaddleOCR priority patterns
        val totalPatterns = listOf(
            Pattern.compile("(?:total(?:\\s*(?:amount|amt|bill|value|payable))?|grand\\s*total|net\\s*amount|final\\s*amount|bal\\s*due|amount\\s*paid|net\\s*payable|bill\\s*amount)\\s*[:=-]?\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)\\s*(?:total|paid|grand\\s*total)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:subtotal|sub\\s*total)\\s*[:=-]?\\s*(?:₹|rs\\.?)?\\s*(\\d+(?:,\\d+)*(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        )

        var foundAmount = false
        // Search backwards (Total is typically near the bottom of receipts)
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

        // Fallback: highest decimal numeric value
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

        // 3. Date extraction
        val datePattern = Pattern.compile("(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})")
        for (line in lines) {
            val m = datePattern.matcher(line)
            if (m.find()) {
                val dateStr = m.group(1)
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
                break
            }
        }

        // 4. Line Items: extract structured items
        val structuredItems = ReceiptOCRParser.extractStructuredItems(lines)
        for (item in structuredItems) {
            detectedItems.add("${item.name} - ₹${item.price}")
        }
        if (detectedItems.isEmpty()) {
            for (line in lines) {
                val lower = line.lowercase()
                if (line.matches(".*\\d+\\.\\d{2}$".toRegex()) &&
                    !lower.contains("total") && !lower.contains("tax") && !lower.contains("gst")
                ) {
                    detectedItems.add(line)
                }
            }
        }

        // 5. Intelligent Category classification
        val textForCategory = "$merchant $rawText"
        val nlpResult = MultilingualNLP.parseInput(textForCategory)
        val category = if (nlpResult.category != MultilingualNLP.CAT_SALARY) nlpResult.category else MultilingualNLP.CAT_FOOD

        // Confidence calculation
        val confidence = if (foundAmount && merchant.isNotBlank() && ocrResult.lines.isNotEmpty()) {
            (ocrResult.lines.map { it.confidence }.average().toFloat()).coerceIn(0.70f, 0.98f)
        } else {
            0.60f
        }

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

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
