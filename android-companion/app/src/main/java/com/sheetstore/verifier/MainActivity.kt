package com.sheetstore.verifier

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup clean programmatic UI (Zero XML dependency needed for compilation)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
            setBackgroundColor(0xFF0F172A.toInt())
        }

        val titleView = TextView(this).apply {
            text = "⚡ SheetStore Pro Auto-Verifier"
            textSize = 20f
            setTextColor(0xFF38BDF8.toInt())
            setPadding(0, 0, 0, 16)
        }
        layout.addView(titleView)

        val descView = TextView(this).apply {
            text = "Ye app Bank SMS aur PhonePe/GPay notifications se 12-digit UTR padhkar Google Sheet me automatically verify karti hai."
            textSize = 13f
            setTextColor(0xFF94A3B8.toInt())
            setPadding(0, 0, 0, 36)
        }
        layout.addView(descView)

        // Preferences
        val sharedPrefs = getSharedPreferences("SheetStoreConfig", Context.MODE_PRIVATE)

        val urlLabel = TextView(this).apply {
            text = "Google Sheet Apps Script Web App URL:"
            textSize = 12f
            setTextColor(0xFFE2E8F0.toInt())
            setPadding(0, 0, 0, 8)
        }
        layout.addView(urlLabel)

        val urlInput = EditText(this).apply {
            hint = "https://script.google.com/macros/s/.../exec"
            setText(sharedPrefs.getString("sheet_web_app_url", ""))
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF64748B.toInt())
            setBackgroundColor(0xFF1E293B.toInt())
            setPadding(32, 28, 32, 28)
        }
        layout.addView(urlInput)

        val saveBtn = Button(this).apply {
            text = "💾 Save Configuration"
            setBackgroundColor(0xFF2563EB.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                val input = urlInput.text.toString().trim()
                sharedPrefs.edit().putString("sheet_web_app_url", input).apply()
                Toast.makeText(this@MainActivity, "Saved successfully!", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(saveBtn)

        val permissionBtn = Button(this).apply {
            text = "🔔 Enable Notification Access"
            setBackgroundColor(0xFF334155.toInt())
            setTextColor(0xFF38BDF8.toInt())
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        layout.addView(permissionBtn)

        val statusView = TextView(this).apply {
            text = "Status: Listener Running in Background"
            textSize = 12f
            setTextColor(0xFF10B981.toInt())
            setPadding(0, 32, 0, 0)
        }
        layout.addView(statusView)

        setContentView(layout)
    }
}

