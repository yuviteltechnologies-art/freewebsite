package com.sheetstore.admin.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.sheetstore.admin.network.ApiClient
import com.sheetstore.admin.parser.UpiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val sharedPrefs = context.getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("auto_verify_enabled", true)
        val sheetUrl = sharedPrefs.getString("sheet_web_app_url", "https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec") ?: ""

        if (!isEnabled || sheetUrl.isEmpty()) {
            Log.d("SheetStoreAdmin", "Auto-verify disabled or Sheet URL empty")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: ""
            val body = sms.displayMessageBody ?: ""

            Log.d("SheetStoreAdmin", "Bank SMS: $sender -> $body")

            val payment = UpiParser.parseMessage(body, sender)
            if (payment != null) {
                Log.d("SheetStoreAdmin", "Auto-Captured UPI Payment: UTR=${payment.utr}, Amount=${payment.amount}")
                forwardNotification(sheetUrl, payment.utr, payment.amount, body)
            }
        }
    }

    private fun forwardNotification(sheetUrl: String, utr: String, amount: String, rawText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.sendBankNotification(
                    fullUrl = sheetUrl,
                    rawText = rawText,
                    utr = utr,
                    amount = amount
                )
                val response = call.execute()
                Log.d("SheetStoreAdmin", "Bank Notification Forward Response: ${response.code()}")
            } catch (e: Exception) {
                Log.e("SheetStoreAdmin", "Failed to forward SMS to Sheet", e)
            }
        }
    }
}

