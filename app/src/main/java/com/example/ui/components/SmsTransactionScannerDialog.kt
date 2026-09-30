package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
        // Patterns: Rs. 450.00, INR 1250, Rs 500, ₹350
        val amountRegex = Pattern.compile("(?i)(?:rs\\.?|inr|₹)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
        var amount = 0.0
        val amtMatcher = amountRegex.matcher(clean)
        if (amtMatcher.find()) {
            val amtStr = amtMatcher.group(1)?.replace(",", "") ?: "0"
            amount = amtStr.toDoubleOrNull() ?: 0.0
        } else {
            // Secondary match: e.g. 500.00 debited
            val secondaryAmt = Pattern.compile("([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:debited|credited|spent|paid)", Pattern.CASE_INSENSITIVE)
            val secMatcher = secondaryAmt.matcher(clean)
            if (secMatcher.find()) {
                val amtStr = secMatcher.group(1)?.replace(",", "") ?: "0"
                amount = amtStr.toDoubleOrNull() ?: 0.0
            }
        }

        if (amount <= 0.0) {
            // Try any standalone number
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
            // Check well-known merchants
            val wellKnown = listOf(
                "Swiggy", "Zomato", "Amazon", "Flipkart", "DMart", "Uber", "Ola",
                "Blinkit", "Zepto", "Instamart", "Netflix", "Jio", "Airtel", "TNEB", "Petrol"
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
                bankName = "$b Bank"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsTransactionScannerDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onConfirmSave: (TransactionEntity) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var inputSmsText by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedSmsTransaction?>(null) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Editable fields for confirmation
    var confirmedAmount by remember { mutableStateOf("") }
    var confirmedType by remember { mutableStateOf("EXPENSE") }
    var confirmedMerchant by remember { mutableStateOf("") }
    var confirmedCategory by remember { mutableStateOf("Food") }
    var confirmedPaymentMethod by remember { mutableStateOf("UPI") }

    val sampleSmsList = listOf(
        "Dear Customer, Rs.450.00 debited from A/C **1234 on 30-Sep-26 to SWIGGY. Ref No: 429182. - HDFC Bank",
        "Sent Rs. 650.00 from Kotak Bank to HP FUEL STATION via UPI Ref 928371982",
        "Salary of Rs. 65,000.00 credited to your A/C **5678 on 30-Sep-26 by INFOSYS LTD. - SBI",
        "Paid Rs. 1,850.00 at DMART SUPERMARKET using ICICI Card ending 9012. Bal: Rs. 42,100",
        "Rs. 1,450.00 received from Ramesh Kumar via PhonePe UPI Ref: 382910"
    )

    fun runSmsParse(text: String) {
        val res = SmsParserEngine.parseBankSms(text)
        if (res != null) {
            parsedResult = res
            confirmedAmount = if (res.amount > 0) res.amount.toString() else ""
            confirmedType = res.type
            confirmedMerchant = res.merchant
            confirmedCategory = res.category
            confirmedPaymentMethod = res.paymentMethod
        }
    }

    val availableCategories = listOf(
        "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
        "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Salary", "Freelance", "Other"
    )

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
                    .verticalScroll(rememberScrollState())
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
                                text = "On-Demand SMS Reader",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Paste bank SMS or pick template to extract",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input SMS Text Area with Paste button
                OutlinedTextField(
                    value = inputSmsText,
                    onValueChange = {
                        inputSmsText = it
                        if (it.length > 10) runSmsParse(it)
                    },
                    label = { Text("Bank / Payment SMS Text") },
                    placeholder = { Text("Paste any bank SMS (e.g. Rs 450 debited to Swiggy)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("sms_input_field"),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    inputSmsText = clip
                                    runSmsParse(clip)
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = EmeraldPrimary)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scan / Parse Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { runSmsParse(inputSmsText) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).testTag("parse_sms_button"),
                        enabled = inputSmsText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract Transaction Details")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Sample Bank SMS Templates
                Text("Or try a sample bank SMS:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sampleSmsList.forEachIndexed { idx, sample ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inputSmsText = sample
                                    runSmsParse(sample)
                                }
                                .testTag("sample_sms_item_$idx")
                        ) {
                            Text(
                                text = sample,
                                fontSize = 11.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Extracted Live Result Card
                if (parsedResult != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Extracted Transaction Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Type Selector (Debit vs Credit)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = confirmedType == "EXPENSE",
                                    onClick = { confirmedType = "EXPENSE" },
                                    label = { Text("🔴 Debit / Expense") },
                                    modifier = Modifier.weight(1f).testTag("sms_type_expense")
                                )
                                FilterChip(
                                    selected = confirmedType == "INCOME",
                                    onClick = { confirmedType = "INCOME" },
                                    label = { Text("🟢 Credit / Income") },
                                    modifier = Modifier.weight(1f).testTag("sms_type_income")
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = confirmedAmount,
                                    onValueChange = { confirmedAmount = it },
                                    label = { Text("Amount (₹) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("sms_amount_field")
                                )

                                OutlinedTextField(
                                    value = confirmedPaymentMethod,
                                    onValueChange = { confirmedPaymentMethod = it },
                                    label = { Text("Mode") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = confirmedMerchant,
                                onValueChange = { confirmedMerchant = it },
                                label = { Text("Merchant / Source *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("sms_merchant_field")
                            )

                            // Category Dropdown
                            ExposedDropdownMenuBox(
                                expanded = categoryDropdownExpanded,
                                onExpandedChange = { categoryDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = confirmedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = categoryDropdownExpanded,
                                    onDismissRequest = { categoryDropdownExpanded = false }
                                ) {
                                    availableCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                confirmedCategory = cat
                                                categoryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Bank Details Badge
                            if (parsedResult!!.bankName.isNotBlank() || parsedResult!!.accountLastDigits.isNotBlank()) {
                                Text(
                                    text = "🏦 ${parsedResult!!.bankName} ${if (parsedResult!!.accountLastDigits.isNotBlank()) "A/C **" + parsedResult!!.accountLastDigits else ""} ${if (parsedResult!!.referenceNumber.isNotBlank()) "• Ref: " + parsedResult!!.referenceNumber else ""}",
                                    fontSize = 11.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Confirmation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val amt = confirmedAmount.toDoubleOrNull() ?: 0.0
                                if (amt > 0 && confirmedMerchant.isNotBlank()) {
                                    val tx = TransactionEntity(
                                        userId = userId,
                                        type = confirmedType,
                                        amount = amt,
                                        category = confirmedCategory,
                                        paymentMethod = confirmedPaymentMethod,
                                        sourceOrMerchant = confirmedMerchant.trim(),
                                        notes = "Extracted from SMS: ${parsedResult!!.bankName}",
                                        date = System.currentTimeMillis()
                                    )
                                    onConfirmSave(tx)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("save_sms_tx_button")
                        ) {
                            Text("Save Entry")
                        }
                    }
                }
            }
        }
    }
}
