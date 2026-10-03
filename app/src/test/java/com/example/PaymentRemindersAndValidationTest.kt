package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.PaymentReminderEntity
import com.example.util.CurrencyFormatter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PaymentRemindersAndValidationTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPaymentReminderEntityCreationAndTypes() {
        val now = System.currentTimeMillis()
        val udhar = PaymentReminderEntity(
            userId = 1L,
            title = "Ramesh Grocery Udhar",
            personOrEntity = "Ramesh Kumar",
            amount = 500.0,
            reminderType = "TO_COLLECT",
            dueDate = now + 86400000L,
            reminderTime = "10:00 AM",
            isAlarmEnabled = true,
            status = "PENDING",
            phoneNumber = "9840123456",
            notes = "Emergency money"
        )

        assertEquals("TO_COLLECT", udhar.reminderType)
        assertEquals("Ramesh Kumar", udhar.personOrEntity)
        assertEquals(500.0, udhar.amount, 0.001)
        assertEquals("PENDING", udhar.status)
        assertTrue(udhar.isAlarmEnabled)

        val loanEmi = PaymentReminderEntity(
            userId = 1L,
            title = "HDFC Car Loan EMI",
            personOrEntity = "HDFC Bank",
            amount = 12500.0,
            reminderType = "TO_PAY",
            dueDate = now + (86400000L * 3),
            reminderTime = "09:00 AM",
            isAlarmEnabled = true,
            status = "PENDING",
            repeatInterval = "MONTHLY"
        )

        assertEquals("TO_PAY", loanEmi.reminderType)
        assertEquals(12500.0, loanEmi.amount, 0.001)
        assertEquals("MONTHLY", loanEmi.repeatInterval)
    }

    @Test
    fun testRoomDaoInsertQueryAndStatusLifecycle() = runBlocking {
        val dao = database.paymentReminderDao()
        val now = System.currentTimeMillis()

        val rameshReminder = PaymentReminderEntity(
            userId = 1L,
            title = "Ramesh 500 Udhar",
            personOrEntity = "Ramesh",
            amount = 500.0,
            reminderType = "TO_COLLECT",
            dueDate = now + 86400000L,
            status = "PENDING"
        )

        val id = dao.insertReminder(rameshReminder)
        assertTrue(id > 0)

        // Query pending
        val pendingList = dao.getPendingReminders(1L).first()
        assertEquals(1, pendingList.size)
        assertEquals("Ramesh", pendingList[0].personOrEntity)
        assertEquals(500.0, pendingList[0].amount, 0.001)

        // Mark completed
        dao.markCompleted(id)
        val afterCompleted = dao.getPendingReminders(1L).first()
        assertEquals(0, afterCompleted.size)

        val allList = dao.getAllReminders(1L).first()
        assertEquals(1, allList.size)
        assertEquals("COMPLETED", allList[0].status)
        assertNotNull(allList[0].completedAt)
    }

    @Test
    fun testSnoozeUpdatesDueDateAndStatus() = runBlocking {
        val dao = database.paymentReminderDao()
        val now = System.currentTimeMillis()

        val loanReminder = PaymentReminderEntity(
            userId = 1L,
            title = "Personal Loan EMI",
            personOrEntity = "SBI Bank",
            amount = 8000.0,
            reminderType = "TO_PAY",
            dueDate = now,
            status = "PENDING"
        )
        val id = dao.insertReminder(loanReminder)

        val snoozedDueDate = now + (86400000L * 2) // +2 days
        dao.snoozeReminder(id, snoozedDueDate)

        val retrieved = dao.getReminderById(id)
        assertNotNull(retrieved)
        assertEquals("SNOOZED", retrieved!!.status)
        assertEquals(snoozedDueDate, retrieved.dueDate)
    }

    @Test
    fun testCurrencyFormattingValidation() {
        assertEquals("₹500", CurrencyFormatter.formatINR(500.0))
        assertEquals("₹500.00", CurrencyFormatter.formatINR(500.0, includeDecimals = true))
        assertEquals("₹12,500", CurrencyFormatter.formatINR(12500.0))
        assertEquals("₹100,000", CurrencyFormatter.formatINR(100000.0))
        assertEquals("₹0", CurrencyFormatter.formatINR(0.0))
    }

    @Test
    fun testNudgeMessageTemplateGeneration() {
        val reminder = PaymentReminderEntity(
            userId = 1L,
            title = "Udhar Repayment",
            personOrEntity = "Ramesh Kumar",
            amount = 500.0,
            reminderType = "TO_COLLECT",
            dueDate = 1790899200000L, // Fixed timestamp for test reproducibility
            phoneNumber = "9840123456"
        )

        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val dateStr = sdf.format(Date(reminder.dueDate))
        val formattedAmount = CurrencyFormatter.formatINR(reminder.amount)

        val msg = "Hi ${reminder.personOrEntity}, gentle reminder that payment of $formattedAmount for '${reminder.title}' is due by $dateStr. Please settle via UPI/GPay. Thank you!"

        assertTrue(msg.contains("Ramesh Kumar"))
        assertTrue(msg.contains("₹500"))
        assertTrue(msg.contains("UPI/GPay"))
        assertTrue(msg.contains("gentle reminder"))
    }

    @Test
    fun testAlarmToggleInDatabase() = runBlocking {
        val dao = database.paymentReminderDao()
        val reminder = PaymentReminderEntity(
            userId = 1L,
            title = "Power Bill",
            personOrEntity = "Electricity Dept",
            amount = 1450.0,
            reminderType = "BILL",
            dueDate = System.currentTimeMillis() + 86400000L,
            isAlarmEnabled = true
        )
        val id = dao.insertReminder(reminder)

        dao.toggleAlarm(id, false)
        val updated = dao.getReminderById(id)
        assertNotNull(updated)
        assertFalse(updated!!.isAlarmEnabled)

        dao.toggleAlarm(id, true)
        val reEnabled = dao.getReminderById(id)
        assertNotNull(reEnabled)
        assertTrue(reEnabled!!.isAlarmEnabled)
    }
}
