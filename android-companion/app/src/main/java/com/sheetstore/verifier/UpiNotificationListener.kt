package com.sheetstore.verifier

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class UpiNotificationListener : NotificationListenerService() {

    // Package names of popular UPI apps in India
    private val UPI_PACKAGES = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                       // PhonePe
        "net.one97.paytm",                       // Paytm
        "in.org.npci.upiapp",                    // BHIM
        "com.whatsapp",                          // WhatsApp Pay
        "com.dreamplug.androidapp"               // CRED
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: ""
        // Check if notification is from a UPI app or Bank app (containing 'bank', 'upi', 'pay')
        val isTargetApp = UPI_PACKAGES.contains(pkg) || pkg.contains("bank") || pkg.contains("upi")
        if (!isTargetApp) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combinedText = "$title $text $bigText".trim()
        Log.d("SheetStoreNotify", "Notification from $pkg: $combinedText")

        val payment = UpiParser.parseMessage(combinedText, pkg)
        if (payment != null) {
            val sharedPrefs = getSharedPreferences("SheetStoreConfig", Context.MODE_PRIVATE)
            val sheetUrl = sharedPrefs.getString("sheet_web_app_url", "") ?: ""
            val isEnabled = sharedPrefs.getBoolean("auto_verify_enabled", true)

            if (isEnabled && sheetUrl.isNotEmpty()) {
                forwardToSheet(sheetUrl, payment.utr, payment.amount, combinedText)
            }
        }
    }

    private fun forwardToSheet(sheetUrl: String, utr: String, amount: String, rawText: String) {
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
                Log.d("SheetStoreNotify", "Forwarded Notification to Sheet - Response: $code")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SheetStoreNotify", "Failed forwarding notification", e)
            }
        }
    }
}

