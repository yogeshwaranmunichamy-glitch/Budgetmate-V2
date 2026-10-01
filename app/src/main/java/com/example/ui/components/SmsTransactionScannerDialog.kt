package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.TransactionEntity
import com.example.ml.MultilingualNLP
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.util.CurrencyFormatter
import com.example.util.DeviceSmsItem
import com.example.util.DeviceSmsReader
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsTransaction(
    val amount: Double,
    val type: String, // "EXPENSE" (Debit) or "INCOME" (Credit)
    val merchant: String,
    val category: String,
    val paymentMethod: String,
    val bankName: String,
    val accountLastDigits: String,
    val referenceNumber: String,
    val rawSms: String
)

object SmsParserEngine {

    fun parseBankSms(smsText: String): ParsedSmsTransaction? {
        val clean = smsText.trim()
        if (clean.isBlank()) return null

        val lower = clean.lowercase(Locale.ROOT)

        // 1. Determine Type: Debit vs Credit
        val isCredit = lower.contains("credited") || lower.contains("received") ||
                lower.contains("deposit") || lower.contains("salary") || lower.contains("added")
        val isDebit = lower.contains("debited") || lower.contains("spent") ||
                lower.contains("paid") || lower.contains("sent") || lower.contains("withdrawn") || lower.contains("txn")

        val type = if (isCredit && !isDebit) "INCOME" else "EXPENSE"

        // 2. Extract Amount
        val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        var amount = 0.0
        val amtMatcher = amountRegex.matcher(clean)
        if (amtMatcher.find()) {
            val amtStr = amtMatcher.group(1)?.replace(",", "") ?: "0"
            amount = amtStr.toDoubleOrNull() ?: 0.0
        } else {
            val secondaryAmt = Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|spent|paid)", Pattern.CASE_INSENSITIVE)
            val secMatcher = secondaryAmt.matcher(clean)
            if (secMatcher.find()) {
                val amtStr = secMatcher.group(1)?.replace(",", "") ?: "0"
                amount = amtStr.toDoubleOrNull() ?: 0.0
            }
        }

        if (amount <= 0.0) {
            val numRegex = Pattern.compile("\\b(\\d{2,6}(?:\\.\\d{1,2})?)\\b")
            val numMatcher = numRegex.matcher(clean)
            if (numMatcher.find()) {
                amount = numMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            }
        }

        // 3. Extract Merchant / Beneficiary
        var merchant = ""
        val toMatcher = Pattern.compile("(?i)(?:to|at|vpa|info|beneficiary|for)\\s+([A-Za-z0-9&\\s]{2,30}?)(?:\\.|\\s+via|\\s+ref|\\s+on|\\s+using|\\s+vpa|\\s+bal|\\s+avl|$)", Pattern.CASE_INSENSITIVE).matcher(clean)
        if (toMatcher.find()) {
            val candidate = toMatcher.group(1)?.trim() ?: ""
            if (!candidate.contains("your", ignoreCase = true) && !candidate.contains("bank", ignoreCase = true) && candidate.length > 2) {
                merchant = candidate
            }
        }

        if (merchant.isBlank()) {
            val fromMatcher = Pattern.compile("(?i)(?:from)\\s+([A-Za-z0-9&\\s]{2,30}?)(?:\\.|\\s+via|\\s+ref|\\s+on|\\s+bal|\\s+avl|$)", Pattern.CASE_INSENSITIVE).matcher(clean)
            if (fromMatcher.find()) {
                val candidate = fromMatcher.group(1)?.trim() ?: ""
                if (!candidate.contains("your", ignoreCase = true) && candidate.length > 2) {
                    merchant = candidate
                }
            }
        }

        if (merchant.isBlank()) {
            val wellKnown = listOf(
                "Swiggy", "Zomato", "Amazon", "Flipkart", "DMart", "Uber", "Ola",
                "Blinkit", "Zepto", "Instamart", "Netflix", "Jio", "Airtel", "TNEB", "Petrol",
                "HP Fuel", "Indian Oil", "Apollo Pharmacy", "Chai Point"
            )
            for (w in wellKnown) {
                if (lower.contains(w.lowercase(Locale.ROOT))) {
                    merchant = w
                    break
                }
            }
        }

        if (merchant.isBlank()) {
            merchant = if (type == "INCOME") "Bank Deposit / Transfer" else "Merchant / Store"
        }

        // 4. Extract Bank Name
        val banks = listOf("HDFC", "SBI", "ICICI", "Axis", "Kotak", "PNB", "Canara", "Bank of Baroda", "GPay", "PhonePe", "Paytm", "CRED", "IndusInd")
        var bankName = "Bank"
        for (b in banks) {
            if (lower.contains(b.lowercase(Locale.ROOT))) {
                bankName = "$b"
                break
            }
        }

        // 5. Account Last 4 Digits
        val acctMatcher = Pattern.compile("(?i)(?:a/c|acct|card|ending)\\s*(?:no\\.?)?\\s*[*xX]*([0-9]{3,4})").matcher(clean)
        val acctDigits = if (acctMatcher.find()) acctMatcher.group(1) ?: "" else ""

        // 6. Payment Method
        val paymentMethod = when {
            lower.contains("upi") || lower.contains("vpa") || lower.contains("gpay") || lower.contains("phonepe") -> "UPI"
            lower.contains("credit card") -> "Credit Card"
            lower.contains("debit card") || lower.contains("card") -> "Debit Card"
            lower.contains("netbanking") || lower.contains("neft") || lower.contains("rtgs") || lower.contains("imps") -> "Bank Transfer"
            lower.contains("atm") || lower.contains("cash") -> "Cash"
            else -> "UPI"
        }

        // 7. Reference Number
        val refMatcher = Pattern.compile("(?i)(?:ref|rrn|txn|utr)\\s*(?:no\\.?)?\\s*:?\\s*([A-Za-z0-9]{6,16})").matcher(clean)
        val refNo = if (refMatcher.find()) refMatcher.group(1) ?: "" else ""

        // 8. Auto-Categorization
        val merchantCategory = if (merchant.isNotBlank()) MultilingualNLP.classifyCategory(merchant, type == "INCOME") else ""
        val category = if (merchantCategory.isNotBlank() && merchantCategory != "Other") {
            merchantCategory
        } else {
            val textCategory = MultilingualNLP.classifyCategory(clean, type == "INCOME")
            if (textCategory.isNotBlank() && textCategory != "Other") {
                textCategory
            } else {
                if (type == "INCOME") "Salary" else "Shopping"
            }
        }

        return ParsedSmsTransaction(
            amount = amount,
            type = type,
            merchant = merchant.trim(),
            category = category,
            paymentMethod = paymentMethod,
            bankName = bankName,
            accountLastDigits = acctDigits,
            referenceNumber = refNo,
            rawSms = clean
        )
    }
}

@Composable
fun SmsTransactionScannerDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onConfirmSave: (TransactionEntity) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(DeviceSmsReader.hasSmsPermission(context)) }
    var deviceSmsList by remember { mutableStateOf<List<DeviceSmsItem>>(emptyList()) }
    var isLoadingSms by remember { mutableStateOf(false) }
    var importedIds by remember { mutableStateOf(setOf<Long>()) }
    var expandedSmsId by remember { mutableStateOf<Long?>(null) }
    var showManualPaste by remember { mutableStateOf(false) }
    var manualSmsText by remember { mutableStateOf("") }

    fun refreshFromDevice() {
        isLoadingSms = true
        val result = DeviceSmsReader.readFinancialSmsFromDevice(context)
        if (result.isNotEmpty()) {
            deviceSmsList = result
        } else {
            // If device has 0 SMS (e.g. running inside browser/emulator), load test list
            deviceSmsList = DeviceSmsReader.getEmulatorTestSmsList()
        }
        isLoadingSms = false
    }

    // Permission launcher for dangerous permission READ_SMS
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            refreshFromDevice()
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            refreshFromDevice()
        }
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
                .fillMaxSize(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Device SMS Connect",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasPermission) "Direct inbox reader • ${deviceSmsList.size} financial SMS"
                                else "Connect to read transactions automatically",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Permission State Card if not granted
                if (!hasPermission) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Direct Device SMS Access",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "BudgetMate reads financial transaction SMS from your bank (HDFC, SBI, ICICI, Axis, UPI) directly from your device inbox. No manual copy-pasting required.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_grant_sms_permission")
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Connect Device SMS Inbox", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action Bar when Connected
                if (hasPermission) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Refresh button
                        OutlinedButton(
                            onClick = { refreshFromDevice() },
                            modifier = Modifier.height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh Inbox", fontSize = 12.sp)
                        }

                        // Import All button
                        val unimportedList = deviceSmsList.filter { it.id !in importedIds }
                        Button(
                            onClick = {
                                unimportedList.forEach { item ->
                                    val tx = TransactionEntity(
                                        userId = userId,
                                        type = item.parsedTransaction.type,
                                        amount = item.parsedTransaction.amount,
                                        category = item.parsedTransaction.category,
                                        paymentMethod = item.parsedTransaction.paymentMethod,
                                        sourceOrMerchant = item.parsedTransaction.merchant,
                                        date = item.date,
                                        notes = "Imported from SMS (${item.sender}) Ref: ${item.parsedTransaction.referenceNumber}",
                                        isReceiptScanned = false,
                                        isVoiceEntered = false,
                                        confidenceScore = 0.95f
                                    )
                                    onConfirmSave(tx)
                                }
                                importedIds = importedIds + unimportedList.map { it.id }.toSet()
                            },
                            enabled = unimportedList.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_import_all_sms"),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (unimportedList.isEmpty()) "All Imported" else "Import All (${unimportedList.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // List of Detected Device Financial Transactions
                if (isLoadingSms) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                } else if (deviceSmsList.isEmpty() && hasPermission) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No bank transaction SMS found in device inbox", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    deviceSmsList = DeviceSmsReader.getEmulatorTestSmsList()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Load Bank SMS for Preview")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(deviceSmsList) { smsItem ->
                            val isImported = smsItem.id in importedIds
                            val parsed = smsItem.parsedTransaction

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isImported) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("device_sms_card_${smsItem.id}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Sender & Date
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(smsItem.sender, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                            }
                                        }

                                        Text(
                                            text = CurrencyFormatter.formatShortDate(smsItem.date),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Row 2: Merchant, Category, and Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = parsed.merchant,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${parsed.category} • ${parsed.paymentMethod}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${if (parsed.type == "INCOME") "+" else "-"}${CurrencyFormatter.formatINR(parsed.amount)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (parsed.type == "INCOME") IncomeGreen else ExpenseRed
                                            )
                                            Text(
                                                text = if (parsed.type == "INCOME") "Credit (Received)" else "Debit (Spent)",
                                                fontSize = 9.sp,
                                                color = if (parsed.type == "INCOME") IncomeGreen else ExpenseRed
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Row 3: Action Buttons (Add to Ledger / View Raw SMS)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (expandedSmsId == smsItem.id) "Hide SMS Text ▲" else "View SMS Text ▼",
                                            fontSize = 11.sp,
                                            color = EmeraldPrimary,
                                            modifier = Modifier.clickable {
                                                expandedSmsId = if (expandedSmsId == smsItem.id) null else smsItem.id
                                            }
                                        )

                                        if (isImported) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = IncomeGreen.copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Imported ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                                                }
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    val tx = TransactionEntity(
                                                        userId = userId,
                                                        type = parsed.type,
                                                        amount = parsed.amount,
                                                        category = parsed.category,
                                                        paymentMethod = parsed.paymentMethod,
                                                        sourceOrMerchant = parsed.merchant,
                                                        date = smsItem.date,
                                                        notes = "SMS Auto-Import (${smsItem.sender}) Ref: ${parsed.referenceNumber}",
                                                        isReceiptScanned = false,
                                                        isVoiceEntered = false,
                                                        confidenceScore = 0.95f
                                                    )
                                                    onConfirmSave(tx)
                                                    importedIds = importedIds + smsItem.id
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("btn_import_sms_${smsItem.id}"),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add to Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Expandable Raw SMS Body
                                    if (expandedSmsId == smsItem.id) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(8.dp)
                                        ) {
                                            Text(smsItem.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (importedIds.isNotEmpty()) "Done (${importedIds.size} added)" else "Close")
                    }
                }
            }
        }
    }
}
