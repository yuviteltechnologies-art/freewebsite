package com.sheetstore.admin.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sheetstore.admin.models.DashboardResponse
import com.sheetstore.admin.models.Order
import com.sheetstore.admin.models.Product
import com.sheetstore.admin.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : Activity() {

    private lateinit var contentContainer: FrameLayout
    private lateinit var navBar: LinearLayout

    private var currentTab = 0 // 0: Orders, 1: Website, 2: Products
    private var dashboardData: DashboardResponse? = null

    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var productsAdapter: ProductsAdapter

    // Views in Website Customizer
    private lateinit var brandNameInput: EditText
    private lateinit var taglineInput: EditText
    private lateinit var waNumberInput: EditText
    private lateinit var upiIdInput: EditText
    private lateinit var announcementInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root Layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
        }

        // 1. Top App Bar
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(40, 48, 40, 24)
            setBackgroundColor(Color.parseColor("#1E293B"))
        }

        val appTitle = TextView(this).apply {
            text = "⚡ SheetStore Pro Admin"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topBar.addView(appTitle)

        // Sheet URL Settings Button
        val settingsBtn = Button(this).apply {
            text = "⚙️ Sheet URL"
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
            setBackgroundColor(Color.parseColor("#0F172A"))
            setOnClickListener { showSheetUrlDialog() }
        }
        topBar.addView(settingsBtn)
        root.addView(topBar)

        // 2. Main Content Container
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(contentContainer)

        // 3. Bottom Navigation Bar
        navBar = createBottomNav()
        root.addView(navBar)

        setContentView(root)

        // Check Permissions on first start
        checkPermissions()

        // Load Tab 0 (Orders)
        switchTab(0)

        // Check if Sheet URL is configured, else prompt user
        val sheetUrl = getSheetUrl()
        if (sheetUrl.isEmpty()) {
            showSheetUrlDialog()
        } else {
            fetchDashboard()
        }
    }

    private fun getSheetUrl(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("sheet_web_app_url", "https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec") ?: "https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec"
    }

    private fun saveSheetUrl(url: String) {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("sheet_web_app_url", url).apply()
    }

    private fun showSheetUrlDialog() {
        val input = EditText(this).apply {
            hint = "https://script.google.com/macros/s/.../exec"
            setText(getSheetUrl())
            setTextColor(Color.BLACK)
            setPadding(32, 24, 32, 24)
        }

        AlertDialog.Builder(this)
            .setTitle("Google Sheet Web App URL")
            .setMessage("Google Sheet me Code.gs deploy karke mila hua Web App URL yahan paste karein:")
            .setView(input)
            .setPositiveButton("Save & Connect") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    saveSheetUrl(url)
                    Toast.makeText(this, "URL Saved! Fetching data...", Toast.LENGTH_SHORT).show()
                    fetchDashboard()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun fetchDashboard() {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val callUrl = if (url.contains("?")) "$url&action=getAdminDashboard" else "$url?action=getAdminDashboard"
                val call = ApiClient.service.getAdminDashboard(callUrl)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        dashboardData = response.body()
                        refreshCurrentTab()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to connect to Google Sheet", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        contentContainer.removeAllViews()

        when (tabIndex) {
            0 -> contentContainer.addView(createOrdersTab())
            1 -> contentContainer.addView(createWebsiteCustomizerTab())
            2 -> contentContainer.addView(createProductsTab())
        }
        updateNavSelection()
    }

    private fun refreshCurrentTab() {
        when (currentTab) {
            0 -> {
                val orders = dashboardData?.orders ?: emptyList()
                ordersAdapter.updateData(orders)
                updateStatsCard()
            }
            1 -> populateSettingsFields()
            2 -> {
                val products = dashboardData?.products ?: emptyList()
                productsAdapter.updateData(products)
            }
        }
    }

    // ==========================================
    // TAB 1: ORDERS & REVENUE DASHBOARD
    // ==========================================
    private var statsRevenueView: TextView? = null
    private var statsOrdersView: TextView? = null

    private fun createOrdersTab(): View {
        val swipeLayout = SwipeRefreshLayout(this)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        // Stats Card
        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(36, 36, 36, 36)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val statsTitle = TextView(this).apply {
            text = "💰 REVENUE & ORDERS STATS"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
        }
        statsCard.addView(statsTitle)

        statsRevenueView = TextView(this).apply {
            text = "₹0.00"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, 8, 0, 4)
        }
        statsCard.addView(statsRevenueView)

        statsOrdersView = TextView(this).apply {
            text = "Total Orders: 0 (Paid: 0 • Pending: 0)"
            textSize = 12f
            setTextColor(Color.parseColor("#E2E8F0"))
        }
        statsCard.addView(statsOrdersView)
        scrollContent.addView(statsCard)

        // Section Title
        val sectionTitle = TextView(this).apply {
            text = "Live Incoming Orders"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 16)
        }
        scrollContent.addView(sectionTitle)

        // RecyclerView
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        ordersAdapter = OrdersAdapter(dashboardData?.orders ?: emptyList()) { order ->
            approveOrder(order)
        }
        rv.adapter = ordersAdapter
        scrollContent.addView(rv)

        swipeLayout.addView(scrollContent)
        swipeLayout.setOnRefreshListener {
            fetchDashboard()
            swipeLayout.isRefreshing = false
        }

        updateStatsCard()
        return swipeLayout
    }

    private fun updateStatsCard() {
        val stats = dashboardData?.stats ?: return
        statsRevenueView?.text = "₹${stats.totalRevenue.toInt()}"
        statsOrdersView?.text = "Total: ${stats.totalOrders} Orders (Paid: ${stats.paidOrders} • Pending: ${stats.pendingOrders})"
    }

    private fun approveOrder(order: Order) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        Toast.makeText(this, "Approving ${order.orderId}...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.approveOrder(
                    fullUrl = url,
                    orderId = order.orderId
                )
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        order.status = "PAID"
                        order.verified = true
                        ordersAdapter.notifyDataSetChanged()
                        Toast.makeText(this@MainActivity, "Order ${order.orderId} Approved! ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Approval failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // TAB 2: WEBSITE CUSTOMIZER (NAME, PHONE, UPI)
    // ==========================================
    private fun createWebsiteCustomizerTab(): View {
        val scroll = ScrollView(this)
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 36, 36, 48)
        }

        val heading = TextView(this).apply {
            text = "🌐 Website Live Settings"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }
        form.addView(heading)

        val subtext = TextView(this).apply {
            text = "Yahan se website ka naam, headline, WhatsApp aur UPI ID live change karein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 32)
        }
        form.addView(subtext)

        // Store Name
        form.addView(createLabel("Website Ka Naam (Brand Name):"))
        brandNameInput = createInput("SheetStore Pro")
        form.addView(brandNameInput)

        // Tagline
        form.addView(createLabel("Brand Tagline:"))
        taglineInput = createInput("Zero Hosting & Domain E-Commerce")
        form.addView(taglineInput)

        // WhatsApp Number
        form.addView(createLabel("WhatsApp Order Number (with 91):"))
        waNumberInput = createInput("919999999999")
        form.addView(waNumberInput)

        // UPI ID
        form.addView(createLabel("UPI ID (Payments Received Here):"))
        upiIdInput = createInput("yourname@okaxis")
        form.addView(upiIdInput)

        // Announcement Bar
        form.addView(createLabel("Top Announcement Bar Text:"))
        announcementInput = createInput("Special Discount Today! 80% OFF")
        form.addView(announcementInput)

        // Save Button
        val saveBtn = Button(this).apply {
            text = "Update Website Live 🚀"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setPadding(0, 28, 0, 28)
            setOnClickListener { saveWebsiteSettings() }
        }
        form.addView(saveBtn)

        populateSettingsFields()
        scroll.addView(form)
        return scroll
    }

    private fun populateSettingsFields() {
        val s = dashboardData?.settings ?: return
        if (::brandNameInput.isInitialized) {
            brandNameInput.setText(s["brand_name"] ?: "")
            taglineInput.setText(s["brand_tagline"] ?: "")
            waNumberInput.setText(s["whatsapp_number"] ?: "")
            upiIdInput.setText(s["upi_id"] ?: "")
            announcementInput.setText(s["announcement_text"] ?: "")
        }
    }

    private fun saveWebsiteSettings() {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please set Sheet URL first", Toast.LENGTH_SHORT).show()
            return
        }

        val body = mapOf(
            "action" to "updateSettings",
            "brand_name" to brandNameInput.text.toString().trim(),
            "brand_tagline" to taglineInput.text.toString().trim(),
            "whatsapp_number" to waNumberInput.text.toString().trim(),
            "upi_id" to upiIdInput.text.toString().trim(),
            "announcement_text" to announcementInput.text.toString().trim()
        )

        Toast.makeText(this, "Updating website...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Website updated live successfully! 🎉", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // TAB 3: PRODUCTS & PRICE MANAGER
    // ==========================================
    private fun createProductsTab(): View {
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val heading = TextView(this).apply {
            text = "🏷️ Products & Price Manager"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        }
        scrollContent.addView(heading)

        val subtext = TextView(this).apply {
            text = "Products ke prices badlein ya active/inactive karein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 24)
        }
        scrollContent.addView(subtext)

        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        productsAdapter = ProductsAdapter(dashboardData?.products ?: emptyList()) { product, newSelling, newOriginal, isActive ->
            saveProductChanges(product, newSelling, newOriginal, isActive)
        }
        rv.adapter = productsAdapter
        scrollContent.addView(rv)

        return scrollContent
    }

    private fun saveProductChanges(product: Product, newSelling: String, newOriginal: String, isActive: Boolean) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        val body = mapOf(
            "action" to "updateProduct",
            "id" to product.id,
            "price_selling" to newSelling,
            "price_original" to newOriginal,
            "is_active" to if (isActive) "TRUE" else "FALSE"
        )

        Toast.makeText(this, "Updating ${product.title}...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateProduct(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        product.priceSelling = newSelling
                        product.priceOriginal = newOriginal
                        product.isActive = if (isActive) "TRUE" else "FALSE"
                        Toast.makeText(this@MainActivity, "Product price updated! ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // HELPERS & NAVIGATION
    // ==========================================
    private fun createBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(16, 20, 16, 20)

            addView(createNavItem("🛒 Orders", 0))
            addView(createNavItem("🌐 Website", 1))
            addView(createNavItem("📦 Products", 2))
        }
    }

    private fun createNavItem(label: String, index: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(if (currentTab == index) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { switchTab(index) }
        }
    }

    private fun updateNavSelection() {
        for (i in 0 until navBar.childCount) {
            val btn = navBar.getChildAt(i) as? Button ?: continue
            btn.setTextColor(if (currentTab == i) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
        }
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, 16, 0, 8)
        }
    }

    private fun createInput(placeholder: String): EditText {
        return EditText(this).apply {
            hint = placeholder
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#64748B"))
            textSize = 14f
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(28, 24, 28, 24)
        }
    }

    private fun checkPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS
            ),
            101
        )

        // Prompt to enable notification listener service if not already enabled
        val listeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (listeners == null || !listeners.contains(packageName)) {
            Toast.makeText(this, "Enable Notification Access to auto-verify PhonePe/GPay", Toast.LENGTH_LONG).show()
        }
    }
}

