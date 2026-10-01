package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.ui.components.ParsedSmsTransaction
import com.example.ui.components.SmsParserEngine

data class DeviceSmsItem(
    val id: Long,
    val sender: String,
    val body: String,
    val date: Long,
    val parsedTransaction: ParsedSmsTransaction
)

object DeviceSmsReader {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Directly queries the Android Telephony SMS content provider on the device
     * and filters for financial transaction messages (debit/credit/salary/UPI/bank).
     */
    fun readFinancialSmsFromDevice(context: Context, limit: Int = 100): List<DeviceSmsItem> {
        val list = mutableListOf<DeviceSmsItem>()
        if (!hasSmsPermission(context)) {
            return list
        }

        try {
            val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val sortOrder = "${Telephony.Sms.DATE} DESC"

            context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndex(Telephony.Sms._ID)
                val addressCol = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyCol = cursor.getColumnIndex(Telephony.Sms.BODY)
                val dateCol = cursor.getColumnIndex(Telephony.Sms.DATE)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = if (idCol >= 0) cursor.getLong(idCol) else count.toLong()
                    val sender = if (addressCol >= 0) cursor.getString(addressCol) ?: "SMS" else "SMS"
                    val body = if (bodyCol >= 0) cursor.getString(bodyCol) ?: "" else ""
                    val date = if (dateCol >= 0) cursor.getLong(dateCol) else System.currentTimeMillis()

                    val parsed = SmsParserEngine.parseBankSms(body)
                    if (parsed != null && parsed.amount > 0) {
                        list.add(
                            DeviceSmsItem(
                                id = id,
                                sender = sender,
                                body = body,
                                date = date,
                                parsedTransaction = parsed
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    /**
     * Fallback simulated bank SMS for testing on Android Emulators or devices
     * without a SIM card.
     */
    fun getEmulatorTestSmsList(): List<DeviceSmsItem> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60 * 60 * 1000

        val samples = listOf(
            Triple("HDFC-BANK", "Dear Customer, Rs. 1,450.00 debited from A/C **1234 on 01-Oct-26 to SWIGGY. UPI Ref: 829102. Bal: Rs. 45,210.00", now - 2 * 3600 * 1000),
            Triple("SBI-INB", "Salary of Rs. 75,000.00 credited to your A/C **8821 on 30-Sep-26 by TECH CORP PVT LTD. - SBI", now - dayMs),
            Triple("AXIS-BK", "Paid Rs. 2,350.00 at DMART SUPERMARKET using Axis Bank UPI Ref 901283. Available Bal: Rs. 18,500.00", now - 2 * dayMs),
            Triple("ICICI-TXN", "Sent Rs. 650.00 from ICICI A/C to HP FUEL STATION via UPI Ref 718293. Txn successful.", now - 3 * dayMs),
            Triple("KOTAK-BANK", "Rs. 3,500.00 received from Rahul Verma via PhonePe UPI Ref: 391029.", now - 4 * dayMs),
            Triple("PAYTM-UPI", "Paid Rs. 180.00 to CHAI POINT via Paytm UPI. Transaction ID: PYTM829101.", now - 5 * dayMs),
            Triple("INDUSIND", "Rs. 4,200.00 debited for APPOLLO PHARMACY using Debit Card ending 4412.", now - 6 * dayMs)
        )

        return samples.mapIndexed { index, (sender, body, date) ->
            val parsed = SmsParserEngine.parseBankSms(body) ?: ParsedSmsTransaction(
                amount = 100.0,
                type = "EXPENSE",
                merchant = "Merchant",
                category = "Food",
                paymentMethod = "UPI",
                bankName = sender,
                accountLastDigits = "",
                referenceNumber = "",
                rawSms = body
            )
            DeviceSmsItem(
                id = 1000L + index,
                sender = sender,
                body = body,
                date = date,
                parsedTransaction = parsed
            )
        }
    }
}
