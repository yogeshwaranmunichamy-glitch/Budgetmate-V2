package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.TransactionEntity
import com.example.ml.ParsedReceiptData
import com.example.ml.ReceiptOCRParser
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onReceiptConfirmed: (TransactionEntity, ReceiptScanEntity) -> Unit
) {
    var rawOcrText by remember { mutableStateOf("") }
    var parsedReceipt by remember { mutableStateOf<ParsedReceiptData?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Editable fields
    var editMerchant by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Food") }
    var editPaymentMethod by remember { mutableStateOf("UPI") }

    // Photo pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessing = true
            // Run simulated optical character recognition parse on receipt image
            val sampleReceipt = "SARAVANA BHAVAN RESTAURANT\nGSTIN: 33AAAAA0000A1Z5\nDate: 25/09/2026\n1x Special Meals 180.00\n2x Masala Dosa 140.00\n1x Filter Coffee 40.00\nSubtotal: 360.00\nCGST 2.5%: 9.00\nSGST 2.5%: 9.00\nGrand Total: 378.00\nPayment: UPI"
            rawOcrText = sampleReceipt
            val parsed = ReceiptOCRParser.parseReceiptText(sampleReceipt)
            parsedReceipt = parsed
            editMerchant = parsed.merchant
            editAmount = parsed.amount.toString()
            editCategory = parsed.category
            isProcessing = false
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            isProcessing = true
            val sampleReceipt = "HP AUTO FUELS\nReceipt No: 4892\nDate: 25/09/2026\nPetrol Normal 5.2 Litres\nRate: 102.50\nNet Amount: 533.00\nPaid via Cash\nThank You Visit Again"
            rawOcrText = sampleReceipt
            val parsed = ReceiptOCRParser.parseReceiptText(sampleReceipt)
            parsedReceipt = parsed
            editMerchant = parsed.merchant
            editAmount = parsed.amount.toString()
            editCategory = parsed.category
            editPaymentMethod = "Cash"
            isProcessing = false
        }
    }

    val sampleReceiptPresets = listOf(
        "ABC Restaurant Bill (₹850)" to "ABC RESTAURANT\nBill #1029 Date: 20/09/2026\nBiryani x 2  ₹600.00\nStarters x 1 ₹200.00\nGST 5% ₹50.00\nGrand Total: 850.00\nMode: Card",
        "DMart Supermarket (₹1,420)" to "DMART RETAIL\nDate: 22/09/2026\nAtta 5kg ₹280.00\nSunflower Oil 2L ₹340.00\nGroceries & Snacks ₹800.00\nTotal Amount: 1420.00\nPayment: GPay UPI",
        "Apollo Pharmacy (₹490)" to "APOLLO PHARMACY\nInv: 5821 Date: 23/09/2026\nParacetamol ₹60.00\nAntibiotic ₹280.00\nVitamins ₹150.00\nNet Payable: 490.00\nPayment: Cash"
    )

    val categories = listOf(
        "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
        "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Insurance", "EMI", "Subscriptions", "Other"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("receipt_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Receipt Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Camera & Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { cameraLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("take_photo_button")
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Take Photo")
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).testTag("upload_receipt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets for quick emulator test
                Text(
                    text = "Or choose sample receipt text:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleReceiptPresets.forEach { (label, rawText) ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .clickable {
                                    rawOcrText = rawText
                                    val parsed = ReceiptOCRParser.parseReceiptText(rawText)
                                    parsedReceipt = parsed
                                    editMerchant = parsed.merchant
                                    editAmount = parsed.amount.toString()
                                    editCategory = parsed.category
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                if (isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scanning and running OCR parser...", fontSize = 11.sp)
                }

                // OCR Extraction Result
                AnimatedVisibility(visible = parsedReceipt != null) {
                    val result = parsedReceipt
                    if (result != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "OCR Extracted Details:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Merchant: ${result.merchant} | Amount: ₹${result.amount}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Date: ${CurrencyFormatter.formatDate(result.date)} | Category: ${result.category}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = editMerchant,
                                onValueChange = { editMerchant = it },
                                label = { Text("Merchant") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = editAmount,
                                onValueChange = { editAmount = it },
                                label = { Text("Amount (₹)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Category selector
                            var catExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = catExpanded,
                                onExpandedChange = { catExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = editCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = catExpanded,
                                    onDismissRequest = { catExpanded = false }
                                ) {
                                    categories.forEach { c ->
                                        DropdownMenuItem(
                                            text = { Text(c) },
                                            onClick = {
                                                editCategory = c
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }

                                Button(
                                    onClick = {
                                        val amt = editAmount.toDoubleOrNull() ?: 0.0
                                        if (amt > 0) {
                                            val tx = TransactionEntity(
                                                userId = userId,
                                                type = "EXPENSE",
                                                amount = amt,
                                                category = editCategory,
                                                paymentMethod = editPaymentMethod,
                                                sourceOrMerchant = editMerchant.ifBlank { "Receipt Merchant" },
                                                date = result.date,
                                                notes = "Scanned Receipt: $editMerchant",
                                                isReceiptScanned = true,
                                                confidenceScore = result.confidence
                                            )
                                            val receiptScan = ReceiptScanEntity(
                                                userId = userId,
                                                merchant = editMerchant,
                                                amount = amt,
                                                date = result.date,
                                                category = editCategory,
                                                rawText = rawOcrText
                                            )
                                            onReceiptConfirmed(tx, receiptScan)
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1.5f).testTag("save_receipt_button")
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Transaction")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
