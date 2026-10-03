package com.example

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupRestoreUnitTest {

    @Test
    fun testBackupJsonFormatValidation() {
        val root = JSONObject()
        root.put("app", "BudgetMate")
        root.put("version", 1)
        root.put("userId", 1L)
        root.put("timestamp", 1727900000000L)

        val txArray = JSONArray()
        val tx = JSONObject().apply {
            put("type", "EXPENSE")
            put("amount", 350.0)
            put("category", "Food")
            put("paymentMethod", "UPI")
            put("sourceOrMerchant", "Saravana Bhavan")
            put("date", 1727900000000L)
            put("notes", "Lunch")
        }
        txArray.put(tx)
        root.put("transactions", txArray)

        val budgetArray = JSONArray()
        val budget = JSONObject().apply {
            put("category", "Food")
            put("monthlyLimit", 5000.0)
            put("monthYear", "2026-10")
        }
        budgetArray.put(budget)
        root.put("budgets", budgetArray)

        val jsonStr = root.toString(2)
        assertNotNull(jsonStr)

        val parsed = JSONObject(jsonStr)
        assertEquals("BudgetMate", parsed.getString("app"))
        assertEquals(1, parsed.getInt("version"))
        assertEquals(1, parsed.getJSONArray("transactions").length())
        assertEquals(1, parsed.getJSONArray("budgets").length())
        assertEquals(350.0, parsed.getJSONArray("transactions").getJSONObject(0).getDouble("amount"), 0.01)
    }

    @Test
    fun testMalformedBackupRejection() {
        val invalidJson = """{"app": "AnotherApp", "data": []}"""
        val root = JSONObject(invalidJson)
        val isBudgetMate = root.optString("app") == "BudgetMate"
        assertTrue("Foreign backup must not be accepted", !isBudgetMate)
    }
}
