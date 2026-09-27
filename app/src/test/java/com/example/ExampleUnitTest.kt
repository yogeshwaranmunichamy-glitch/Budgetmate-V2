package com.example

import com.example.ml.MultilingualNLP
import com.example.ml.ReceiptOCRParser
import com.example.ml.TripAnalyticsEngine
import com.example.ml.TripVoiceQueryType
import com.example.ml.VoiceCorrectionResult
import com.example.ml.VoiceMLEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTripVoiceExpenseParsing_TamilAndEnglish() {
        val input = "Inniku dinner-ku 600 spend pannen paid via gpay"
        val parsed = MultilingualNLP.parseInput(input)
        assertEquals("EXPENSE", parsed.type)
        assertEquals(600.0, parsed.amount, 0.01)
        assertEquals("Food", parsed.category)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun testTripVoiceExpenseParsing_Dinner() {
        val input = "Dinner at beach restaurant ₹1,250 by card"
        val parsed = MultilingualNLP.parseInput(input)
        assertEquals("EXPENSE", parsed.type)
        assertEquals(1250.0, parsed.amount, 0.01)
        assertEquals("Food", parsed.category)
        assertEquals("Card", parsed.paymentMethod)
    }

    @Test
    fun testTripAnalyticsAndSplitwiseSettlements() {
        val trip = com.example.data.local.entities.TripEntity(
            id = 10,
            userId = 1,
            name = "Goa Trip",
            destination = "Goa",
            startDate = System.currentTimeMillis() - 86400000L * 2,
            endDate = System.currentTimeMillis() + 86400000L * 2,
            budget = 5000.0
        )

        val expenses = listOf(
            com.example.data.local.entities.TripExpenseEntity(
                tripId = 10,
                userId = 1,
                title = "Cafe",
                amount = 1200.0,
                category = "Food & Dining",
                date = System.currentTimeMillis()
            ),
            com.example.data.local.entities.TripExpenseEntity(
                tripId = 10,
                userId = 1,
                title = "Cab",
                amount = 800.0,
                category = "Local Commute",
                date = System.currentTimeMillis()
            )
        )

        val analytics = TripAnalyticsEngine.analyzeTrip(trip, expenses)

        assertEquals(2000.0, analytics.totalSpent, 0.01)
        assertEquals(3000.0, analytics.remainingBudget, 0.01)
        assertEquals(40.0f, analytics.percentSpent, 0.1f)
        assertTrue(analytics.categorySpends.isNotEmpty())
    }

    @Test
    fun testTripVoiceQuery() {
        val q1 = TripAnalyticsEngine.parseTripVoiceQuery("How much spent on food?")
        assertEquals(TripVoiceQueryType.CATEGORY_SPEND, q1.queryType)
        assertEquals("Food & Dining", q1.targetCategory)

        val q2 = TripAnalyticsEngine.parseTripVoiceQuery("Remaining trip budget?")
        assertEquals(TripVoiceQueryType.REMAINING_BUDGET, q2.queryType)

        val q3 = TripAnalyticsEngine.parseTripVoiceQuery("Who owes whom in Goa trip?")
        assertEquals(TripVoiceQueryType.WHO_OWES_WHOM, q3.queryType)
    }

    @Test
    fun testMultilingualNLP_AmountExtractionAndNumberWords() {
        val r1 = MultilingualNLP.parseInput("Swiggy food order 450 rupees")
        assertEquals(450.0, r1.amount, 0.01)
        assertEquals("Food", r1.category)

        val r2 = MultilingualNLP.parseInput("Paid fifty rupees for tea")
        assertEquals(50.0, r2.amount, 0.01)
        assertEquals("Food", r2.category)
    }

    @Test
    fun testMultilingualNLP_CustomKeywordTraining() {
        MultilingualNLP.trainCustomKeyword("fitnesstribe", "Medical")
        val result = MultilingualNLP.parseInput("Paid 1500 for fitnesstribe membership")
        assertEquals("Medical", result.category)
        assertEquals(1500.0, result.amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_FourExpenses() {
        val input = "I spent ₹500 for petrol, ₹300 for food, ₹200 for parking and ₹1,000 for hotel."
        val items = MultilingualNLP.parseMultiInput(input)

        assertEquals(4, items.size)
        assertTrue(VoiceMLEngine.isSameCategory("Fuel", items[0].category))
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(300.0, items[1].amount, 0.01)

        assertEquals("Parking", items[2].category)
        assertEquals(200.0, items[2].amount, 0.01)

        assertEquals("Hotel", items[3].category)
        assertEquals(1000.0, items[3].amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_TwoExpenses() {
        val input = "I spent 500 on petrol and 300 for food."
        val items = MultilingualNLP.parseMultiInput(input)

        assertEquals(2, items.size)
        assertTrue(VoiceMLEngine.isSameCategory("Fuel", items[0].category))
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(300.0, items[1].amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_CompactSpeech() {
        val input = "Petrol 500, food 300, parking 200."
        val items = MultilingualNLP.parseMultiInput(input)

        assertEquals(3, items.size)
        assertTrue(VoiceMLEngine.isSameCategory("Fuel", items[0].category))
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(300.0, items[1].amount, 0.01)

        assertEquals("Parking", items[2].category)
        assertEquals(200.0, items[2].amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_NaturalSentence() {
        val input = "Today I spent around 500 for fuel, 250 for lunch and 100 for parking."
        val items = MultilingualNLP.parseMultiInput(input)

        assertEquals(3, items.size)
        assertTrue(VoiceMLEngine.isSameCategory("Fuel", items[0].category))
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(250.0, items[1].amount, 0.01)

        assertEquals("Parking", items[2].category)
        assertEquals(100.0, items[2].amount, 0.01)
    }

    @Test
    fun testVoiceConfirmationAndCorrection_Flow() {
        // Step 1: Initial list: Petrol 500, Food 300, Parking 200
        val initialItems = MultilingualNLP.parseMultiInput("Petrol 500, food 300, parking 200.")
        assertEquals(3, initialItems.size)

        // Step 2: Readback formatting
        val prompt1 = MultilingualNLP.formatConfirmationPrompt(initialItems, isUpdate = false)
        assertTrue(prompt1.contains("500"))
        assertTrue(prompt1.contains("Food ₹300"))
        assertTrue(prompt1.contains("Parking ₹200"))
        assertTrue(prompt1.contains("Is this correct?"))

        // Step 3: Voice Correction: "Petrol should be 600"
        val cor1 = MultilingualNLP.processVoiceResponse("Petrol should be 600", initialItems)
        assertTrue(cor1 is VoiceCorrectionResult.Updated)
        val updatedItems1 = (cor1 as VoiceCorrectionResult.Updated).items
        assertEquals(600.0, updatedItems1.first { VoiceMLEngine.isSameCategory("Fuel", it.category) }.amount, 0.01)
        assertTrue(cor1.speechPrompt.contains("Updated list:"))
        assertTrue(cor1.speechPrompt.contains("600"))

        // Step 4: Voice Correction: "Food is actually 350"
        val cor2 = MultilingualNLP.processVoiceResponse("Food is actually 350", updatedItems1)
        assertTrue(cor2 is VoiceCorrectionResult.Updated)
        val updatedItems2 = (cor2 as VoiceCorrectionResult.Updated).items
        assertEquals(350.0, updatedItems2.first { it.category == "Food" }.amount, 0.01)

        // Step 5: Voice Correction: "Remove parking"
        val cor3 = MultilingualNLP.processVoiceResponse("Remove parking", updatedItems2)
        assertTrue(cor3 is VoiceCorrectionResult.Removed)
        val updatedItems3 = (cor3 as VoiceCorrectionResult.Removed).items
        assertEquals(2, updatedItems3.size)
        assertFalse(updatedItems3.any { it.category == "Parking" })

        // Step 6: Voice Correction: "Add 200 for toll"
        val cor4 = MultilingualNLP.processVoiceResponse("Add 200 for toll", updatedItems3)
        assertTrue(cor4 is VoiceCorrectionResult.Added)
        val updatedItems4 = (cor4 as VoiceCorrectionResult.Added).items
        assertEquals(3, updatedItems4.size)
        assertTrue(updatedItems4.any { it.category == "Toll" && it.amount == 200.0 })

        // Step 7: Voice Confirmation: "Okay"
        val confirmResult = MultilingualNLP.processVoiceResponse("Okay", updatedItems4)
        assertTrue(confirmResult is VoiceCorrectionResult.Confirmed)
        assertEquals(3, (confirmResult as VoiceCorrectionResult.Confirmed).items.size)
    }

    // Comprehensive tests for Prompt 2: Multilingual Voice ML (Tamil, Hindi, English, Tanglish, Hinglish)
    @Test
    fun testVoiceML_MultilingualSemanticParsing() {
        // "மளிகைக்கு 500" = Grocery ₹500
        val r1 = VoiceMLEngine.parseMultiInput("மளிகைக்கு 500")
        assertEquals(1, r1.size)
        assertEquals("Grocery", r1[0].category)
        assertEquals(500.0, r1[0].amount, 0.01)

        // "maligai ku 500" = Grocery ₹500
        val r2 = VoiceMLEngine.parseMultiInput("maligai ku 500")
        assertEquals(1, r2.size)
        assertEquals("Grocery", r2[0].category)
        assertEquals(500.0, r2[0].amount, 0.01)

        // "किराने के लिए 500" = Grocery ₹500
        val r3 = VoiceMLEngine.parseMultiInput("किराने के लिए 500")
        assertEquals(1, r3.size)
        assertEquals("Grocery", r3[0].category)
        assertEquals(500.0, r3[0].amount, 0.01)

        // "grocery ke liye 500" = Grocery ₹500
        val r4 = VoiceMLEngine.parseMultiInput("grocery ke liye 500")
        assertEquals(1, r4.size)
        assertEquals("Grocery", r4[0].category)
        assertEquals(500.0, r4[0].amount, 0.01)

        // "கரண்ட் 2000" = Electricity ₹2000
        val r5 = VoiceMLEngine.parseMultiInput("கரண்ட் 2000")
        assertEquals(1, r5.size)
        assertEquals("Electricity", r5[0].category)
        assertEquals(2000.0, r5[0].amount, 0.01)

        // "current bill 2000" = Electricity ₹2000
        val r6 = VoiceMLEngine.parseMultiInput("current bill 2000")
        assertEquals(1, r6.size)
        assertEquals("Electricity", r6[0].category)
        assertEquals(2000.0, r6[0].amount, 0.01)

        // "बिजली बिल 2000" = Electricity ₹2000
        val r7 = VoiceMLEngine.parseMultiInput("बिजली बिल 2000")
        assertEquals(1, r7.size)
        assertEquals("Electricity", r7[0].category)
        assertEquals(2000.0, r7[0].amount, 0.01)

        // "petrolக்கு 1000" = Fuel ₹1000
        val r8 = VoiceMLEngine.parseMultiInput("petrolக்கு 1000")
        assertEquals(1, r8.size)
        assertEquals("Fuel", r8[0].category)
        assertEquals(1000.0, r8[0].amount, 0.01)

        // "petrol ke liye 1000" = Fuel ₹1000
        val r9 = VoiceMLEngine.parseMultiInput("petrol ke liye 1000")
        assertEquals(1, r9.size)
        assertEquals("Fuel", r9[0].category)
        assertEquals(1000.0, r9[0].amount, 0.01)
    }

    @Test
    fun testVoiceML_MultipleInputsSplitting() {
        // "மளிகைக்கு 500, கரண்ட் 2000, petrol 1000" = Grocery ₹500 + Electricity ₹2000 + Fuel ₹1000
        val multiInput = "மளிகைக்கு 500, கரண்ட் 2000, petrol 1000"
        val items = VoiceMLEngine.parseMultiInput(multiInput)

        assertEquals(3, items.size)
        assertEquals("Grocery", items[0].category)
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Electricity", items[1].category)
        assertEquals(2000.0, items[1].amount, 0.01)

        assertEquals("Fuel", items[2].category)
        assertEquals(1000.0, items[2].amount, 0.01)
    }

    @Test
    fun testVoiceML_ContextualUpdatePreservesOtherItems() {
        // Existing: Grocery ₹500, Electricity ₹2000, Travel ₹1000
        val initialItems = listOf(
            com.example.ml.ParsedExpenseItem(category = "Grocery", amount = 500.0),
            com.example.ml.ParsedExpenseItem(category = "Electricity", amount = 2000.0),
            com.example.ml.ParsedExpenseItem(category = "Travel", amount = 1000.0)
        )

        // User speaks: "மளிகையை 700 ஆ மாற்று" (Change grocery to 700)
        val result = VoiceMLEngine.processVoiceCommand("மளிகையை 700 ஆ மாற்று", initialItems)
        val updated = result.updatedItems

        assertEquals(3, updated.size)
        assertEquals(700.0, updated.first { it.category == "Grocery" }.amount, 0.01)
        assertEquals(2000.0, updated.first { it.category == "Electricity" }.amount, 0.01)
        assertEquals(1000.0, updated.first { it.category == "Travel" }.amount, 0.01)
        assertFalse(result.isConfirmed)
        assertFalse(result.isClearAll)
    }

    @Test
    fun testVoiceML_ClearAllExplicitCommand() {
        val initialItems = listOf(
            com.example.ml.ParsedExpenseItem(category = "Grocery", amount = 500.0),
            com.example.ml.ParsedExpenseItem(category = "Electricity", amount = 2000.0)
        )

        val result = VoiceMLEngine.processVoiceCommand("clear all", initialItems)
        assertTrue(result.isClearAll)
        assertEquals(0, result.updatedItems.size)
    }

    @Test
    fun testOCR_SafeReceiptParsing() {
        val sampleText = "DMART SUPERMARKET\nDate: 25/09/2026\nAtta ₹280.00\nCooking Oil ₹340.00\nTOTAL: 620.00\nUPI"
        val parsed = ReceiptOCRParser.parseReceiptText(sampleText)
        assertEquals("DMART SUPERMARKET", parsed.merchant)
        assertEquals(620.0, parsed.amount, 0.01)
        assertNotNull(parsed.category)
        assertTrue(parsed.confidence > 0.5f)
    }
}
