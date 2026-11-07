package com.sheetstore.admin.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
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
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {

    private lateinit var contentContainer: FrameLayout
    private lateinit var navBar: LinearLayout

    private var currentTab = 0 // 0: Orders, 1: Templates, 2: Website, 3: Products
    private var dashboardData: DashboardResponse? = null

    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var productsAdapter: ProductsAdapter

    // 5 Ready-made Website Templates
    data class WebTheme(
        val id: String,
        val name: String,
        val subtitle: String,
        val description: String,
        val bestFor: String,
        val colors: List<String>
    )

    private val availableTemplates = listOf(
        WebTheme(
            id = "modern-blue",
            name = "⚡ Modern Cyber Blue",
            subtitle = "Tech, SaaS & Digital Store (Default)",
            description = "Sleek dark slate aesthetic with electric blue glows and glassmorphism.",
            bestFor = "Best for: Software, digital goods, web kits & modern agencies",
            colors = listOf("#080C1A", "#2563EB", "#38BDF8")
        ),
        WebTheme(
            id = "vedic-gold",
            name = "🕉️ Royal Vedic Gold",
            subtitle = "Vastu, Astrology & Luxury Spiritual",
            description = "Majestic dark plum maroon with radiant sun gold and warm ambient lighting.",
            bestFor = "Best for: Vastu Shastra, astrology, gemstones, spiritual items & luxury",
            colors = listOf("#090510", "#F59E0B", "#D97706")
        ),
        WebTheme(
            id = "minimal-light",
            name = "🛍️ Clean Minimal Light",
            subtitle = "Modern D2C Storefront (White)",
            description = "Crisp white minimalist design with high-contrast text & emerald green accents.",
            bestFor = "Best for: Clean retail, e-commerce, physical products & fashion",
            colors = listOf("#F8FAFC", "#0F172A", "#059669")
        ),
        WebTheme(
            id = "neon-violet",
            name = "👾 Cyberpunk Neon Violet",
            subtitle = "Creators, AI Prompts & Gaming",
            description = "Pitch black background with radiant violet and neon pink gradients.",
            bestFor = "Best for: AI prompts, video LUTs, Canva templates & gaming assets",
            colors = listOf("#030107", "#8B5CF6", "#EC4899")
        ),
        WebTheme(
            id = "emerald-pro",
            name = "🌿 Emerald Business Pro",
            subtitle = "Finance & High-Trust B2B",
            description = "Corporate dark pine slate with vibrant mint green trust badges.",
            bestFor = "Best for: Financial consulting, ebooks, coaching & corporate sales",
            colors = listOf("#03140F", "#10B981", "#047857")
        )
    )

    companion object {
        private const val REQUEST_PICK_LOGO = 2001
        private const val REQUEST_PICK_BANNER = 2002
        private const val MASTER_AUTH_URL = "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec"
    }

    // Views in Website Customizer
    private lateinit var brandNameInput: EditText
    private lateinit var taglineInput: EditText
    private lateinit var logoUrlInput: EditText
    private lateinit var bannerUrlInput: EditText
    private lateinit var fontFamilyInput: EditText
    private lateinit var headerColorInput: EditText
    private lateinit var footerColorInput: EditText
    private lateinit var heroTitleInput: EditText
    private lateinit var heroSubtitleInput: EditText
    private lateinit var ctaTextInput: EditText
    private lateinit var mainProductPriceInput: EditText
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

        // User Auth Login Button
        val authBtn = Button(this).apply {
            val email = getLoggedInEmail()
            text = if (email.isNotEmpty()) "👤 ${getLoggedInName()}" else "👤 Login"
            textSize = 11f
            setTextColor(Color.parseColor("#10B981"))
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(20, 10, 20, 10)
            setOnClickListener { showAuthDialog(this) }
        }
        topBar.addView(authBtn)

        // Sheet URL Settings Button
        val settingsBtn = Button(this).apply {
            text = "⚙️ Sheet URL"
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(20, 10, 20, 10)
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

        // On first start: if not logged in, prompt user to log in with Mobile Number & Password for 100% automatic Sheet URL sync!
        val loggedInEmail = getLoggedInEmail()
        if (loggedInEmail.isEmpty()) {
            showAuthDialog(authBtn)
        } else {
            fetchDashboard()
        }
    }

    private fun getSheetUrl(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("sheet_web_app_url", "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec") ?: "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec"
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

    private fun getLoggedInEmail(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("logged_in_email", "") ?: ""
    }

    private fun getLoggedInName(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val name = prefs.getString("logged_in_name", "") ?: ""
        return if (name.isNotEmpty()) name.split(" ").firstOrNull() ?: name else "Account"
    }

    private fun saveAuthSession(name: String, email: String, sheetUrl: String?) {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val edit = prefs.edit()
            .putString("logged_in_name", name)
            .putString("logged_in_email", email)
        if (!sheetUrl.isNullOrEmpty()) {
            edit.putString("sheet_web_app_url", sheetUrl)
        }
        edit.apply()
    }

    private fun clearAuthSession() {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("logged_in_name")
            .remove("logged_in_email")
            .apply()
    }

    private fun showAuthDialog(authBtn: Button) {
        val currentEmail = getLoggedInEmail()
        if (currentEmail.isNotEmpty()) {
            val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
            val name = prefs.getString("logged_in_name", "User") ?: "User"
            val sheetUrl = getSheetUrl()

            val msg = "👤 Logged in as: $name\n✉️ Email: $currentEmail\n\n📊 Connected Sheet:\n${sheetUrl.take(45)}..."

            AlertDialog.Builder(this)
                .setTitle("Account Info")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .setNegativeButton("🚪 Log Out") { _, _ ->
                    clearAuthSession()
                    authBtn.text = "👤 Login"
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                }
                .show()
        } else {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 20)
            }

            val emailInput = EditText(this).apply {
                hint = "WhatsApp Mobile No. ya Email (e.g. 9876543210)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT
                setTextColor(Color.BLACK)
                setPadding(24, 20, 24, 20)
            }
            dialogLayout.addView(emailInput)

            val passInput = EditText(this).apply {
                hint = "Password (Default: 123456)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                setTextColor(Color.BLACK)
                setPadding(24, 20, 24, 20)
            }
            dialogLayout.addView(passInput)

            AlertDialog.Builder(this)
                .setTitle("Store Portal Login 🔐")
                .setMessage("Website checkout wala Mobile Number aur Password daalein (Default: 123456). Aapka Sheet URL apne aap connect ho jayega!")
                .setView(dialogLayout)
                .setPositiveButton("Log In 🚀") { _, _ ->
                    val email = emailInput.text.toString().trim()
                    val pass = passInput.text.toString().trim()

                    if (email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Email aur Password dono zaroori hain", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Always authenticate against Central Master Users Database (which stores the user's Step 2 Sheet URL)
                            val call = ApiClient.service.login(MASTER_AUTH_URL, "login", email, pass)
                            val res = call.execute()
                            val body = res.body()

                            withContext(Dispatchers.Main) {
                                if (body != null && body.success && body.user != null) {
                                    val u = body.user
                                    val resolvedSheetUrl = if (!u.sheetUrl.isNullOrEmpty()) u.sheetUrl else getSheetUrl()
                                    saveAuthSession(u.name ?: "User", u.email ?: email, resolvedSheetUrl)
                                    authBtn.text = "👤 ${getLoggedInName()}"
                                    Toast.makeText(this@MainActivity, "Welcome ${u.name}! Store Connected 🚀", Toast.LENGTH_LONG).show()
                                    fetchDashboard()
                                } else {
                                    val errMsg = body?.message ?: "Login failed! Kripya details check karein."
                                    Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
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
            1 -> contentContainer.addView(createTemplatesTab())
            2 -> contentContainer.addView(createWebsiteCustomizerTab())
            3 -> contentContainer.addView(createProductsTab())
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
            1 -> {
                contentContainer.removeAllViews()
                contentContainer.addView(createTemplatesTab())
            }
            2 -> populateSettingsFields()
            3 -> {
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
    // TAB 1: 5 READY-MADE WEBSITE TEMPLATES (1-CLICK SWITCHER)
    // ==========================================
    private fun createTemplatesTab(): View {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 48)
        }

        // Section Title & Subtitle
        val heading = TextView(this).apply {
            text = "🎨 1-Click Website Templates"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 6)
        }
        container.addView(heading)

        val subtext = TextView(this).apply {
            text = "Choose any ready-made design below. Ek click me live website ka theme, colors aur layout change ho jayega:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 24)
        }
        container.addView(subtext)

        val currentActiveId = (dashboardData?.settings?.get("template_id") ?: "modern-blue").trim().lowercase()

        // 5 Ready-made Templates Cards
        for (theme in availableTemplates) {
            val isActive = (currentActiveId == theme.id)
            val card = createTemplateCard(theme, isActive)
            container.addView(card)
        }

        scroll.addView(container)
        return scroll
    }

    private fun createTemplateCard(theme: WebTheme, isActive: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(if (isActive) "#1E293B" else "#131C2E"))
            setPadding(32, 32, 32, 32)
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 28)
            }
        }

        // Top Row: Title + Status Badge
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = theme.name
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(titleView)

        val badge = TextView(this).apply {
            if (isActive) {
                text = "✓ ACTIVE LIVE"
                setTextColor(Color.parseColor("#10B981"))
                setBackgroundColor(Color.parseColor("#064E3B"))
            } else {
                text = "READY-MADE"
                setTextColor(Color.parseColor("#94A3B8"))
                setBackgroundColor(Color.parseColor("#0F172A"))
            }
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(20, 8, 20, 8)
        }
        topRow.addView(badge)
        card.addView(topRow)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = theme.subtitle
            textSize = 12f
            setTextColor(Color.parseColor(if (theme.id == "vedic-gold") "#F59E0B" else "#38BDF8"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 8, 0, 6)
        }
        card.addView(subtitleView)

        // Description
        val descView = TextView(this).apply {
            text = theme.description
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, 0, 0, 6)
        }
        card.addView(descView)

        // Best For Tag
        val bestForView = TextView(this).apply {
            text = theme.bestFor
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 16)
        }
        card.addView(bestForView)

        // Color Swatches Row
        val swatchesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 20)
        }

        val paletteLabel = TextView(this).apply {
            text = "Palette: "
            textSize = 11f
            setTextColor(Color.parseColor("#64748B"))
        }
        swatchesRow.addView(paletteLabel)

        for (colorHex in theme.colors) {
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(40, 40).apply {
                    setMargins(8, 0, 8, 0)
                }
                setBackgroundColor(Color.parseColor(colorHex))
            }
            swatchesRow.addView(dot)
        }
        card.addView(swatchesRow)

        // Action Button
        val applyBtn = Button(this).apply {
            if (isActive) {
                text = "Current Active Website Theme ✓"
                setBackgroundColor(Color.parseColor("#065F46"))
                setTextColor(Color.parseColor("#A7F3D0"))
                isEnabled = false
            } else {
                text = "Apply This Template ⚡"
                setBackgroundColor(Color.parseColor("#2563EB"))
                setTextColor(Color.WHITE)
                isEnabled = true
                setOnClickListener {
                    applyWebsiteTemplate(theme.id, theme.name)
                }
            }
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 24, 0, 24)
        }
        card.addView(applyBtn)

        return card
    }

    private fun applyWebsiteTemplate(templateId: String, templateTitle: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please configure Sheet URL first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Activating $templateTitle...", Toast.LENGTH_SHORT).show()

        val body = mapOf(
            "action" to "updateSettings",
            "template_id" to templateId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                        s["template_id"] = templateId
                        dashboardData = dashboardData?.copy(settings = s)
                        refreshCurrentTab()
                        Toast.makeText(this@MainActivity, "🎉 '$templateTitle' activated! Website updated live.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to update template", Toast.LENGTH_SHORT).show()
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
            text = "Yahan se website ka pura data, logo, fonts, header/footer colors aur text live change karein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, 24)
        }
        form.addView(subtext)

        // 1. BRANDING SECTION
        form.addView(createSectionHeader("🏢 1. BRANDING & LOGO"))
        form.addView(createLabel("Website Ka Naam (Brand Name):"))
        brandNameInput = createInput("SheetStore Pro")
        form.addView(brandNameInput)

        form.addView(createLabel("Brand Tagline:"))
        taglineInput = createInput("Zero Hosting & Domain E-Commerce")
        form.addView(taglineInput)

        form.addView(createLabel("Custom Logo Image:"))
        form.addView(createSizeBadge("📐 Logo Size: 512 x 512 px (Square 1:1, PNG/WebP)"))
        logoUrlInput = createInput("https://example.com/my-logo.png")
        form.addView(logoUrlInput)
        val pickLogoBtn = Button(this).apply {
            text = "🖼️ Phone Se Logo Photo Chunein"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(20, 10, 20, 10)
            setOnClickListener { openImagePicker(REQUEST_PICK_LOGO) }
        }
        form.addView(pickLogoBtn)

        form.addView(createLabel("Hero / Showcase Banner Image:"))
        form.addView(createSizeBadge("📐 Banner Size: 1200 x 600 px (Landscape 2:1, WebP/JPG)"))
        bannerUrlInput = createInput("https://images.unsplash.com/photo-1460925895917-afdab827c52f?auto=format&fit=crop&w=800&q=80")
        form.addView(bannerUrlInput)
        val pickBannerBtn = Button(this).apply {
            text = "🖼️ Phone Se Banner Photo Chunein"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(20, 10, 20, 10)
            setOnClickListener { openImagePicker(REQUEST_PICK_BANNER) }
        }
        form.addView(pickBannerBtn)

        // 2. DESIGN & STYLING SECTION
        form.addView(createSectionHeader("🎨 2. FONTS & COLORS"))
        form.addView(createLabel("Heading Font (Tap Button To Select):"))
        fontFamilyInput = createInput("Outfit")
        form.addView(fontFamilyInput)
        form.addView(createFontChips { fontName, label ->
            fontFamilyInput.setText(fontName)
            Toast.makeText(this, "Font Chuna: $label", Toast.LENGTH_SHORT).show()
        })

        form.addView(createLabel("Header Background Color (Tap Color To Select):"))
        headerColorInput = createInput("#080C1A")
        form.addView(headerColorInput)
        form.addView(createColorChips { hex, name ->
            headerColorInput.setText(hex)
            Toast.makeText(this, "Header Color: $name", Toast.LENGTH_SHORT).show()
        })

        form.addView(createLabel("Footer Background Color (Tap Color To Select):"))
        footerColorInput = createInput("#050811")
        form.addView(footerColorInput)
        form.addView(createColorChips { hex, name ->
            footerColorInput.setText(hex)
            Toast.makeText(this, "Footer Color: $name", Toast.LENGTH_SHORT).show()
        })

        // 3. TEXT & HEADLINES SECTION
        form.addView(createSectionHeader("✍️ 3. HEADLINES & BUTTONS"))
        form.addView(createLabel("Hero Main Headline:"))
        heroTitleInput = createInput("Shopify aur WordPress ka Monthly Kharcha Band Karein.")
        form.addView(heroTitleInput)

        form.addView(createLabel("Hero Subtitle / Description:"))
        heroSubtitleInput = createInput("Google Sheet ko apna backend banayein aur super fast website live karein.")
        form.addView(heroSubtitleInput)

        form.addView(createLabel("Main CTA Button Text (e.g. Get Store Now / Buy Now):"))
        ctaTextInput = createInput("Get Store Now")
        form.addView(ctaTextInput)

        form.addView(createLabel("Main Website Product Price in ₹ (Live Test Ke Liye ₹10):"))
        mainProductPriceInput = createInput("10")
        form.addView(mainProductPriceInput)

        form.addView(createLabel("Top Announcement Bar Text:"))
        announcementInput = createInput("Special Discount Today! 80% OFF")
        form.addView(announcementInput)

        // 4. CONTACT & PAYMENTS SECTION
        form.addView(createSectionHeader("💳 4. CONTACT & PAYMENTS"))
        form.addView(createLabel("WhatsApp Order Number (with 91):"))
        waNumberInput = createInput("919999999999")
        form.addView(waNumberInput)

        form.addView(createLabel("UPI ID (Payments Received Here):"))
        upiIdInput = createInput("yourname@okaxis")
        form.addView(upiIdInput)

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

    private fun createSectionHeader(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 24, 0, 8)
        }
    }

    private fun createColorChips(onColorSelected: (String, String) -> Unit): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val palette = listOf(
            Pair("🖤 Dark Slate", "#080C1A"),
            Pair("💜 Royal Vedic", "#160826"),
            Pair("💙 Tech Navy", "#0F172A"),
            Pair("💚 Forest Pine", "#041D14"),
            Pair("🖤 Pure Black", "#000000"),
            Pair("🤍 Clean Slate", "#F8FAFC")
        )

        for (item in palette) {
            val btn = Button(this).apply {
                text = item.first
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.parseColor("#1E293B"))
                setTextColor(Color.WHITE)
                setPadding(20, 10, 20, 10)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 12, 0)
                }
                setOnClickListener {
                    onColorSelected(item.second, item.first)
                }
            }
            chipsRow.addView(btn)
        }

        scroll.addView(chipsRow)
        container.addView(scroll)
        return container
    }

    private fun createFontChips(onFontSelected: (String, String) -> Unit): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 16)
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val fonts = listOf(
            Pair("🕉️ Vedic (Cinzel)", "Cinzel"),
            Pair("⚡ Modern (Outfit)", "Outfit"),
            Pair("👾 Creative (Space)", "Space Grotesk"),
            Pair("🛍️ Minimal (Jakarta)", "Plus Jakarta Sans")
        )

        for (item in fonts) {
            val btn = Button(this).apply {
                text = item.first
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.parseColor("#1E293B"))
                setTextColor(Color.parseColor("#38BDF8"))
                setPadding(20, 10, 20, 10)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 12, 0)
                }
                setOnClickListener {
                    onFontSelected(item.second, item.first)
                }
            }
            chipsRow.addView(btn)
        }

        scroll.addView(chipsRow)
        container.addView(scroll)
        return container
    }

    private fun populateSettingsFields() {
        val s = dashboardData?.settings ?: return
        if (::brandNameInput.isInitialized) {
            brandNameInput.setText(s["brand_name"] ?: "")
            taglineInput.setText(s["brand_tagline"] ?: "")
            logoUrlInput.setText(s["logo_url"] ?: "")
            bannerUrlInput.setText(s["banner_url"] ?: "")
            fontFamilyInput.setText(s["font_family"] ?: "")
            headerColorInput.setText(s["header_color"] ?: "")
            footerColorInput.setText(s["footer_color"] ?: "")
            heroTitleInput.setText(s["hero_title"] ?: "")
            heroSubtitleInput.setText(s["hero_subtitle"] ?: "")
            ctaTextInput.setText(s["cta_text"] ?: "")
            mainProductPriceInput.setText(s["main_product_price"] ?: "10")
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
            "logo_url" to logoUrlInput.text.toString().trim(),
            "banner_url" to bannerUrlInput.text.toString().trim(),
            "font_family" to fontFamilyInput.text.toString().trim(),
            "header_color" to headerColorInput.text.toString().trim(),
            "footer_color" to footerColorInput.text.toString().trim(),
            "hero_title" to heroTitleInput.text.toString().trim(),
            "hero_subtitle" to heroSubtitleInput.text.toString().trim(),
            "cta_text" to ctaTextInput.text.toString().trim(),
            "main_product_price" to mainProductPriceInput.text.toString().trim(),
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
            setPadding(8, 16, 8, 16)

            addView(createNavItem("🛒 Orders", 0))
            addView(createNavItem("🎨 Themes", 1))
            addView(createNavItem("🌐 Settings", 2))
            addView(createNavItem("📦 Products", 3))
        }
    }

    private fun createNavItem(label: String, index: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
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

    private fun createSizeBadge(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            setBackgroundColor(Color.parseColor("#064E3B"))
            setPadding(16, 6, 16, 6)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 8)
            }
        }
    }

    private fun openImagePicker(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    when (requestCode) {
                        REQUEST_PICK_LOGO -> {
                            // Scale down to max 300x300 for crisp lightweight square logo
                            val scaled = scaleBitmap(originalBitmap, 300, 300)
                            val base64 = bitmapToBase64(scaled)
                            val dataUrl = "data:image/jpeg;base64,$base64"
                            logoUrlInput.setText(dataUrl)
                            Toast.makeText(this, "Logo photo chuni gayi! (300x300 px)", Toast.LENGTH_SHORT).show()
                        }
                        REQUEST_PICK_BANNER -> {
                            // Scale down to max 800x400 for landscape banner
                            val scaled = scaleBitmap(originalBitmap, 800, 400)
                            val base64 = bitmapToBase64(scaled)
                            val dataUrl = "data:image/jpeg;base64,$base64"
                            bannerUrlInput.setText(dataUrl)
                            Toast.makeText(this, "Banner photo chuni gayi! (800x400 px)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Photo load karne me error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, maxW: Int, maxH: Int): Bitmap {
        var w = bitmap.width
        var h = bitmap.height
        if (w > maxW || h > maxH) {
            val ratio = w.toFloat() / h.toFloat()
            if (ratio > 1) {
                w = maxW
                h = (maxW / ratio).toInt()
            } else {
                h = maxH
                w = (maxH * ratio).toInt()
            }
            return Bitmap.createScaledBitmap(bitmap, w, h, true)
        }
        return bitmap
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
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

