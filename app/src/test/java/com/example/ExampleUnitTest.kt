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
}

