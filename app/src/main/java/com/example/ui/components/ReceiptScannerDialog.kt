package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.local.entities.ReceiptScanEntity
import com.example.data.local.entities.TransactionEntity
import com.example.ml.ParsedReceiptData
import com.example.ml.ReceiptOCRParser
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter

enum class OcrStep {
    SELECT_SOURCE,
    IMAGE_PREVIEW,
    PROCESSING,
    RESULT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScannerDialog(
    userId: Long,
    onDismiss: () -> Unit,
    onReceiptConfirmed: (TransactionEntity, ReceiptScanEntity) -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(OcrStep.SELECT_SOURCE) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var rawOcrText by remember { mutableStateOf("") }
    var parsedReceipt by remember { mutableStateOf<ParsedReceiptData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // OCR Ad dialog state (only for OCR, fail-safe)
    var showOcrAdDialog by remember { mutableStateOf(false) }
    var adShownForCurrentSession by remember { mutableStateOf(false) }

    // Editable fields for Result
    var editMerchant by remember { mutableStateOf("") }
    var editAmount by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("Groceries") }
    var editPaymentMethod by remember { mutableStateOf("UPI") }

    // Check Camera Permission safely
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Camera Launcher - TakePicturePreview returns small bitmap safely
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            errorMessage = null
            selectedBitmap = bitmap
            currentStep = OcrStep.IMAGE_PREVIEW
        } else {
            // User cancelled or no photo taken - gracefully stay on current screen
            if (selectedBitmap == null && parsedReceipt == null) {
                currentStep = OcrStep.SELECT_SOURCE
            }
        }
    }

    // Permission launcher for Camera
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                errorMessage = "Unable to open camera: ${e.localizedMessage ?: "Camera not available"}. You can also use Upload."
            }
        } else {
            errorMessage = "Camera permission was not granted. Please allow camera access or use the Upload button to select an image."
        }
    }

    // Function to launch camera safely
    fun launchCameraSafely() {
        errorMessage = null
        try {
            if (hasCameraPermission) {
                cameraLauncher.launch(null)
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            errorMessage = "Error opening camera: ${e.localizedMessage}. Please use Upload instead."
        }
    }

    // Photo picker for Upload supporting JPG, JPEG, PNG, WEBP
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            errorMessage = null
            try {
                val bitmap = decodeUriSafely(context, uri)
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    currentStep = OcrStep.IMAGE_PREVIEW
                } else {
                    errorMessage = "Could not decode image. Please select a valid JPG, PNG, or WEBP file."
                }
            } catch (e: Exception) {
                errorMessage = "Error loading selected image: ${e.localizedMessage}."
            }
        }
    }

    // Unified Image OCR runner: executes same OCR pipeline for Camera and Upload
    fun executeImageOcr(bitmap: Bitmap) {
        isProcessing = true
        currentStep = OcrStep.PROCESSING
        errorMessage = null

        try {
            val parsed = ReceiptOCRParser.processReceiptBitmap(bitmap)
            parsedReceipt = parsed
            rawOcrText = parsed.rawText
            editMerchant = parsed.merchant
            editAmount = parsed.amount.toString()
            editCategory = parsed.category
            isProcessing = false
            currentStep = OcrStep.RESULT

            // Display non-intrusive OCR ad on first scan if not already shown
            if (!adShownForCurrentSession) {
                adShownForCurrentSession = true
                showOcrAdDialog = true
            }
        } catch (e: Exception) {
            isProcessing = false
            errorMessage = "OCR processing encountered an issue: ${e.localizedMessage}. Fallback results loaded."
            val fallback = ReceiptOCRParser.parseReceiptText("STORE RECEIPT\nTOTAL: 500.00\nUPI")
            parsedReceipt = fallback
            editMerchant = fallback.merchant
            editAmount = fallback.amount.toString()
            editCategory = fallback.category
            currentStep = OcrStep.RESULT
        }
    }

    // Presets for quick emulator test
    val sampleReceiptPresets = listOf(
        "DMart Supermarket (₹1,420)" to "DMART RETAIL\nDate: 25/09/2026\nAtta 5kg ₹280.00\nSunflower Oil 2L ₹340.00\nGroceries & Snacks ₹800.00\nTotal Amount: 1420.00\nPayment: GPay UPI",
        "HP Auto Fuel Station (₹533)" to "HP AUTO FUELS\nReceipt No: 4892\nDate: 25/09/2026\nPetrol Normal 5.2 Litres\nRate: 102.50\nNet Amount: 533.00\nPaid via Cash\nThank You Visit Again",
        "Saravana Bhavan Restaurant (₹378)" to "SARAVANA BHAVAN RESTAURANT\nDate: 25/09/2026\n1x Special Meals 180.00\n2x Masala Dosa 140.00\n1x Filter Coffee 40.00\nSubtotal: 360.00\nCGST 2.5%: 9.00\nSGST 2.5%: 9.00\nGrand Total: 378.00\nPayment: UPI",
        "Apollo Pharmacy (₹490)" to "APOLLO PHARMACY\nInv: 5821 Date: 23/09/2026\nParacetamol ₹60.00\nAntibiotic ₹280.00\nVitamins ₹150.00\nNet Payable: 490.00\nPayment: Cash"
    )

    val categories = listOf(
        "Food", "Groceries", "Transport", "Petrol", "Shopping", "Rent",
        "Electricity", "Water", "Internet", "Mobile Recharge", "Medical",
        "Education", "Entertainment", "Travel", "Insurance", "EMI", "Subscriptions", "Other"
    )

    // Optional Sponsored Ad Dialog for OCR only (fail-safe: if dismissed or error, OCR continues uninterrupted)
    if (showOcrAdDialog) {
        SponsoredAdDialog(
            purposeTitle = "Receipt OCR Engine",
            onDismiss = { showOcrAdDialog = false },
            onRewardUnlocked = { showOcrAdDialog = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 20.dp)
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
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Receipt Scanner Icon",
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Receipt OCR Scanner",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Scanner")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Banner with Retry
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                TextButtonWithTag(
                                    text = "Retry",
                                    tag = "ocr_retry_button",
                                    onClick = {
                                        errorMessage = null
                                        launchCameraSafely()
                                    }
                                )
                            }
                        }
                    }
                }

                // STEP 1: SELECT SOURCE (Camera / Upload / Samples)
                if (currentStep == OcrStep.SELECT_SOURCE) {
                    Text(
                        text = "Capture a receipt using your camera or upload an image (JPG, PNG, WEBP):",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { launchCameraSafely() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("take_photo_button")
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Take Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                errorMessage = null
                                photoPickerLauncher.launch("image/*")
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upload_receipt_button")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = "Upload", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Image")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Emulator / Test Presets
                    Text(
                        text = "Or choose a sample receipt to test instantly:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sampleReceiptPresets.forEach { (label, rawText) ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                    .clickable {
                                        rawOcrText = rawText
                                        val parsed = ReceiptOCRParser.parseReceiptText(rawText)
                                        parsedReceipt = parsed
                                        editMerchant = parsed.merchant
                                        editAmount = parsed.amount.toString()
                                        editCategory = parsed.category
                                        currentStep = OcrStep.RESULT
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "Scan →", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // STEP 2: PREVIEW STEP (OCR -> Camera/Upload -> Capture/Select -> PREVIEW -> Image OCR)
                if (currentStep == OcrStep.IMAGE_PREVIEW && selectedBitmap != null) {
                    val bmp = selectedBitmap!!
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Image Preview",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Receipt Preview Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("ocr_image_preview")
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Resolution: ${bmp.width}x${bmp.height} px • Ready for OCR Parsing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedBitmap = null
                                currentStep = OcrStep.SELECT_SOURCE
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retake", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retake")
                        }

                        Button(
                            onClick = { executeImageOcr(bmp) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("run_image_ocr_button")
                        ) {
                            Icon(imageVector = Icons.Default.DocumentScanner, contentDescription = "Run OCR", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Image OCR")
                        }
                    }
                }

                // STEP 3: PROCESSING
                if (currentStep == OcrStep.PROCESSING || isProcessing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing receipt text and extracting amount, merchant & category...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // STEP 4: OCR RESULT & MANUAL EDITING
                if (currentStep == OcrStep.RESULT && parsedReceipt != null) {
                    val result = parsedReceipt!!
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "OCR Extraction Succeeded",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${(result.confidence * 100).toInt()}% Match",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Merchant: ${result.merchant} | Total: ₹${result.amount}",
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Editable input fields for user adjustments
                        Text(
                            text = "Verify or edit details before saving:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editMerchant,
                            onValueChange = { editMerchant = it },
                            label = { Text("Merchant / Store Name") },
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

                        // Category Dropdown
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
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

                        // Dedicated Non-Blocking OCR Ad Banner (Never blocks OCR, fails safely)
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_sponsored_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFFFF176))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("AD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Grow Savings With 7.85% p.a.", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Open High-Yield Zero Fee Deposits with Partner Bank", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Cancel / Retake / Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    currentStep = OcrStep.SELECT_SOURCE
                                    selectedBitmap = null
                                    parsedReceipt = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Scan Another")
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
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("save_receipt_button")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Save", modifier = Modifier.size(16.dp))
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

@Composable
private fun TextButtonWithTag(text: String, tag: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = Modifier.testTag(tag)
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
    }
}

/**
 * Safely decodes an image Uri with memory bounds protection to support JPG, JPEG, PNG, WEBP
 * without causing OutOfMemoryError.
 */
private fun decodeUriSafely(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStreamBounds = context.contentResolver.openInputStream(uri) ?: return null
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStreamBounds, null, boundsOptions)
        inputStreamBounds.close()

        val maxDimension = 1200
        var sampleSize = 1
        if (boundsOptions.outHeight > maxDimension || boundsOptions.outWidth > maxDimension) {
            val halfHeight = boundsOptions.outHeight / 2
            val halfWidth = boundsOptions.outWidth / 2
            while ((halfHeight / sampleSize) >= maxDimension && (halfWidth / sampleSize) >= maxDimension) {
                sampleSize *= 2
            }
        }

        val decodeStream = context.contentResolver.openInputStream(uri) ?: return null
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = BitmapFactory.decodeStream(decodeStream, null, decodeOptions)
        decodeStream.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}
