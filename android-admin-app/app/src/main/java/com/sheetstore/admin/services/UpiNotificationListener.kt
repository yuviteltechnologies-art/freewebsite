package com.sheetstore.admin.services

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.sheetstore.admin.network.ApiClient
import com.sheetstore.admin.parser.UpiParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpiNotificationListener : NotificationListenerService() {

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
        val isTarget = UPI_PACKAGES.contains(pkg) || pkg.contains("bank") || pkg.contains("upi")
        if (!isTarget) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val combined = "$title $text $bigText".trim()
        Log.d("SheetStoreNotify", "Notification from $pkg: $combined")

        val payment = UpiParser.parseMessage(combined, pkg)
        if (payment != null) {
            val sharedPrefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
            val sheetUrl = sharedPrefs.getString("sheet_web_app_url", "https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec") ?: ""
            val isEnabled = sharedPrefs.getBoolean("auto_verify_enabled", true)

            if (isEnabled && sheetUrl.isNotEmpty()) {
                forwardNotification(sheetUrl, payment.utr, payment.amount, combined)
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
                Log.d("SheetStoreNotify", "Notification forward result: ${response.code()}")
            } catch (e: Exception) {
                Log.e("SheetStoreNotify", "Error forwarding notification", e)
            }
        }
    }
}

