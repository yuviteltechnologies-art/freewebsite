package com.sheetstore.verifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val sharedPrefs = context.getSharedPreferences("SheetStoreConfig", Context.MODE_PRIVATE)
        val isEnabled = sharedPrefs.getBoolean("auto_verify_enabled", true)
        val sheetUrl = sharedPrefs.getString("sheet_web_app_url", "") ?: ""

        if (!isEnabled || sheetUrl.isEmpty()) {
            Log.d("SheetStore", "Auto-verifier disabled or Sheet URL empty")
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val sender = sms.displayOriginatingAddress ?: ""
            val body = sms.displayMessageBody ?: ""

            Log.d("SheetStore", "SMS from: $sender - Body: $body")

            val payment = UpiParser.parseMessage(body, sender)
            if (payment != null) {
                Log.d("SheetStore", "Matched Payment: UTR=${payment.utr}, Amount=${payment.amount}")
                sendToGoogleSheet(sheetUrl, payment.utr, payment.amount, body)
            }
        }
    }

    private fun sendToGoogleSheet(sheetUrl: String, utr: String, amount: String, rawText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val queryParams = "action=bankNotification" +
                        "&utr=" + URLEncoder.encode(utr, "UTF-8") +
                        "&amount=" + URLEncoder.encode(amount, "UTF-8") +
                        "&raw_text=" + URLEncoder.encode(rawText, "UTF-8")

                val targetUrl = if (sheetUrl.contains("?")) "$sheetUrl&$queryParams" else "$sheetUrl?$queryParams"
                val url = URL(targetUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                val code = conn.responseCode
                Log.d("SheetStore", "Google Sheet Response Code: $code")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SheetStore", "Error forwarding SMS to Google Sheet", e)
            }
        }
    }
}

