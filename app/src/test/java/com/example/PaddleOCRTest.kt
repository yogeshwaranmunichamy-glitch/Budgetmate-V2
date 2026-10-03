package com.example

import com.example.ml.PaddleOCREngine
import com.example.ml.PaddleOCRLine
import com.example.ml.PaddleOCRResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaddleOCRTest {

    @Test
    fun testPaddleOCRResultStructure() {
        val line1 = PaddleOCRLine("DMART RETAIL", 0.98f)
        val line2 = PaddleOCRLine("Total Amount: 1420.00", 0.95f)
        val result = PaddleOCRResult(
            lines = listOf(line1, line2),
            fullText = "DMART RETAIL\nTotal Amount: 1420.00",
            engineName = "PaddleOCR Neural Engine (On-Device)",
            durationMs = 120
        )

        assertEquals(2, result.lines.size)
        assertEquals("PaddleOCR Neural Engine (On-Device)", result.engineName)
        assertTrue(result.durationMs > 0)
    }

    @Test
    fun testPaddleOCRReceiptParserDMart() {
        val lines = listOf(
            PaddleOCRLine("DMART RETAIL CHENNAI", 0.98f),
            PaddleOCRLine("GSTIN: 33AAACD1234F1Z5", 0.92f),
            PaddleOCRLine("Date: 25/09/2026", 0.95f),
            PaddleOCRLine("Atta 5kg ₹280.00", 0.94f),
            PaddleOCRLine("Sunflower Oil 2L ₹340.00", 0.96f),
            PaddleOCRLine("Subtotal: ₹620.00", 0.93f),
            PaddleOCRLine("Total Amount: 1420.00", 0.99f),
            PaddleOCRLine("Paid via: GPay UPI", 0.97f)
        )
        val ocrResult = PaddleOCRResult(
            lines = lines,
            fullText = lines.joinToString("\n") { it.text },
            engineName = "PaddleOCR",
            durationMs = 85
        )

        val parsed = PaddleOCREngine.parsePaddleOCRReceipt(ocrResult)

        assertEquals("DMART RETAIL CHENNAI", parsed.merchant)
        assertEquals(1420.0, parsed.amount, 0.01)
        assertTrue(parsed.confidence >= 0.85f)
        assertEquals("Groceries", parsed.category)
        assertTrue(parsed.items.isNotEmpty())
    }

    @Test
    fun testPaddleOCRReceiptParserRestaurant() {
        val lines = listOf(
            PaddleOCRLine("SARAVANA BHAVAN RESTAURANT", 0.97f),
            PaddleOCRLine("Date: 28/09/2026", 0.94f),
            PaddleOCRLine("Special Meals 180.00", 0.95f),
            PaddleOCRLine("Masala Dosa 140.00", 0.93f),
            PaddleOCRLine("Filter Coffee 40.00", 0.92f),
            PaddleOCRLine("Grand Total: 360.00", 0.98f),
            PaddleOCRLine("Payment: CASH", 0.96f)
        )
        val ocrResult = PaddleOCRResult(
            lines = lines,
            fullText = lines.joinToString("\n") { it.text },
            engineName = "PaddleOCR",
            durationMs = 95
        )

        val parsed = PaddleOCREngine.parsePaddleOCRReceipt(ocrResult)

        assertEquals("SARAVANA BHAVAN RESTAURANT", parsed.merchant)
        assertEquals(360.0, parsed.amount, 0.01)
        assertEquals("Food", parsed.category)
    }

    @Test
    fun testPaddleOCRFallbackOnEmptyInput() {
        val emptyResult = PaddleOCRResult(
            lines = emptyList(),
            fullText = "",
            engineName = "PaddleOCR",
            durationMs = 10
        )
        val parsed = PaddleOCREngine.parsePaddleOCRReceipt(emptyResult)

        assertNotNull(parsed)
        assertEquals(0.0, parsed.amount, 0.01)
    }

    @Test
    fun testExtractStructuredMultipleItemsFromBill() {
        val lines = listOf(
            PaddleOCRLine("RELIANCE FRESH RETAIL", 0.98f),
            PaddleOCRLine("Date: 01/10/2026", 0.95f),
            PaddleOCRLine("Atta 5kg ₹280.00", 0.96f),
            PaddleOCRLine("Sunflower Oil 2L ₹340.00", 0.97f),
            PaddleOCRLine("Basmati Rice 1kg 120.00", 0.95f),
            PaddleOCRLine("Total Amount: 740.00", 0.99f),
            PaddleOCRLine("Payment: UPI", 0.98f)
        )
        val ocrResult = PaddleOCRResult(
            lines = lines,
            fullText = lines.joinToString("\n") { it.text },
            engineName = "PaddleOCR",
            durationMs = 80
        )

        val parsed = PaddleOCREngine.parsePaddleOCRReceipt(ocrResult)
        assertEquals(3, parsed.parsedItems.size)

        val item1 = parsed.parsedItems[0]
        assertEquals("Atta 5kg", item1.name)
        assertEquals(280.0, item1.price, 0.01)

        val item2 = parsed.parsedItems[1]
        assertEquals("Sunflower Oil 2L", item2.name)
        assertEquals(340.0, item2.price, 0.01)

        val item3 = parsed.parsedItems[2]
        assertEquals("Basmati Rice 1kg", item3.name)
        assertEquals(120.0, item3.price, 0.01)

        val itemsSum = parsed.parsedItems.sumOf { it.price }
        assertEquals(740.0, itemsSum, 0.01)
    }

    @Test
    fun testExtractMultipleItemsWithQuantities() {
        val rawReceipt = """
            HOTEL SARAVANA
            Date: 02/10/2026
            2x Masala Dosa 140.00
            1x Filter Coffee 40.00
            Total: 180.00
        """.trimIndent()

        val parsed = com.example.ml.ReceiptOCRParser.parseReceiptText(rawReceipt)
        assertEquals(2, parsed.parsedItems.size)
        assertEquals("Masala Dosa", parsed.parsedItems[0].name)
        assertEquals(2, parsed.parsedItems[0].quantity)
        assertEquals(140.0, parsed.parsedItems[0].price, 0.01)

        assertEquals("Filter Coffee", parsed.parsedItems[1].name)
        assertEquals(1, parsed.parsedItems[1].quantity)
        assertEquals(40.0, parsed.parsedItems[1].price, 0.01)
    }
}
