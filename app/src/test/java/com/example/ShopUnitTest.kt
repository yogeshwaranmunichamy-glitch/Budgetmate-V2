package com.example

import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.ShopInvoiceEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopUnitTest {

    @Test
    fun testInventoryCalculationsAndAlerts() {
        val normalItem = InventoryItemEntity(
            userId = 1,
            name = "Basmati Rice 25kg",
            purchasePrice = 1800.0,
            sellingPrice = 2150.0,
            currentStock = 20.0,
            minStockAlert = 5.0
        )

        assertEquals(36000.0, normalItem.stockValue, 0.01)
        assertEquals(43000.0, normalItem.retailValue, 0.01)
        assertFalse(normalItem.isLowStock)
        assertFalse(normalItem.isOutOfStock)

        val lowStockItem = normalItem.copy(currentStock = 3.0)
        assertTrue(lowStockItem.isLowStock)
        assertFalse(lowStockItem.isOutOfStock)

        val outOfStockItem = normalItem.copy(currentStock = 0.0)
        assertFalse(outOfStockItem.isLowStock)
        assertTrue(outOfStockItem.isOutOfStock)
    }

    @Test
    fun testShopInvoiceGrandTotalAndBalanceDue() {
        val invoice = ShopInvoiceEntity(
            userId = 1,
            invoiceNumber = "INV-2026-001",
            partyName = "Ramesh Sharma",
            type = "SALE",
            subtotal = 5000.0,
            discountAmount = 200.0,
            taxRate = 18.0,
            taxAmount = 864.0, // (5000 - 200) * 0.18 = 864
            grandTotal = 5664.0,
            paidAmount = 2000.0,
            paymentStatus = "PARTIAL"
        )

        assertFalse(invoice.isFullyPaid)
        assertEquals(3664.0, invoice.balanceDue, 0.01)

        val fullyPaid = invoice.copy(paidAmount = 5664.0, paymentStatus = "PAID")
        assertTrue(fullyPaid.isFullyPaid)
        assertEquals(0.0, fullyPaid.balanceDue, 0.01)
    }

    @Test
    fun testDailyCashRegisterTallyCalculations() {
        val register = DailyCashRegisterEntity(
            userId = 1,
            openingCash = 5000.0,
            cashSales = 4500.0,
            cashKhataCollected = 1500.0,
            cashShopExpenses = 800.0,
            cashSupplierPaid = 2000.0,
            actualClosingCash = 8200.0
        )

        assertEquals(6000.0, register.totalCashInflow, 0.01)
        assertEquals(2800.0, register.totalCashOutflow, 0.01)
        // Expected = 5000 + 6000 - 2800 = 8200
        assertEquals(8200.0, register.expectedClosingCash, 0.01)
        assertEquals(0.0, register.cashDiscrepancy, 0.01) // Perfect match

        val shortageRegister = register.copy(actualClosingCash = 8000.0)
        assertEquals(-200.0, shortageRegister.cashDiscrepancy, 0.01) // Shortage of 200
    }

    @Test
    fun testKhataPartyEntityDefaults() {
        val customer = KhataPartyEntity(
            userId = 1,
            name = "Anand Fabricators",
            phone = "9876543210",
            type = "CUSTOMER",
            currentBalance = 4500.0
        )
        assertEquals("CUSTOMER", customer.type)
        assertEquals(4500.0, customer.currentBalance, 0.01)

        val supplier = KhataPartyEntity(
            userId = 1,
            name = "Balaji Hardware",
            type = "SUPPLIER",
            currentBalance = 8000.0
        )
        assertEquals("SUPPLIER", supplier.type)
        assertEquals(8000.0, supplier.currentBalance, 0.01)
    }
}
