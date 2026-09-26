package com.example

import com.example.data.local.entities.TripEntity
import com.example.data.local.entities.TripExpenseEntity
import com.example.ml.TripAnalyticsEngine
import com.example.ml.TripVoiceQueryType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testTripVoiceExpenseParsing_TamilAndEnglish() {
        val companions = listOf("Me", "Rahul", "Priya", "Arun")
        val parsed = TripAnalyticsEngine.parseTripVoiceExpense(
            "Goa trip-ku cab 650 rupees Rahul kuduthan",
            companions
        )
        assertEquals(650.0, parsed.amount, 0.01)
        assertEquals("Local Commute", parsed.category)
        assertEquals("Rahul", parsed.paidBy)
    }

    @Test
    fun testTripVoiceExpenseParsing_Dinner() {
        val companions = listOf("Me", "Rahul", "Priya", "Arun")
        val parsed = TripAnalyticsEngine.parseTripVoiceExpense(
            "Paid 1200 for hotel dinner in Goa",
            companions
        )
        assertEquals(1200.0, parsed.amount, 0.01)
        assertEquals("Food & Dining", parsed.category)
        assertEquals("Me", parsed.paidBy)
    }

    @Test
    fun testTripAnalyticsAndSplitwiseSettlements() {
        val now = System.currentTimeMillis()
        val trip = TripEntity(
            id = 1L,
            userId = 1L,
            name = "Goa Trip",
            destination = "Goa",
            startDate = now,
            endDate = now + 86400000L * 4,
            budget = 20000.0,
            companions = "Me, Rahul"
        )

        // Me paid 1000 for both -> each share is 500 -> Rahul owes Me 500
        val expenses = listOf(
            TripExpenseEntity(
                id = 1L,
                tripId = 1L,
                userId = 1L,
                title = "Dinner",
                amount = 1000.0,
                category = "Food & Dining",
                paidBy = "Me",
                splitAmong = "All",
                date = now
            )
        )

        val analytics = TripAnalyticsEngine.analyzeTrip(trip, expenses)
        assertEquals(1000.0, analytics.totalSpent, 0.01)
        assertEquals(19000.0, analytics.remainingBudget, 0.01)
        assertEquals(1, analytics.settlements.size)
        assertEquals("Rahul", analytics.settlements[0].from)
        assertEquals("Me", analytics.settlements[0].to)
        assertEquals(500.0, analytics.settlements[0].amount, 0.01)
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
        val r1 = com.example.ml.MultilingualNLP.parseInput("Swiggy food order 450 rupees")
        assertEquals(450.0, r1.amount, 0.01)
        assertEquals("Food", r1.category)

        val r2 = com.example.ml.MultilingualNLP.parseInput("Paid fifty rupees for tea")
        assertEquals(50.0, r2.amount, 0.01)
        assertEquals("Food", r2.category)
    }

    @Test
    fun testMultilingualNLP_CustomKeywordTraining() {
        com.example.ml.MultilingualNLP.trainCustomKeyword("fitnesstribe", "Medical")
        val result = com.example.ml.MultilingualNLP.parseInput("Paid 1500 for fitnesstribe membership")
        assertEquals("Medical", result.category)
        assertEquals(1500.0, result.amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_FourExpenses() {
        val input = "I spent ₹500 for petrol, ₹300 for food, ₹200 for parking and ₹1,000 for hotel."
        val items = com.example.ml.MultilingualNLP.parseMultiInput(input)

        assertEquals(4, items.size)
        assertEquals("Petrol", items[0].category)
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
        val items = com.example.ml.MultilingualNLP.parseMultiInput(input)

        assertEquals(2, items.size)
        assertEquals("Petrol", items[0].category)
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(300.0, items[1].amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_CompactSpeech() {
        val input = "Petrol 500, food 300, parking 200."
        val items = com.example.ml.MultilingualNLP.parseMultiInput(input)

        assertEquals(3, items.size)
        assertEquals("Petrol", items[0].category)
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(300.0, items[1].amount, 0.01)

        assertEquals("Parking", items[2].category)
        assertEquals(200.0, items[2].amount, 0.01)
    }

    @Test
    fun testMultiCategoryParsing_NaturalSentence() {
        val input = "Today I spent around 500 for fuel, 250 for lunch and 100 for parking."
        val items = com.example.ml.MultilingualNLP.parseMultiInput(input)

        assertEquals(3, items.size)
        assertEquals("Petrol", items[0].category)
        assertEquals(500.0, items[0].amount, 0.01)

        assertEquals("Food", items[1].category)
        assertEquals(250.0, items[1].amount, 0.01)

        assertEquals("Parking", items[2].category)
        assertEquals(100.0, items[2].amount, 0.01)
    }

    @Test
    fun testVoiceConfirmationAndCorrection_Flow() {
        // Step 1: Initial list: Petrol 500, Food 300, Parking 200
        val initialItems = com.example.ml.MultilingualNLP.parseMultiInput("Petrol 500, food 300, parking 200.")
        assertEquals(3, initialItems.size)

        // Step 2: Readback formatting
        val prompt1 = com.example.ml.MultilingualNLP.formatConfirmationPrompt(initialItems, isUpdate = false)
        assertTrue(prompt1.contains("Petrol ₹500"))
        assertTrue(prompt1.contains("Food ₹300"))
        assertTrue(prompt1.contains("Parking ₹200"))
        assertTrue(prompt1.contains("Is this correct?"))

        // Step 3: Voice Correction: "Petrol should be 600"
        val cor1 = com.example.ml.MultilingualNLP.processVoiceResponse("Petrol should be 600", initialItems)
        assertTrue(cor1 is com.example.ml.VoiceCorrectionResult.Updated)
        val updatedItems1 = (cor1 as com.example.ml.VoiceCorrectionResult.Updated).items
        assertEquals(600.0, updatedItems1.first { it.category == "Petrol" }.amount, 0.01)
        assertTrue(cor1.speechPrompt.contains("Updated list:"))
        assertTrue(cor1.speechPrompt.contains("Petrol ₹600"))

        // Step 4: Voice Correction: "Food is actually 350"
        val cor2 = com.example.ml.MultilingualNLP.processVoiceResponse("Food is actually 350", updatedItems1)
        assertTrue(cor2 is com.example.ml.VoiceCorrectionResult.Updated)
        val updatedItems2 = (cor2 as com.example.ml.VoiceCorrectionResult.Updated).items
        assertEquals(350.0, updatedItems2.first { it.category == "Food" }.amount, 0.01)

        // Step 5: Voice Correction: "Remove parking"
        val cor3 = com.example.ml.MultilingualNLP.processVoiceResponse("Remove parking", updatedItems2)
        assertTrue(cor3 is com.example.ml.VoiceCorrectionResult.Removed)
        val updatedItems3 = (cor3 as com.example.ml.VoiceCorrectionResult.Removed).items
        assertEquals(2, updatedItems3.size)
        assertFalse(updatedItems3.any { it.category == "Parking" })

        // Step 6: Voice Correction: "Add 200 for toll"
        val cor4 = com.example.ml.MultilingualNLP.processVoiceResponse("Add 200 for toll", updatedItems3)
        assertTrue(cor4 is com.example.ml.VoiceCorrectionResult.Added)
        val updatedItems4 = (cor4 as com.example.ml.VoiceCorrectionResult.Added).items
        assertEquals(3, updatedItems4.size)
        assertTrue(updatedItems4.any { it.category == "Toll" && it.amount == 200.0 })

        // Step 7: Voice Confirmation: "Okay"
        val confirmResult = com.example.ml.MultilingualNLP.processVoiceResponse("Okay", updatedItems4)
        assertTrue(confirmResult is com.example.ml.VoiceCorrectionResult.Confirmed)
        assertEquals(3, (confirmResult as com.example.ml.VoiceCorrectionResult.Confirmed).items.size)
    }
}

