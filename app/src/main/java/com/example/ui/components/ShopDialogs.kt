package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.DailyCashRegisterEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.KhataPartyEntity
import com.example.data.local.entities.ShopInvoiceEntity
import com.example.data.local.entities.ShopProfileEntity
import com.example.ui.theme.AnomalyWarning
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TealAccent
import com.example.util.CurrencyFormatter
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------- 1. Add / Edit Khata Party Dialog ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataPartyDialog(
    userId: Long,
    existingParty: KhataPartyEntity? = null,
    initialType: String = "CUSTOMER",
    onDismiss: () -> Unit,
    onSave: (KhataPartyEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingParty?.name ?: "") }
    var phone by remember { mutableStateOf(existingParty?.phone ?: "") }
    var type by remember { mutableStateOf(existingParty?.type ?: initialType) }
    var address by remember { mutableStateOf(existingParty?.address ?: "") }
    var creditLimitStr by remember { mutableStateOf(existingParty?.creditLimit?.toInt()?.toString() ?: "25000") }
    var notes by remember { mutableStateOf(existingParty?.notes ?: "") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingParty == null) "Add ${if (type == "CUSTOMER") "Customer" else "Supplier"}" else "Edit ${existingParty.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Selector (Customer vs Supplier)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "CUSTOMER",
                        onClick = { type = "CUSTOMER" },
                        label = { Text("Customer (Client)") },
                        modifier = Modifier.weight(1f).testTag("select_type_customer")
                    )
                    FilterChip(
                        selected = type == "SUPPLIER",
                        onClick = { type = "SUPPLIER" },
                        label = { Text("Supplier (Vendor)") },
                        modifier = Modifier.weight(1f).testTag("select_type_supplier")
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Name / Business *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("party_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("party_phone_input")
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { creditLimitStr = it },
                    label = { Text("Credit Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Terms, GSTIN)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter party name."
                        return@Button
                    }
                    val limit = creditLimitStr.toDoubleOrNull() ?: 25000.0
                    val party = existingParty?.copy(
                        name = name.trim(),
                        phone = phone.trim(),
                        type = type,
                        address = address.trim(),
                        creditLimit = limit,
                        notes = notes.trim()
                    ) ?: KhataPartyEntity(
                        userId = userId,
                        name = name.trim(),
                        phone = phone.trim(),
                        type = type,
                        address = address.trim(),
                        creditLimit = limit,
                        notes = notes.trim()
                    )
                    onSave(party)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_party_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- 2. Khata Entry Dialog (Record You Gave / You Got) ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KhataEntryDialog(
    party: KhataPartyEntity,
    initialType: String = "GAVE", // "GAVE" or "GOT"
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, desc: String, billNo: String, paymentMethod: String) -> Unit
) {
    var entryType by remember { mutableStateOf(initialType) }
    var amountStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val paymentOptions = listOf("Cash", "UPI / GPay", "Bank Transfer", "Credit / Udhar", "Cheque")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (entryType == "GAVE") ExpenseRed else IncomeGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (entryType == "GAVE") "You Gave ₹ to ${party.name}" else "You Got ₹ from ${party.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = entryType == "GAVE",
                        onClick = { entryType = "GAVE" },
                        label = { Text("🔴 You Gave (Udhar)") },
                        modifier = Modifier.weight(1f).testTag("select_entry_gave")
                    )
                    FilterChip(
                        selected = entryType == "GOT",
                        onClick = { entryType = "GOT" },
                        label = { Text("🟢 You Got (Jama)") },
                        modifier = Modifier.weight(1f).testTag("select_entry_got")
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it; errorMsg = null },
                    label = { Text("Amount (₹) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("khata_amount_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Item details") },
                    placeholder = { Text("e.g. Sold 2 bags cement, or partial payment") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("khata_desc_input")
                )

                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("Bill / Invoice # (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment Mode Chips
                Text("Payment Mode", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Cash", "UPI", "Bank").forEach { mode ->
                        FilterChip(
                            selected = paymentMethod.startsWith(mode),
                            onClick = { paymentMethod = mode },
                            label = { Text(mode, fontSize = 11.sp) }
                        )
                    }
                }

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMsg = "Please enter a valid amount."
                        return@Button
                    }
                    onSave(entryType, amt, description.trim(), billNumber.trim(), paymentMethod)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (entryType == "GAVE") ExpenseRed else IncomeGreen
                ),
                modifier = Modifier.testTag("save_khata_entry_button")
            ) {
                Text("Save Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- 3. Inventory Item Dialog ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryItemDialog(
    userId: Long,
    existingItem: InventoryItemEntity? = null,
    onDismiss: () -> Unit,
    onSave: (InventoryItemEntity) -> Unit
) {
    var name by remember { mutableStateOf(existingItem?.name ?: "") }
    var sku by remember { mutableStateOf(existingItem?.sku ?: "") }
    var category by remember { mutableStateOf(existingItem?.category ?: "Retail Goods") }
    var unit by remember { mutableStateOf(existingItem?.unit ?: "Pcs") }
    var purchasePriceStr by remember { mutableStateOf(existingItem?.purchasePrice?.toString() ?: "") }
    var sellingPriceStr by remember { mutableStateOf(existingItem?.sellingPrice?.toString() ?: "") }
    var currentStockStr by remember { mutableStateOf(existingItem?.currentStock?.toString() ?: "10") }
    var minAlertStr by remember { mutableStateOf(existingItem?.minStockAlert?.toString() ?: "5") }
    var taxRateStr by remember { mutableStateOf(existingItem?.taxRate?.toInt()?.toString() ?: "18") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Raw Materials", "Finished Goods", "Retail Goods", "Spares & Tools", "Packaging")
    val units = listOf("Pcs", "Kg", "Meter", "Liter", "Bag", "Box", "Dozen", "Ton")
    val gstSlabs = listOf("0", "5", "12", "18", "28")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingItem == null) "Add Item / Raw Material" else "Edit Item",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; errorMsg = null },
                        label = { Text("Item Name *") },
                        placeholder = { Text("e.g. Steel Bar 12mm, Basmati Rice") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("item_name_input")
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU / Code") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit (Kg, Pcs)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Raw Materials", "Finished Goods", "Retail Goods").forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = { Text("Buy / Cost Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("item_buy_price")
                        )
                        OutlinedTextField(
                            value = sellingPriceStr,
                            onValueChange = { sellingPriceStr = it },
                            label = { Text("Sell Price (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("item_sell_price")
                        )
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentStockStr,
                            onValueChange = { currentStockStr = it },
                            label = { Text("Current Stock") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("item_stock_input")
                        )
                        OutlinedTextField(
                            value = minAlertStr,
                            onValueChange = { minAlertStr = it },
                            label = { Text("Low Stock Alert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Text("GST Tax Slab", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gstSlabs.forEach { slab ->
                            FilterChip(
                                selected = taxRateStr == slab,
                                onClick = { taxRateStr = slab },
                                label = { Text("$slab%") }
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    item {
                        Text(text = errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter an item name."
                        return@Button
                    }
                    val buy = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val sell = sellingPriceStr.toDoubleOrNull() ?: buy
                    val stock = currentStockStr.toDoubleOrNull() ?: 0.0
                    val minAlert = minAlertStr.toDoubleOrNull() ?: 5.0
                    val tax = taxRateStr.toDoubleOrNull() ?: 18.0

                    val item = existingItem?.copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category,
                        unit = unit.trim(),
                        purchasePrice = buy,
                        sellingPrice = sell,
                        currentStock = stock,
                        minStockAlert = minAlert,
                        taxRate = tax,
                        updatedAt = System.currentTimeMillis()
                    ) ?: InventoryItemEntity(
                        userId = userId,
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category,
                        unit = unit.trim(),
                        purchasePrice = buy,
                        sellingPrice = sell,
                        currentStock = stock,
                        minStockAlert = minAlert,
                        taxRate = tax
                    )
                    onSave(item)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_inventory_item_button")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- 4. Stock Adjustment Dialog (Stock In / Stock Out) ----------------
@Composable
fun StockAdjustmentDialog(
    item: InventoryItemEntity,
    initialType: String = "STOCK_IN",
    onDismiss: () -> Unit,
    onAdjust: (type: String, quantity: Double, unitPrice: Double, reason: String, note: String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var quantityStr by remember { mutableStateOf("") }
    var unitPriceStr by remember {
        mutableStateOf(
            if (initialType == "STOCK_IN") item.purchasePrice.toString() else item.sellingPrice.toString()
        )
    }
    var reason by remember {
        mutableStateOf(if (initialType == "STOCK_IN") "Purchase" else "Sale")
    }
    var note by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val reasonsIn = listOf("Purchase", "Production Batch", "Returned by Customer", "Adjustment")
    val reasonsOut = listOf("Sale", "Production Consumption", "Damaged / Waste", "Adjustment")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${if (type == "STOCK_IN") "+ Stock In" else "- Stock Out"}: ${item.name}",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "STOCK_IN",
                        onClick = {
                            type = "STOCK_IN"
                            reason = "Purchase"
                            unitPriceStr = item.purchasePrice.toString()
                        },
                        label = { Text("🟢 Stock In (Add)") },
                        modifier = Modifier.weight(1f).testTag("select_stock_in")
                    )
                    FilterChip(
                        selected = type == "STOCK_OUT",
                        onClick = {
                            type = "STOCK_OUT"
                            reason = "Sale"
                            unitPriceStr = item.sellingPrice.toString()
                        },
                        label = { Text("🔴 Stock Out (Deduct)") },
                        modifier = Modifier.weight(1f).testTag("select_stock_out")
                    )
                }

                Text(
                    text = "Current Stock: ${item.currentStock} ${item.unit}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it; errorMsg = null },
                    label = { Text("Quantity (${item.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("stock_qty_input")
                )

                OutlinedTextField(
                    value = unitPriceStr,
                    onValueChange = { unitPriceStr = it },
                    label = { Text("Unit Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Reason", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentReasons = if (type == "STOCK_IN") reasonsIn else reasonsOut
                    currentReasons.take(3).forEach { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Notes / Batch # (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(text = errorMsg!!, color = ExpenseRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityStr.toDoubleOrNull()
                    if (qty == null || qty <= 0) {
                        errorMsg = "Please enter a valid quantity."
                        return@Button
                    }
                    if (type == "STOCK_OUT" && qty > item.currentStock) {
                        errorMsg = "Stock out quantity ($qty) exceeds current stock (${item.currentStock})."
                        return@Button
                    }
                    val price = unitPriceStr.toDoubleOrNull() ?: 0.0
                    onAdjust(type, qty, price, reason, note.trim())
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "STOCK_IN") IncomeGreen else ExpenseRed
                ),
                modifier = Modifier.testTag("save_stock_adjustment_button")
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Data holder for Invoice Line Item in creator
data class InvoiceLineItemDraft(
    val name: String,
    val qty: Double,
    val unit: String,
    val rate: Double
) {
    val total: Double get() = qty * rate
}

// ---------------- 5. Professional Invoice Creator Dialog ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceCreateDialog(
    userId: Long,
    parties: List<KhataPartyEntity>,
    inventoryItems: List<InventoryItemEntity>,
    onDismiss: () -> Unit,
    onSaveInvoice: (ShopInvoiceEntity, autoKhata: Boolean, autoDeductInventory: Boolean) -> Unit
) {
    var partyName by remember { mutableStateOf("") }
    var partyPhone by remember { mutableStateOf("") }
    var selectedPartyId by remember { mutableStateOf<Long?>(null) }
    var invoiceType by remember { mutableStateOf("SALE") }
    var discountStr by remember { mutableStateOf("0") }
    var taxRateStr by remember { mutableStateOf("5") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var isCreditPayment by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var autoDeductStock by remember { mutableStateOf(true) }
    var autoLinkKhata by remember { mutableStateOf(true) }

    // Draft items
    val lineItems = remember { mutableStateListOf<InvoiceLineItemDraft>() }

    // Add item picker sub-state
    var selectedInventoryItem by remember { mutableStateOf<InventoryItemEntity?>(inventoryItems.firstOrNull()) }
    var itemQtyStr by remember { mutableStateOf("1") }
    var itemRateStr by remember { mutableStateOf(inventoryItems.firstOrNull()?.sellingPrice?.toString() ?: "100") }

    val subtotal = lineItems.sumOf { it.total }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val taxRate = taxRateStr.toDoubleOrNull() ?: 0.0
    val taxableAmount = (subtotal - discount).coerceAtLeast(0.0)
    val taxAmount = (taxableAmount * taxRate) / 100.0
    val grandTotal = taxableAmount + taxAmount

    val invoiceNo = remember {
        "INV-" + SimpleDateFormat("yyMMdd-HHmm", Locale.US).format(Date())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create GST / Cash Invoice",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = invoiceNo,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customer Selection
                    item {
                        Text("Customer Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = partyName,
                                onValueChange = {
                                    partyName = it
                                    selectedPartyId = null
                                },
                                label = { Text("Customer Name / Walk-in") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("invoice_customer_name")
                            )
                            OutlinedTextField(
                                value = partyPhone,
                                onValueChange = { partyPhone = it },
                                label = { Text("Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Quick Select from Khata
                        if (parties.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Or select saved party:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parties.take(4).forEach { p ->
                                    FilterChip(
                                        selected = selectedPartyId == p.id,
                                        onClick = {
                                            selectedPartyId = p.id
                                            partyName = p.name
                                            partyPhone = p.phone
                                        },
                                        label = { Text(p.name, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Add Items Section
                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Add Item to Bill", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                        if (inventoryItems.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                inventoryItems.take(4).forEach { itm ->
                                    FilterChip(
                                        selected = selectedInventoryItem?.id == itm.id,
                                        onClick = {
                                            selectedInventoryItem = itm
                                            itemRateStr = itm.sellingPrice.toString()
                                        },
                                        label = { Text(itm.name.take(12), fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = itemQtyStr,
                                onValueChange = { itemQtyStr = it },
                                label = { Text("Qty (${selectedInventoryItem?.unit ?: "Pcs"})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = itemRateStr,
                                onValueChange = { itemRateStr = it },
                                label = { Text("Rate (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val q = itemQtyStr.toDoubleOrNull() ?: 1.0
                                    val r = itemRateStr.toDoubleOrNull() ?: 0.0
                                    val name = selectedInventoryItem?.name ?: "Custom Item"
                                    val u = selectedInventoryItem?.unit ?: "Pcs"
                                    if (q > 0 && r > 0) {
                                        lineItems.add(InvoiceLineItemDraft(name, q, u, r))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.testTag("add_item_to_invoice_btn")
                            ) {
                                Text("+ Add")
                            }
                        }
                    }

                    // Added Line Items List
                    if (lineItems.isEmpty()) {
                        item {
                            Text(
                                "No items added yet. Pick an item above to add.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        item {
                            Text("Invoice Items (${lineItems.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        items(lineItems) { line ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(line.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${line.qty} ${line.unit} × ${CurrencyFormatter.formatINR(line.rate)}", fontSize = 11.sp)
                                    }
                                    Text(CurrencyFormatter.formatINR(line.total), fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { lineItems.remove(line) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Tax & Discounts
                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                label = { Text("Discount (₹)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = taxRateStr,
                                onValueChange = { taxRateStr = it },
                                label = { Text("GST Rate %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Payment Mode & Credit
                    item {
                        Text("Payment Status & Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "UPI", "Credit / Udhar").forEach { mode ->
                                FilterChip(
                                    selected = if (mode.startsWith("Credit")) isCreditPayment else paymentMode == mode && !isCreditPayment,
                                    onClick = {
                                        if (mode.startsWith("Credit")) {
                                            isCreditPayment = true
                                            paymentMode = "Khata Credit"
                                        } else {
                                            isCreditPayment = false
                                            paymentMode = mode
                                        }
                                    },
                                    label = { Text(mode) }
                                )
                            }
                        }

                        if (isCreditPayment) {
                            Text(
                                "ℹ️ This bill amount will automatically be added to ${partyName.ifBlank { "Customer" }}'s Khata balance.",
                                fontSize = 11.sp,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    // Auto options
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-deduct item stock from inventory", fontSize = 12.sp)
                            Switch(checked = autoDeductStock, onCheckedChange = { autoDeductStock = it })
                        }
                    }
                }

                // Summary Footer & Create Button
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", fontSize = 12.sp)
                            Text(CurrencyFormatter.formatINR(subtotal), fontSize = 12.sp)
                        }
                        if (taxAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GST ($taxRate%)", fontSize = 12.sp)
                                Text("+${CurrencyFormatter.formatINR(taxAmount)}", fontSize = 12.sp)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(CurrencyFormatter.formatINR(grandTotal), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EmeraldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (lineItems.isEmpty()) return@Button
                            val jsonArr = JSONArray()
                            for (it in lineItems) {
                                val obj = JSONObject()
                                obj.put("name", it.name)
                                obj.put("qty", it.qty)
                                obj.put("unit", it.unit)
                                obj.put("rate", it.rate)
                                obj.put("total", it.total)
                                jsonArr.put(obj)
                            }

                            val paidAmt = if (isCreditPayment) 0.0 else grandTotal
                            val status = if (isCreditPayment) "CREDIT_UDHAR" else "PAID"

                            val invoice = ShopInvoiceEntity(
                                userId = userId,
                                invoiceNumber = invoiceNo,
                                partyId = selectedPartyId,
                                partyName = partyName.ifBlank { "Walk-in Customer" },
                                partyPhone = partyPhone,
                                type = invoiceType,
                                subtotal = subtotal,
                                discountAmount = discount,
                                taxRate = taxRate,
                                taxAmount = taxAmount,
                                grandTotal = grandTotal,
                                paidAmount = paidAmt,
                                paymentStatus = status,
                                paymentMode = paymentMode,
                                itemsJson = jsonArr.toString(),
                                notes = notes
                            )
                            onSaveInvoice(invoice, autoLinkKhata && isCreditPayment, autoDeductStock)
                        },
                        enabled = lineItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).testTag("confirm_create_invoice_btn")
                    ) {
                        Text("Generate Bill")
                    }
                }
            }
        }
    }
}

// ---------------- 6. Invoice Slip Viewer / Share Dialog ----------------
@Composable
fun InvoiceSlipDialog(
    invoice: ShopInvoiceEntity,
    shopProfile: ShopProfileEntity?,
    slipText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bill Slip #${invoice.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = slipText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, slipText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Invoice Bill")
                    context.startActivity(shareIntent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("share_invoice_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share via WhatsApp / Text")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ---------------- 7. Daily Cash Register (Galla) Dialog ----------------
@Composable
fun DailyCashRegisterDialog(
    userId: Long,
    existingRegister: DailyCashRegisterEntity?,
    onDismiss: () -> Unit,
    onSave: (DailyCashRegisterEntity) -> Unit
) {
    var openingCashStr by remember { mutableStateOf(existingRegister?.openingCash?.toInt()?.toString() ?: "5000") }
    var actualCashStr by remember { mutableStateOf(existingRegister?.actualClosingCash?.toInt()?.toString() ?: "5000") }
    var notes by remember { mutableStateOf(existingRegister?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Daily Cash Counter (Galla)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Verify drawer cash balance for today. Tally cash inflows and payouts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = openingCashStr,
                    onValueChange = { openingCashStr = it },
                    label = { Text("Morning Opening Cash (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("opening_cash_input")
                )

                OutlinedTextField(
                    value = actualCashStr,
                    onValueChange = { actualCashStr = it },
                    label = { Text("Actual Drawer Cash Count (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("actual_cash_input")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g. Discrepancy, Cash handed to owner)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val opening = openingCashStr.toDoubleOrNull() ?: 0.0
                    val actual = actualCashStr.toDoubleOrNull() ?: opening
                    val reg = existingRegister?.copy(
                        openingCash = opening,
                        actualClosingCash = actual,
                        notes = notes.trim()
                    ) ?: DailyCashRegisterEntity(
                        userId = userId,
                        openingCash = opening,
                        actualClosingCash = actual,
                        notes = notes.trim()
                    )
                    onSave(reg)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// ---------------- 8. Shop Profile Settings Dialog ----------------
@Composable
fun ShopProfileDialog(
    userId: Long,
    existingProfile: ShopProfileEntity?,
    onDismiss: () -> Unit,
    onSave: (ShopProfileEntity) -> Unit
) {
    var businessName by remember { mutableStateOf(existingProfile?.businessName ?: "") }
    var ownerName by remember { mutableStateOf(existingProfile?.ownerName ?: "") }
    var phone by remember { mutableStateOf(existingProfile?.phone ?: "") }
    var address by remember { mutableStateOf(existingProfile?.address ?: "") }
    var gstin by remember { mutableStateOf(existingProfile?.gstin ?: "") }
    var upiId by remember { mutableStateOf(existingProfile?.upiId ?: "") }
    var businessType by remember { mutableStateOf(existingProfile?.businessType ?: "Kirana & Retail") }
    var isGstEnabled by remember { mutableStateOf(existingProfile?.isGstEnabled ?: false) }

    val businessTypes = listOf(
        "Kirana & Retail",
        "Small Scale Industry / Workshop",
        "Garments & Textiles",
        "Hardware & Electricals",
        "Food & Restaurant",
        "Wholesale & Spares",
        "Services & Repair"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Business / Shop Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it },
                        label = { Text("Business / Shop Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("business_name_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = upiId,
                            onValueChange = { upiId = it },
                            label = { Text("UPI ID (for bills)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Shop / Factory Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = { Text("GSTIN (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text("Business Type", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        businessTypes.take(3).forEach { bt ->
                            FilterChip(
                                selected = businessType == bt,
                                onClick = { businessType = bt },
                                label = { Text(bt.take(14), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (businessName.isBlank()) return@Button
                    val profile = existingProfile?.copy(
                        businessName = businessName.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        gstin = gstin.trim(),
                        upiId = upiId.trim(),
                        businessType = businessType,
                        isGstEnabled = gstin.isNotBlank()
                    ) ?: ShopProfileEntity(
                        userId = userId,
                        businessName = businessName.trim(),
                        ownerName = ownerName.trim(),
                        phone = phone.trim(),
                        address = address.trim(),
                        gstin = gstin.trim(),
                        upiId = upiId.trim(),
                        businessType = businessType,
                        isGstEnabled = gstin.isNotBlank()
                    )
                    onSave(profile)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("save_shop_profile_btn")
            ) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
