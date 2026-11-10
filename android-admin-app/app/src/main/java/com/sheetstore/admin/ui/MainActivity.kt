package com.sheetstore.admin.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sheetstore.admin.R
import com.sheetstore.admin.models.*
import com.sheetstore.admin.network.ApiClient
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class MainActivity : Activity() {

    private var autoPollJob: Job? = null
    private val knownOrderIds = mutableSetOf<String>()

    private lateinit var contentContainer: FrameLayout
    private lateinit var navBar: LinearLayout
    private lateinit var topBarView: LinearLayout

    private var currentTab = 0 // 0: Orders, 1: Niche & Themes, 2: Customizer, 3: Products
    private var dashboardData: DashboardResponse? = null

    private lateinit var ordersAdapter: OrdersAdapter
    private var ordersRecyclerView: RecyclerView? = null
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var authBtn: Button
    private lateinit var headerNichePill: TextView
    private lateinit var topAvatarView: TextView
    private lateinit var topBrandNameView: TextView
    private var activeHubMockupLogo: ImageView? = null

    // Top Stats Views & Orders Segment Views
    private var statsRevenueView: TextView? = null
    private var statsOrdersView: TextView? = null
    private var ordersSegmentLiveBtn: TextView? = null
    private var ordersSegmentPaidBtn: TextView? = null
    private var ordersEmptyView: TextView? = null
    private var designSubTabIndex = -1

    // 6 Ready-made Website Themes
    data class WebTheme(
        val id: String,
        val name: String,
        val subtitle: String,
        val description: String,
        val bestFor: String,
        val colors: List<String>,
        val matchedNiches: List<String>
    )

    private val availableTemplates = listOf(
        WebTheme(
            id = "modern-blue",
            name = "⚡ Modern Cyber Blue",
            subtitle = "Tech, SaaS & Digital Store (Default)",
            description = "Sleek dark slate aesthetic with electric blue glows and glassmorphism.",
            bestFor = "Best for: Software, digital goods, web kits & modern agencies",
            colors = listOf("#080C1A", "#2563EB", "#38BDF8"),
            matchedNiches = listOf("digital", "portfolio", "ecommerce", "linktree")
        ),
        WebTheme(
            id = "vedic-gold",
            name = "🕉️ Royal Vedic Gold",
            subtitle = "Vastu, Astrology & Luxury Spiritual",
            description = "Majestic dark plum maroon with radiant sun gold and warm ambient lighting.",
            bestFor = "Best for: Vastu Shastra, astrology, gemstones, spiritual items & luxury",
            colors = listOf("#090510", "#F59E0B", "#D97706"),
            matchedNiches = listOf("vastu")
        ),
        WebTheme(
            id = "minimal-light",
            name = "🛍️ Clean Minimal Light",
            subtitle = "Modern D2C Storefront (White)",
            description = "Crisp white minimalist design with high-contrast text & emerald green accents.",
            bestFor = "Best for: Clean retail, e-commerce, physical products & fashion",
            colors = listOf("#F8FAFC", "#0F172A", "#059669"),
            matchedNiches = listOf("ecommerce", "linktree", "portfolio")
        ),
        WebTheme(
            id = "neon-violet",
            name = "👾 Cyberpunk Neon Violet",
            subtitle = "Creators, AI Prompts & Bio Links",
            description = "Pitch black background with radiant violet and neon pink gradients.",
            bestFor = "Best for: Bio link cards, AI prompts, Canva templates & gaming assets",
            colors = listOf("#030107", "#8B5CF6", "#EC4899"),
            matchedNiches = listOf("linktree", "digital")
        ),
        WebTheme(
            id = "emerald-pro",
            name = "🌿 Emerald Business Pro",
            subtitle = "Finance & High-Trust B2B",
            description = "Corporate dark pine slate with vibrant mint green trust badges.",
            bestFor = "Best for: Freelancers, web agency, consulting, ebooks & corporate sales",
            colors = listOf("#03140F", "#10B981", "#047857"),
            matchedNiches = listOf("portfolio", "ecommerce", "digital")
        ),
        WebTheme(
            id = "noolam-saree",
            name = "🥻 NOOLAM Kanchipuram Silk",
            subtitle = "Sarees, Ethnic Wear & Bridal Couture",
            description = "Royal South Indian temple sanctum aesthetic with 24K gold zari and Korvai borders.",
            bestFor = "Best for: Silk sarees, handloom, lehengas, bridal jewellery & ethnic fashion",
            colors = listOf("#140204", "#D4AF37", "#500A13"),
            matchedNiches = listOf("saree", "ecommerce")
        )
    )

    companion object {
        const val ORDER_CHANNEL_ID = "sheetstore_orders_channel"
        const val NOTIFICATION_PERMISSION_CODE = 102
        private const val REQUEST_PICK_LOGO = 2001
        private const val REQUEST_PICK_BANNER = 2002
        private const val REQUEST_PICK_LOGO_HUB = 2003
        private const val REQUEST_PICK_QR = 2004
        private const val MASTER_AUTH_URL = "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec"
    }

    // Store Master Hub Views
    private var activeHubLogoInput: EditText? = null
    private var activeHubLogoPreview: ImageView? = null

    // Site Customizer Input Views
    private val pendingSettings = mutableMapOf<String, String>()
    private var liveLogoPreview: ImageView? = null
    private var liveBannerPreview: ImageView? = null
    private var liveQrPreview: ImageView? = null

    private lateinit var brandNameInput: EditText
    private lateinit var taglineInput: EditText
    private lateinit var logoUrlInput: EditText
    private lateinit var bannerUrlInput: EditText
    private lateinit var fontFamilyInput: EditText
    private lateinit var headerColorInput: EditText
    private lateinit var footerColorInput: EditText
    private lateinit var heroBadgeInput: EditText
    private lateinit var heroTitleInput: EditText
    private lateinit var heroSubtitleInput: EditText
    private lateinit var ctaTextInput: EditText
    private lateinit var announcementInput: EditText
    private lateinit var announcementSwitch: Switch
    private lateinit var waNumberInput: EditText
    private lateinit var upiIdInput: EditText
    private lateinit var altPhoneInput: EditText
    private lateinit var helplineInput: EditText
    private lateinit var businessEmailInput: EditText
    private lateinit var businessAddressInput: EditText
    private lateinit var mapUrlInput: EditText
    private lateinit var qrCodeUrlInput: EditText
    private lateinit var contactFormSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Local Push Notification Alert Channel & Known Orders
        createNotificationChannel()
        loadKnownOrderIds()
        // Notification permission will be requested after login in showDashboardHome()

        // Root Layout (Sleek Deep Night Navy #0B0F19)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        // 1. Native Modern Top App Bar
        topBarView = createTopAppBar()
        root.addView(topBarView)

        // 2. Main Content Frame
        contentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        root.addView(contentContainer)

        // 3. Modern Rounded Bottom Navigation Dock
        navBar = createBottomNav()
        root.addView(navBar)

        setContentView(root)

        // Initially hide topBar and navBar so user cannot bypass login gate
        topBarView.visibility = View.GONE
        navBar.visibility = View.GONE

        // Start with Full-Screen Splash Screen, then Full-Screen Login, then Theme Selection
        showSplashScreen()

        // Start Background Auto-Polling for incoming orders
        startAutoPolling()
    }

    override fun onResume() {
        super.onResume()
        if (isUserLoggedIn()) {
            fetchDashboard(silent = true)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isUserLoggedIn()) {
            topBarView.visibility = View.VISIBLE
            navBar.visibility = View.VISIBLE
            handleIntentTab(intent)
            fetchDashboard()
        }
    }

    private fun handleIntentTab(intent: Intent?) {
        val tab = intent?.getIntExtra("open_tab", -1) ?: -1
        if (tab >= 0) {
            switchTab(tab)
        } else {
            switchTab(0)
        }
        val targetOrderId = intent?.getStringExtra("order_id")
        if (!targetOrderId.isNullOrEmpty()) {
            val cleanTarget = targetOrderId.replace("#", "").trim()
            CoroutineScope(Dispatchers.Main).launch {
                delay(350)
                ordersRecyclerView?.let { rv ->
                    val orders = dashboardData?.orders ?: emptyList()
                    val idx = orders.indexOfFirst { it.orderId.replace("#", "").trim().equals(cleanTarget, ignoreCase = true) }
                    if (idx != -1) {
                        rv.smoothScrollToPosition(idx)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        autoPollJob?.cancel()
    }

    // ==========================================
    // TOP APP BAR (PROFILE & LOGOUT ONLY)
    // ==========================================
    private fun createTopAppBar(): LinearLayout {
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 12))
            setBackgroundColor(Color.parseColor("#0F172A"))
            elevation = UiKit.dp(this@MainActivity, 6).toFloat()
        }

        // Left: Profile & Subscription Button
        authBtn = Button(this).apply {
            val name = getLoggedInName().ifEmpty { "Profile" }
            text = "👤 $name"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor("#1E3A8A"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this@MainActivity, 38))
            setOnClickListener { showProfileSubscriptionDialog() }
        }
        topBar.addView(authBtn)

        // Center: Brand Name Title & Small Avatar
        val centerBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = UiKit.dp(this@MainActivity, 6)
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
        }

        topAvatarView = TextView(this).apply {
            val brand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
            text = brand.take(2).uppercase()
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#00F0FF"),
                radiusDp = 8f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 26), UiKit.dp(this@MainActivity, 26)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        centerBox.addView(topAvatarView)

        topBrandNameView = TextView(this).apply {
            val brand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
            text = brand
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        centerBox.addView(topBrandNameView)
        topBar.addView(centerBox)

        val activeNiche = getActiveNiche()
        headerNichePill = TextView(this).apply {
            text = activeNiche.emoji
            visibility = View.GONE
        }

        // Right: Logout Button
        val logoutBtn = Button(this).apply {
            text = "🚪 Logout"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F87171"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#271216"),
                strokeColor = Color.parseColor("#7F1D1D"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this@MainActivity, 38))
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Logout Confirmation")
                    .setMessage("Kya aap Admin Portal se logout karna chahte hain?")
                    .setPositiveButton("Logout") { _, _ -> performLogout() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        topBar.addView(logoutBtn)

        return topBar
    }

    private fun showProfileSubscriptionDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 20))
        }

        val header = TextView(this).apply {
            text = "👤 Merchant Profile & Subscription"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        dialogView.addView(header)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 16f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16))
        }

        val name = getLoggedInName().ifEmpty { "Store Owner" }
        val phone = getLoggedInPhone().ifEmpty { "9907522413" }
        val email = getLoggedInEmail().ifEmpty { "merchant@sheetstore.com" }
        val userId = getUserId().ifEmpty { "PRO-MERCHANT" }

        val details = listOf(
            Pair("👤 Store Owner:", name),
            Pair("📱 Registered Phone:", phone),
            Pair("✉️ Registered Email:", email),
            Pair("🆔 Merchant ID:", userId),
            Pair("⚡ Subscription Plan:", "Lifetime Pro License"),
            Pair("🟢 Status:", "✓ Active & Verified"),
            Pair("📅 Validity:", "Lifetime Unlimited Access"),
            Pair("📊 Database:", "Google Sheet Cloud Sync Active")
        )

        for (item in details) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, UiKit.dp(this@MainActivity, 6), 0, UiKit.dp(this@MainActivity, 6))
            }
            val label = TextView(this).apply {
                text = item.first
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#94A3B8"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.1f)
            }
            val value = TextView(this).apply {
                text = item.second
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (item.first.contains("Status")) Color.parseColor("#10B981") else Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f)
            }
            row.addView(label)
            row.addView(value)
            card.addView(row)
        }
        dialogView.addView(card)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Theek Hai / Close", null)
            .show()
    }

    // ==========================================
    // MULTI-PURPOSE INITIAL ONBOARDING FLOW
    // ==========================================
    private fun isOnboardingCompleted(): Boolean {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_onboarding_completed", false)
    }

    private fun setOnboardingCompleted(completed: Boolean) {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_onboarding_completed", completed).apply()
    }

    private fun getActiveNicheId(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("business_niche_id", "digital") ?: "digital"
    }

    private fun setActiveNicheId(id: String) {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("business_niche_id", id).apply()
    }

    private fun getActiveNiche(): BusinessNiche {
        return NicheCatalog.getNicheById(getActiveNicheId())
    }

    private fun createOnboardingView(isFromDashboard: Boolean = false): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 40))
        }

        // Back button if opened from dashboard
        if (isFromDashboard) {
            val backRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
            }
            val backBtn = TextView(this).apply {
                text = "← Back to Dashboard"
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                setOnClickListener {
                    showDashboardHome()
                }
            }
            backRow.addView(backBtn)
            container.addView(backRow)
        }

        // Header Badge
        val topBadge = UiKit.createPillBadge(
            context = this,
            text = "✨ BUSINESS SEGMENT SELECTION",
            textColor = Color.parseColor("#00F0FF"),
            bgColor = Color.parseColor("#0C2A4A"),
            borderColor = Color.parseColor("#00F0FF")
        )
        container.addView(topBadge)

        // Title
        val titleView = TextView(this).apply {
            text = "Apna Business Segment Chunein"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 4))
        }
        container.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "SheetStore Pro multi-purpose hai! Aap kis category ka business chalate hain? Chuniye, aur app automatic matching website layout aur copy set kar dega:"
            textSize = 12.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setLineSpacing(6f, 1.15f)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 20))
        }
        container.addView(subtitleView)

        var selectedNicheId = getActiveNicheId()
        val nicheCardViews = mutableListOf<Pair<BusinessNiche, LinearLayout>>()

        // Render 5 Rich Niche Cards
        for (niche in NicheCatalog.allNiches) {
            val isSelected = (niche.id == selectedNicheId)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiKit.cardDrawable(
                    bgColor = if (isSelected) Color.parseColor("#152238") else Color.parseColor("#121826"),
                    strokeColor = if (isSelected) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                    strokeWidthDp = if (isSelected) 2 else 1,
                    radiusDp = 18f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 14)
                }
            }

            // Top Row: Emoji + Title + Selection Indicator
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val iconView = TextView(this).apply {
                text = niche.emoji
                textSize = 22f
                setPadding(0, 0, UiKit.dp(this@MainActivity, 12), 0)
            }
            topRow.addView(iconView)

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val title = TextView(this).apply {
                text = niche.title
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            textCol.addView(title)

            val sub = TextView(this).apply {
                text = niche.subtitle
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
            }
            textCol.addView(sub)
            topRow.addView(textCol)

            val checkBadge = TextView(this).apply {
                text = if (isSelected) "✓ SELECTED" else "SELECT"
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isSelected) Color.parseColor("#00F0FF") else Color.parseColor("#64748B"))
                background = UiKit.cardDrawable(
                    bgColor = if (isSelected) Color.parseColor("#0C2A4A") else Color.parseColor("#1A2234"),
                    strokeColor = if (isSelected) Color.parseColor("#00F0FF") else Color.parseColor("#2E3D5B"),
                    strokeWidthDp = 1,
                    radiusDp = 8f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4))
            }
            topRow.addView(checkBadge)
            card.addView(topRow)

            // Features chips
            val chipRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, UiKit.dp(this@MainActivity, 10), 0, 0)
            }
            for (feat in niche.features.take(3)) {
                val chip = TextView(this).apply {
                    text = "• $feat"
                    textSize = 10f
                    setTextColor(Color.parseColor("#38BDF8"))
                    setPadding(0, 0, UiKit.dp(this@MainActivity, 10), 0)
                }
                chipRow.addView(chip)
            }
            card.addView(chipRow)

            // Recommended Theme Pill
            val recThemeLabel = TextView(this).apply {
                text = "🎨 Auto-Matched Theme: ${niche.recommendedThemeName}"
                textSize = 10.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#F5DE7A"))
                setPadding(0, UiKit.dp(this@MainActivity, 8), 0, 0)
            }
            card.addView(recThemeLabel)

            // Click listener
            card.setOnClickListener {
                selectedNicheId = niche.id
                for ((n, c) in nicheCardViews) {
                    val sel = (n.id == selectedNicheId)
                    c.background = UiKit.cardDrawable(
                        bgColor = if (sel) Color.parseColor("#152238") else Color.parseColor("#121826"),
                        strokeColor = if (sel) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                        strokeWidthDp = if (sel) 2 else 1,
                        radiusDp = 18f,
                        context = this@MainActivity
                    )
                    val r = c.getChildAt(0) as? LinearLayout
                    val chk = r?.getChildAt(2) as? TextView
                    chk?.text = if (sel) "✓ SELECTED" else "SELECT"
                    chk?.setTextColor(if (sel) Color.parseColor("#00F0FF") else Color.parseColor("#64748B"))
                }
            }

            nicheCardViews.add(Pair(niche, card))
            container.addView(card)
        }

        // Action Button: Save Niche & Apply Recommended Defaults
        val continueBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "Apply Segment & Launch Store ➔",
            startColor = "#2563EB",
            endColor = "#00F0FF"
        ) {
            val chosen = NicheCatalog.getNicheById(selectedNicheId)
            setActiveNicheId(chosen.id)
            setOnboardingCompleted(true)

            // Automatically apply the matched theme & pre-filled copy
            applyWebsiteTemplate(chosen.recommendedThemeId, chosen.recommendedThemeName)
            saveNicheDefaultsToSheet(chosen)

            Toast.makeText(this, "🎉 '${chosen.title}' activated! Matched website theme applied.", Toast.LENGTH_LONG).show()

            showDashboardHome()
        }
        container.addView(continueBtn)

        scroll.addView(container)
        return scroll
    }

    private fun saveNicheDefaultsToSheet(niche: BusinessNiche) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        val body = mapOf(
            "action" to "updateSettings",
            "business_niche" to niche.id,
            "template_id" to niche.recommendedThemeId,
            "hero_badge" to niche.defaultBadge,
            "hero_title" to niche.defaultHeroTitle,
            "hero_subtitle" to niche.defaultHeroSubtitle,
            "brand_tagline" to niche.defaultTagline
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.service.updateSettings(fullUrl = url, body = body).execute()
            } catch (e: Exception) {}
        }
    }

    // ==========================================
    // RESTRICTION & VERIFICATION LOCK SCREEN
    // ==========================================
    private fun isSheetVerified(): Boolean {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val verified = prefs.getBoolean("is_sheet_verified", false)
        val email = getLoggedInEmail()
        val phone = getLoggedInPhone()
        val sheetUrl = getSheetUrl()
        return (verified || (email.isNotEmpty() || phone.isNotEmpty())) && sheetUrl.isNotEmpty()
    }

    private fun isUserLoggedIn(): Boolean {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val verified = prefs.getBoolean("is_sheet_verified", false)
        val email = getLoggedInEmail()
        val phone = getLoggedInPhone()
        val sheetUrl = getSheetUrl()
        return (verified || (email.isNotEmpty() || phone.isNotEmpty())) && sheetUrl.isNotEmpty()
    }

    private fun isThemeSelected(): Boolean {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_theme_selected", false)
    }

    private fun showSplashScreen() {
        topBarView.visibility = View.GONE
        navBar.visibility = View.GONE
        contentContainer.removeAllViews()

        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B0F19"))
            setPadding(UiKit.dp(this@MainActivity, 28), UiKit.dp(this@MainActivity, 32), UiKit.dp(this@MainActivity, 28), UiKit.dp(this@MainActivity, 32))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val logoView = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 88), UiKit.dp(this@MainActivity, 88)).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 18)
            }
        }
        splash.addView(logoView)

        val titleView = TextView(this).apply {
            text = "SheetStore Pro"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        splash.addView(titleView)

        val taglineView = TextView(this).apply {
            text = "Zero-Hosting Digital Business Platform"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00F0FF"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 24))
        }
        splash.addView(taglineView)

        val pillBadge = UiKit.createPillBadge(
            context = this,
            text = "🚀 v1.2.8 • Secure Admin Portal",
            textColor = Color.parseColor("#00F0FF"),
            bgColor = Color.parseColor("#0C2A4A"),
            borderColor = Color.parseColor("#00F0FF")
        )
        splash.addView(pillBadge)

        val statusText = TextView(this).apply {
            text = "⚡ Initializing secure session..."
            textSize = 11.5f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 24), 0, 0)
        }
        splash.addView(statusText)

        contentContainer.addView(splash)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1400)
            checkAuthAndNavigate()
        }
    }

    private fun checkAuthAndNavigate() {
        if (!isUserLoggedIn()) {
            showFullScreenLoginScreen()
        } else {
            showDashboardHome()
        }
    }

    private fun showDashboardHome() {
        topBarView.visibility = View.VISIBLE
        navBar.visibility = View.VISIBLE
        authBtn.text = "👤 ${getLoggedInName()} (v1.2.8)"
        val brand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
        topBrandNameView.text = brand
        topAvatarView.text = brand.take(2).uppercase()
        handleIntentTab(intent)
        fetchDashboard()
        // Request notification permission after login / dashboard launch
        requestNotificationPermission()
    }

    private fun showBackendApiConfigDialog() {
        val currentSheet = getSheetUrl().ifEmpty { MASTER_AUTH_URL }
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 14))
        }

        val desc = TextView(this).apply {
            text = "Normal users ke liye Google Sheet automatic sync rehti hai. Agar aapko custom Google Sheet API URL set karna ho toh yahan darj karein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        dialogView.addView(desc)

        val input = UiKit.createStyledInput(this, "https://script.google.com/macros/s/.../exec", currentSheet)
        dialogView.addView(input)

        AlertDialog.Builder(this)
            .setTitle("⚙️ Backend Sheet API Settings")
            .setView(dialogView)
            .setPositiveButton("Save URL") { _, _ ->
                val newUrl = input.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    saveSheetUrl(newUrl)
                    Toast.makeText(this, "Custom Sheet API URL saved! ✓", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Reset Default") { _, _ ->
                saveSheetUrl(MASTER_AUTH_URL)
                Toast.makeText(this, "Reset to default Master API! ✓", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFullScreenLoginScreen() {
        topBarView.visibility = View.GONE
        navBar.visibility = View.GONE
        contentContainer.removeAllViews()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 40))
        }

        // Top Corner Settings Row (Subtle gear so main screen stays clean & automatic)
        val cornerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
        }
        val cornerSettingBtn = Button(this).apply {
            text = "⚙️ Settings"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121B2E"),
                strokeColor = Color.parseColor("#1E2D4A"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
            setOnClickListener {
                showBackendApiConfigDialog()
            }
        }
        cornerRow.addView(cornerSettingBtn)
        container.addView(cornerRow)

        val logoView = ImageView(this).apply {
            setImageResource(R.drawable.ic_app_logo)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 74), UiKit.dp(this@MainActivity, 74)).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }
        container.addView(logoView)

        val headerTitle = TextView(this).apply {
            text = "Admin Portal Login"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        container.addView(headerTitle)

        val vBadge = UiKit.createPillBadge(
            context = this,
            text = "⚡ SheetStore Admin v1.2.8",
            textColor = Color.parseColor("#00F0FF"),
            bgColor = Color.parseColor("#0C2A4A"),
            borderColor = Color.parseColor("#00F0FF")
        )
        container.addView(vBadge)

        val headerSub = TextView(this).apply {
            text = "Bina login ke app access nahi kiya ja sakta. Kripya apna registered WhatsApp Number / Email aur Password daalein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1.15f)
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 22))
        }
        container.addView(headerSub)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 20))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 20)
            }
        }

        val phoneLabel = TextView(this).apply {
            text = "📱 REGISTERED MOBILE YA EMAIL ID"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        card.addView(phoneLabel)

        val savedIdentifier = getLoggedInPhone().ifEmpty { getLoggedInEmail().ifEmpty { "9907522413" } }
        val idInput = UiKit.createStyledInput(this, "jaise: 9907522413 ya email", savedIdentifier)
        card.addView(idInput)

        val passLabel = TextView(this).apply {
            text = "🔑 PASSWORD (DEFAULT: 123456)"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 6))
        }
        card.addView(passLabel)

        val passInput = UiKit.createStyledInput(this, "Password daalein (Default: 123456)", "123456").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        card.addView(passInput)

        val loginBtn = Button(this).apply {
            text = "🚀 Login & Unlock Store ➔"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#00F0FF"),
                radiusDp = 14f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 50)).apply {
                topMargin = UiKit.dp(this@MainActivity, 20)
            }
            setOnClickListener {
                val id = idInput.text.toString().trim()
                val pass = passInput.text.toString().trim()

                if (id.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Kripya Mobile Number ya Email darj karein!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (pass.isEmpty()) {
                    Toast.makeText(this@MainActivity, "Kripya Password darj karein!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                isEnabled = false
                text = "Verifying Credentials... ⏳"
                performMasterLogin(id, pass, authBtn) { success, errMsg ->
                    isEnabled = true
                    text = "🚀 Login & Unlock Store ➔"
                    if (success) {
                        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_sheet_verified", true).apply()
                        showDashboardHome()
                    } else {
                        Toast.makeText(this@MainActivity, "❌ $errMsg", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        card.addView(loginBtn)
        container.addView(card)

        val helpCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0E1626"),
                strokeColor = Color.parseColor("#1B2942"),
                strokeWidthDp = 1,
                radiusDp = 16f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val helpTitle = TextView(this).apply {
            text = "❓ Account nahi bana ya Password bhool gaye?"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        helpCard.addView(helpTitle)

        val helpText = TextView(this).apply {
            text = "Website se order verify hote hi account automatic activate ho jata hai. Agar koi dikkat ho toh WhatsApp support se instant help lein:"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        helpCard.addView(helpText)

        val waBtn = UiKit.createSecondaryButton(this, "💬 WhatsApp Support Par Sampark Karein", "#10B981") {
            try {
                val uri = Uri.parse("https://wa.me/917987823521?text=${Uri.encode("Namaste! Mujhe SheetStore Admin Login karne me sahayata chahiye.")}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }
        helpCard.addView(waBtn)
        container.addView(helpCard)

        scroll.addView(container)
        contentContainer.addView(scroll)
    }

    private fun createWebsiteMockupBox(theme: WebTheme): LinearLayout {
        val bgCol = Color.parseColor(theme.colors[0])
        val accentCol = Color.parseColor(theme.colors[1])
        val lightCol = Color.parseColor(theme.colors[2])
        val isLight = (theme.id == "minimal-light")

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = bgCol,
                strokeColor = accentCol,
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }

        // Mock Browser Address Bar (Compact)
        val urlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = if (isLight) Color.parseColor("#E2E8F0") else Color.parseColor("#151F33"),
                strokeColor = Color.TRANSPARENT,
                strokeWidthDp = 0,
                radiusDp = 5f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 3), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 3))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        val lockDot = View(this).apply {
            background = UiKit.cardDrawable(Color.parseColor("#10B981"), Color.TRANSPARENT, 0, 3f, this@MainActivity)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        urlBar.addView(lockDot)

        val lockText = TextView(this).apply {
            text = "🔒 https://yourstore.com (${theme.name.take(18)})"
            textSize = 8.5f
            setTextColor(if (isLight) Color.parseColor("#475569") else Color.parseColor("#94A3B8"))
        }
        urlBar.addView(lockText)
        box.addView(urlBar)

        // Mock Website Compact Hero / Header Bar
        val mockHeroRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = if (isLight) Color.parseColor("#F1F5F9") else Color.parseColor("#101A2E"),
                strokeColor = accentCol,
                strokeWidthDp = 1,
                radiusDp = 6f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 6))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val brandCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val mockBrand = TextView(this).apply {
            text = when (theme.id) {
                "noolam-saree" -> "🥻 NOOLAM Silk Studio"
                "vedic-gold" -> "🕉️ Vedic Vastu Store"
                "neon-violet" -> "👾 Bio Links & Creator"
                "emerald-pro" -> "🌿 B2B Consulting Pro"
                "minimal-light" -> "🛍️ Clean D2C Storefront"
                else -> "⚡ Cyber Digital Store"
            }
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isLight) Color.parseColor("#0F172A") else Color.WHITE)
        }
        brandCol.addView(mockBrand)

        val mockTag = TextView(this).apply {
            text = "Live Website Mockup Preview"
            textSize = 8f
            setTextColor(lightCol)
        }
        brandCol.addView(mockTag)
        mockHeroRow.addView(brandCol)

        val mockPayBtn = TextView(this).apply {
            text = "⚡ Instant UPI ➔"
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = accentCol,
                strokeColor = Color.TRANSPARENT,
                strokeWidthDp = 0,
                radiusDp = 4f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4))
        }
        mockHeroRow.addView(mockPayBtn)
        box.addView(mockHeroRow)

        return box
    }

    private fun showFullScreenSegmentSelection(isFromDashboard: Boolean = false) {
        topBarView.visibility = View.GONE
        navBar.visibility = View.GONE
        contentContainer.removeAllViews()
        contentContainer.addView(createOnboardingView(isFromDashboard))
    }

    // ==========================================
    // TAB 0: STORE HUB & ORDERS
    // ==========================================
    private fun createStoreHubCard(): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1F2D48"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 18)
            }
        }

        // Header: Store Name + Status Badge
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        val storeBrand = dashboardData?.settings?.get("brand_name") ?: "SheetStore Pro"
        val title = TextView(this).apply {
            text = "⚡ $storeBrand"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val badge = UiKit.createPillBadge(this, "● Live Synced", Color.parseColor("#10B981"), Color.parseColor("#064E3B"), Color.parseColor("#10B981"))
        headerRow.addView(title)
        headerRow.addView(badge)
        card.addView(headerRow)

        // Website Link Row
        val webUrl = getWebsiteLiveUrl()
        val webLabel = TextView(this).apply {
            text = "🌐 $webUrl"
            textSize = 11.5f
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        card.addView(webLabel)

        // Buttons Row 1: Open Website & Share on WhatsApp
        val btnRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        val openWebBtn = Button(this).apply {
            text = "🌐 Open Website ↗"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#1D4ED8"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 40), 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
            setOnClickListener {
                val url = getWebsiteLiveUrl()
                if (url.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Could not open browser", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val shareWaBtn = Button(this).apply {
            text = "📢 WhatsApp Share 📲"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#059669"),
                endColor = Color.parseColor("#047857"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 40), 1f)
            setOnClickListener {
                val url = getWebsiteLiveUrl()
                val brand = dashboardData?.settings?.get("brand_name") ?: "SheetStore"
                val shareText = "Namaste! Hamari dukaan '$brand' live hai! Products dekhne aur order karne ke liye click karein:\n👉 $url"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(intent, "Share Store via"))
            }
        }
        btnRow1.addView(openWebBtn)
        btnRow1.addView(shareWaBtn)
        card.addView(btnRow1)

        // Buttons Row 2: Open Google Sheet & Copy Link
        val btnRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val openSheetBtn = UiKit.createSecondaryButton(this, "📊 Google Sheet ↗", "#38BDF8") {
            val sUrl = getGoogleSpreadsheetUrl()
            if (sUrl.isNotEmpty()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Could not open sheet", Toast.LENGTH_SHORT).show()
                }
            }
        }
        openSheetBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f).apply {
            rightMargin = UiKit.dp(this@MainActivity, 8)
        }

        val copyWebBtn = UiKit.createSecondaryButton(this, "📋 Copy Store Link", "#CBD5E1") {
            val url = getWebsiteLiveUrl()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Store Website URL", url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this@MainActivity, "Website link copied! ✓", Toast.LENGTH_SHORT).show()
        }
        copyWebBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f)

        btnRow2.addView(openSheetBtn)
        btnRow2.addView(copyWebBtn)
        card.addView(btnRow2)

        return card
    }

    private fun createOrdersTab(): View {
        val swipeLayout = SwipeRefreshLayout(this)
        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 24))
        }

        // ==========================================
        // 1. UNIFIED COMMON BOX (Revenue + Fast UTR Approval + Search Bar)
        // ==========================================
        val unifiedBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 18f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 14)
            }
        }

        // --- Row 1: Revenue Stats + Quick Sync Button ---
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        val revCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val revLabel = TextView(this).apply {
            text = "💰 TOTAL STORE REVENUE"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
        }
        revCol.addView(revLabel)

        statsRevenueView = TextView(this).apply {
            text = "₹${(dashboardData?.stats?.totalRevenue ?: 0.0).toInt()}"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, UiKit.dp(this@MainActivity, 2), 0, UiKit.dp(this@MainActivity, 2))
        }
        revCol.addView(statsRevenueView)

        statsOrdersView = TextView(this).apply {
            val stats = dashboardData?.stats
            text = "Total Orders: ${stats?.totalOrders ?: 0} (Paid: ${stats?.paidOrders ?: 0} • Pending: ${stats?.pendingOrders ?: 0})"
            textSize = 11f
            setTextColor(Color.parseColor("#CBD5E1"))
        }
        revCol.addView(statsOrdersView)
        statsRow.addView(revCol)

        val syncBtn = UiKit.createSecondaryButton(this, "🔄 Sync Sheet", "#38BDF8") {
            fetchDashboard()
            Toast.makeText(this@MainActivity, "Refreshing from Google Sheet...", Toast.LENGTH_SHORT).show()
        }
        syncBtn.layoutParams = LinearLayout.LayoutParams(
            UiKit.dp(this@MainActivity, 105),
            UiKit.dp(this@MainActivity, 38)
        )
        statsRow.addView(syncBtn)
        unifiedBox.addView(statsRow)

        // --- Divider Line ---
        val divider = View(this).apply {
            setBackgroundColor(Color.parseColor("#1C2638"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 1)).apply {
                setMargins(0, UiKit.dp(this@MainActivity, 6), 0, UiKit.dp(this@MainActivity, 10))
            }
        }
        unifiedBox.addView(divider)

        // --- Row 2: Manual Payment Approval Action ---
        val manualRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        val manualHintCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val manualTitle = TextView(this).apply {
            text = "⚡ Manual Payment Check"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        manualHintCol.addView(manualTitle)
        val manualSub = TextView(this).apply {
            text = "Bank SMS ya screenshot match karein"
            textSize = 10f
            setTextColor(Color.parseColor("#94A3B8"))
        }
        manualHintCol.addView(manualSub)
        manualRow.addView(manualHintCol)

        val btnApproveUtr = Button(this).apply {
            text = "🔍 12-Digit UTR Approve"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#1D4ED8"),
                radiusDp = 10f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(
                UiKit.dp(this@MainActivity, 155),
                UiKit.dp(this@MainActivity, 38)
            )
            setOnClickListener {
                showManualUtrApprovalDialog()
            }
        }
        manualRow.addView(btnApproveUtr)
        unifiedBox.addView(manualRow)

        // --- Row 3: Live Search Bar ---
        val searchBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0B0F19"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val searchIcon = TextView(this).apply {
            text = "🔍"
            textSize = 13f
            setPadding(0, 0, UiKit.dp(this@MainActivity, 6), 0)
        }
        val searchInput = EditText(this).apply {
            hint = "Search by UTR, Order ID, Phone or Name..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 11.5f
            background = null
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    ordersAdapter.filter(s?.toString() ?: "")
                    updateEmptyOrdersVisibility()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        searchBar.addView(searchIcon)
        searchBar.addView(searchInput)
        unifiedBox.addView(searchBar)

        scrollContent.addView(unifiedBox)

        // ==========================================
        // 2. TWO SEPARATE SEGMENTS: Live Incoming vs Paid Orders
        // ==========================================
        val segmentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F172A"),
                strokeColor = Color.parseColor("#1E293B"),
                strokeWidthDp = 1,
                radiusDp = 12f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
        }

        val pendingCount = dashboardData?.stats?.pendingOrders ?: 0
        val paidCount = dashboardData?.stats?.paidOrders ?: 0

        val tabLiveBtn = TextView(this).apply {
            text = "⚡ Live Incoming ($pendingCount)"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        ordersSegmentLiveBtn = tabLiveBtn

        val tabPaidBtn = TextView(this).apply {
            text = "✓ Paid Orders ($paidCount)"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        ordersSegmentPaidBtn = tabPaidBtn

        fun updateSegmentStyles(activeTab: OrdersAdapter.OrderFilterTab) {
            if (activeTab == OrdersAdapter.OrderFilterTab.LIVE_INCOMING) {
                tabLiveBtn.setTextColor(Color.WHITE)
                tabLiveBtn.background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#152846"),
                    strokeColor = Color.parseColor("#00F0FF"),
                    strokeWidthDp = 1,
                    radiusDp = 9f,
                    context = this@MainActivity
                )
                tabPaidBtn.setTextColor(Color.parseColor("#94A3B8"))
                tabPaidBtn.background = null
            } else {
                tabPaidBtn.setTextColor(Color.WHITE)
                tabPaidBtn.background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#063D2B"),
                    strokeColor = Color.parseColor("#10B981"),
                    strokeWidthDp = 1,
                    radiusDp = 9f,
                    context = this@MainActivity
                )
                tabLiveBtn.setTextColor(Color.parseColor("#94A3B8"))
                tabLiveBtn.background = null
            }
        }

        tabLiveBtn.setOnClickListener {
            ordersAdapter.setFilterTab(OrdersAdapter.OrderFilterTab.LIVE_INCOMING)
            updateSegmentStyles(OrdersAdapter.OrderFilterTab.LIVE_INCOMING)
            updateEmptyOrdersVisibility()
        }

        tabPaidBtn.setOnClickListener {
            ordersAdapter.setFilterTab(OrdersAdapter.OrderFilterTab.PAID)
            updateSegmentStyles(OrdersAdapter.OrderFilterTab.PAID)
            updateEmptyOrdersVisibility()
        }

        segmentContainer.addView(tabLiveBtn)
        segmentContainer.addView(tabPaidBtn)
        scrollContent.addView(segmentContainer)

        updateSegmentStyles(OrdersAdapter.OrderFilterTab.LIVE_INCOMING)

        // Empty Orders State View
        val emptyNotice = TextView(this).apply {
            text = "🎉 Koi pending order nahi hai!\nSabhi incoming orders verified hain."
            textSize = 12.5f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setLineSpacing(4f, 1.15f)
            setPadding(0, UiKit.dp(this@MainActivity, 32), 0, UiKit.dp(this@MainActivity, 32))
            visibility = View.GONE
        }
        ordersEmptyView = emptyNotice
        scrollContent.addView(emptyNotice)

        // RecyclerView
        val rv = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
        ordersRecyclerView = rv
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
        statsOrdersView?.text = "Total: ${stats.totalOrders} • Paid: ${stats.paidOrders} • Pending: ${stats.pendingOrders}"
        ordersSegmentLiveBtn?.text = "⚡ Live Incoming (${stats.pendingOrders})"
        ordersSegmentPaidBtn?.text = "✓ Paid Orders (${stats.paidOrders})"
        updateEmptyOrdersVisibility()
    }

    private fun updateEmptyOrdersVisibility() {
        if (!::ordersAdapter.isInitialized) return
        val count = ordersAdapter.itemCount
        ordersEmptyView?.apply {
            visibility = if (count == 0) View.VISIBLE else View.GONE
            text = if (ordersAdapter.getFilterTab() == OrdersAdapter.OrderFilterTab.LIVE_INCOMING) {
                "🎉 Koi pending order nahi hai!\nSabhi incoming orders verified hain."
            } else {
                "Abhi tak koi paid order nahi hai."
            }
        }
    }

    private fun approveOrder(order: Order) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        val cleanOrderId = order.orderId.replace("#", "").trim()
        val cleanUtr = order.utr.trim()
        Toast.makeText(this, "Approving $cleanOrderId...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.approveOrder(
                    fullUrl = url,
                    orderId = cleanOrderId,
                    utr = cleanUtr
                )
                val response = call.execute()
                val body = response.body()

                withContext(Dispatchers.Main) {
                    val isSuccess = response.isSuccessful && (body?.success == true || (body?.error.isNullOrEmpty() && body?.message != null))
                    if (isSuccess) {
                        order.status = "PAID"
                        order.verified = true
                        ordersAdapter.notifyDataSetChanged()
                        fetchDashboard()
                        Toast.makeText(this@MainActivity, "Order $cleanOrderId Approved! ✓", Toast.LENGTH_SHORT).show()
                    } else {
                        val errMsg = body?.error ?: "Approval failed"
                        Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun approveOrderByUtr(utr: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) return
        val cleanUtr = utr.trim()

        Toast.makeText(this, "Approving UTR $cleanUtr in Google Sheet...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.approveOrderByUtr(
                    fullUrl = url,
                    utr = cleanUtr
                )
                val response = call.execute()
                val body = response.body()

                withContext(Dispatchers.Main) {
                    val isSuccess = response.isSuccessful && (body?.success == true || (body?.error.isNullOrEmpty() && body?.message != null))
                    if (isSuccess) {
                        dashboardData?.orders?.find { it.utr.trim() == cleanUtr }?.let { order ->
                            order.status = "PAID"
                            order.verified = true
                        }
                        ordersAdapter.notifyDataSetChanged()
                        fetchDashboard()
                        Toast.makeText(this@MainActivity, "🎉 UTR $cleanUtr Marked as PAID! ✓", Toast.LENGTH_LONG).show()
                    } else {
                        val errMsg = body?.error ?: "Approval failed or UTR not found"
                        Toast.makeText(this@MainActivity, errMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showManualUtrApprovalDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F172A"),
                strokeColor = Color.parseColor("#2563EB"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 22), UiKit.dp(this@MainActivity, 22), UiKit.dp(this@MainActivity, 22), UiKit.dp(this@MainActivity, 22))
        }

        val title = TextView(this).apply {
            text = "🖐️ Manual Payment / UTR Approval"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        dialogView.addView(title)

        val desc = TextView(this).apply {
            text = "PhonePe, Google Pay ya Paytm se prapt 12-digit UPI UTR / Ref No. yahan enter karein. Google Sheet me order turant 'PAID' mark ho jayega."
            textSize = 11.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
        }
        dialogView.addView(desc)

        val utrInput = EditText(this).apply {
            hint = "12-Digit UTR (e.g. 412984920194)"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(android.text.InputFilter.LengthFilter(12))
            background = UiKit.inputDrawable(this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 12))
        }
        dialogView.addView(utrInput)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, UiKit.dp(this@MainActivity, 16), 0, 0)
        }
        val cancelBtn = UiKit.createSecondaryButton(this, "Cancel", "#94A3B8") {
            dialog.dismiss()
        }
        cancelBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 40), 1f).apply {
            rightMargin = UiKit.dp(this@MainActivity, 8)
        }

        val approveBtn = Button(this).apply {
            text = "Confirm & Mark PAID ✓"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#059669"),
                endColor = Color.parseColor("#047857"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 40), 1.4f)
            setOnClickListener {
                val enteredUtr = utrInput.text.toString().trim()
                if (enteredUtr.length != 12) {
                    Toast.makeText(this@MainActivity, "Kripya valid 12-digit UTR enter karein!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                dialog.dismiss()
                approveOrderByUtr(enteredUtr)
            }
        }
        btnRow.addView(cancelBtn)
        btnRow.addView(approveBtn)
        dialogView.addView(btnRow)

        dialog.show()
    }

    // ==========================================
    // ==========================================
    // WEBSITE CATEGORIES & 15 E-COMMERCE THEMES
    // ==========================================
    data class WebsiteCategory(
        val id: String,
        val icon: String,
        val title: String,
        val subtitle: String,
        val description: String,
        val bullets: List<String>,
        val badge: String,
        val demoUrl: String,
        val accentColor: String
    )

    private val ecommerceThemes = listOf(
        WebsiteCategory(
            id = "electronics",
            icon = "📱",
            title = "Electronics",
            subtitle = "Mobiles, Laptops, Audio, Smart Gadgets & Accessories",
            description = "High-tech electronics store with technical specifications, warranty badges and instant UPI checkout.",
            bullets = listOf("Tech Specs Comparison", "Warranty Badges", "EMI / UPI Checkout", "Brand Filtering"),
            badge = "Gadgets & Tech",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#00F0FF"
        ),
        WebsiteCategory(
            id = "fashion",
            icon = "👗",
            title = "Fashion & Apparel",
            subtitle = "Clothing, Footwear, Ethnic Wear, Bags & Accessories",
            description = "Trendy fashion store layout with size selectors, color swatches and image lookbooks.",
            bullets = listOf("Size & Color Selectors", "Lookbook Showcase", "Trendy Grid Layout", "WhatsApp Styling Help"),
            badge = "Clothing & Style",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html",
            accentColor = "#EC4899"
        ),
        WebsiteCategory(
            id = "health_medical",
            icon = "💊",
            title = "Health & Medical",
            subtitle = "Medicines, Wellness, Healthcare Devices & Supplements",
            description = "Clean, certified medical pharmacy and wellness store with dosage guidance and prescription upload.",
            bullets = listOf("Prescription Upload", "Dosage & Usage Info", "Verified Badges", "Express Delivery"),
            badge = "Pharmacy & Care",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#10B981"
        ),
        WebsiteCategory(
            id = "beauty_personal_care",
            icon = "💄",
            title = "Beauty & Personal Care",
            subtitle = "Skincare, Makeup, Haircare, Fragrances & Grooming",
            description = "Elegant cosmetic and skincare storefront with ingredient highlights and beauty routines.",
            bullets = listOf("Ingredient Highlights", "Skin Type Matching", "Customer Glow Reviews", "Bundle Discounts"),
            badge = "Cosmetics & Glow",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F43F5E"
        ),
        WebsiteCategory(
            id = "home_kitchen",
            icon = "🍳",
            title = "Home & Kitchen",
            subtitle = "Cookware, Kitchen Appliances, Dining & Home Essentials",
            description = "Modern culinary and home utility store highlighting material durability and kitchen dimensions.",
            bullets = listOf("Material & Capacity Specs", "Cooking Video Demos", "Cash on Delivery", "Replacement Guarantee"),
            badge = "Cookware & Dining",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F59E0B"
        ),
        WebsiteCategory(
            id = "grocery_food",
            icon = "🛒",
            title = "Grocery & Food",
            subtitle = "Daily Staples, Fresh Fruits, Vegetables, Snacks & Beverages",
            description = "Fast neighborhood grocery store with quick re-order, weight options and slot delivery.",
            bullets = listOf("Weight / Quantity Picker", "1-Tap Quick Reorder", "Freshness Guarantee", "Local Slot Delivery"),
            badge = "Daily Essentials",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#84CC16"
        ),
        WebsiteCategory(
            id = "automotive_accessories",
            icon = "🚗",
            title = "Automotive & Accessories",
            subtitle = "Car Parts, Bike Accessories, Helmets, Oils & Tools",
            description = "Rugged automotive storefront with vehicle make/model compatibility filter.",
            bullets = listOf("Vehicle Model Compatibility", "OEM Part Numbers", "Installation Guides", "Heavy Duty Shipping"),
            badge = "Auto Parts & Care",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#EF4444"
        ),
        WebsiteCategory(
            id = "industrial_supplies",
            icon = "🏭",
            title = "Industrial Supplies",
            subtitle = "Hardware, Power Tools, Safety Equipment & Machinery",
            description = "High-durability B2B industrial catalog with bulk tier pricing and technical datasheets.",
            bullets = listOf("Bulk Quantity Tiers", "Technical Data Sheets", "GST Invoicing", "Safety Standards"),
            badge = "Tools & Equipment",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#64748B"
        ),
        WebsiteCategory(
            id = "books_stationery",
            icon = "📚",
            title = "Books & Stationery",
            subtitle = "Educational Books, Novels, Art Supplies, Notebooks & Office Supplies",
            description = "Thoughtful bookstore and stationery catalog with author bios and sample reading previews.",
            bullets = listOf("Author & Publisher Bios", "Reading Level Filters", "School / Office Packs", "Fast Book Delivery"),
            badge = "Office & Reading",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#8B5CF6"
        ),
        WebsiteCategory(
            id = "sports_fitness",
            icon = "⚽",
            title = "Sports & Fitness",
            subtitle = "Gym Equipment, Activewear, Sports Gear & Nutrition",
            description = "Energetic sports and gym storefront with gear recommendations and workout categories.",
            bullets = listOf("Training Level Filters", "Equipment Dimensions", "Protein / Shaker Combos", "Durable Gear Warranty"),
            badge = "Gym & Athletics",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#06B6D4"
        ),
        WebsiteCategory(
            id = "baby_kids",
            icon = "👶",
            title = "Baby & Kids",
            subtitle = "Toys, Baby Care, Kids Clothing, Diapers & Nursery",
            description = "Playful and secure parenting store highlighting non-toxic materials and age suitability.",
            bullets = listOf("Age Group Filtering", "Non-Toxic Safety Badges", "Parent Gift Combos", "Gentle Baby Care"),
            badge = "Toys & Baby Care",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#FB7185"
        ),
        WebsiteCategory(
            id = "agriculture_farming",
            icon = "🌾",
            title = "Agriculture & Farming",
            subtitle = "Seeds, Fertilizers, Irrigation, Farm Tools & Animal Feed",
            description = "Kisan and farming friendly store with seasonal crop guides and direct WhatsApp ordering.",
            bullets = listOf("Seasonal Crop Guides", "Organic / Hybrid Seeds", "Direct Kisan WhatsApp Support", "Bulk Farm Shipping"),
            badge = "Seeds & Agro Tools",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#22C55E"
        ),
        WebsiteCategory(
            id = "pet_supplies",
            icon = "🐾",
            title = "Pet Supplies",
            subtitle = "Dog Food, Cat Care, Pet Toys, Grooming & Pet Health",
            description = "Pet lover storefront with breed size selectors and recurring subscription boxes.",
            bullets = listOf("Breed & Age Nutrition", "Treats & Toy Bundles", "Vet Approved Badges", "Doorstep Pet Care"),
            badge = "Dogs, Pets & Care",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F97316"
        ),
        WebsiteCategory(
            id = "furniture_home_decor",
            icon = "🛋️",
            title = "Furniture & Home Decor",
            subtitle = "Sofas, Beds, Lighting, Wall Art & Living Space Decor",
            description = "Aesthetic interior showroom with dimension blueprints and custom fabric inquiry.",
            bullets = listOf("Room Dimension Guides", "Fabric & Wood Swatches", "Assembly Assistance", "Safe Transit Packaging"),
            badge = "Living & Interior",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#A855F7"
        ),
        WebsiteCategory(
            id = "b2b_wholesale",
            icon = "📦",
            title = "B2B Wholesale",
            subtitle = "Bulk Orders, Wholesale Pricing, Dealer Inquiries & Quotations",
            description = "Wholesale trader platform with Minimum Order Quantities (MOQ) and instant quotation generator.",
            bullets = listOf("MOQ Bulk Quantity Selector", "Request Quote on WhatsApp", "GST Tax Invoicing", "Commercial Freight"),
            badge = "Bulk Order & Trade",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#3B82F6"
        ),
        WebsiteCategory(
            id = "vastu",
            icon = "🕉️",
            title = "वास्तु ब्रह्मांड (Vastu Shastra)",
            subtitle = "Self-Audit, 9x9 Mandala & Cosmic Grid Hub",
            description = "Scientific and traditional Vastu Shastra with interactive 9x9 mandala grid, 10-minute home self-audit score, digital guides shop and WhatsApp consultations.",
            bullets = listOf("9x9 Purusha Mandala", "10-Min Home Audit Score", "Sunlight & Airflow Science", "Digital Guides & Yantras"),
            badge = "Vedic Hub",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/vastu-demo.html",
            accentColor = "#E9B44C"
        )
    )

    data class MainCategoryItem(
        val id: String,
        val icon: String,
        val title: String,
        val subCountText: String,
        val badgeText: String,
        val accentColor: String
    )

    private val mainCategoryCatalog = listOf(
        MainCategoryItem(
            id = "vastu",
            icon = "🕉️",
            title = "Vastu & Spiritual Hub",
            subCountText = "4 Themes",
            badgeText = "Vedic Vastu",
            accentColor = "#E9B44C"
        ),
        MainCategoryItem(
            id = "ecommerce",
            icon = "🛒",
            title = "E-Commerce Online Store",
            subCountText = "16 Themes",
            badgeText = "16 Themes",
            accentColor = "#00F0FF"
        ),
        MainCategoryItem(
            id = "portfolio",
            icon = "💼",
            title = "Portfolio & Showcase",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#A78BFA"
        ),
        MainCategoryItem(
            id = "linktree",
            icon = "🔗",
            title = "Linktree Bio Page",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#10B981"
        ),
        MainCategoryItem(
            id = "digital_product",
            icon = "💻",
            title = "Digital Products & SaaS",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#F59E0B"
        ),
        MainCategoryItem(
            id = "blogger",
            icon = "✍️",
            title = "Personal Blogger & News",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#EC4899"
        ),
        MainCategoryItem(
            id = "shop",
            icon = "🏬",
            title = "Local Retail Shop & Dukaan",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#38BDF8"
        ),
        MainCategoryItem(
            id = "display_store",
            icon = "🏷️",
            title = "Product Showroom Catalog",
            subCountText = "4 Themes",
            badgeText = "4 Themes",
            accentColor = "#8B5CF6"
        )
    )

    private val portfolioThemes = listOf(
        WebsiteCategory(
            id = "portfolio_designer",
            icon = "🎨",
            title = "UI/UX & Designer",
            subtitle = "Designers, Illustrators & Creatives",
            description = "Showcase design case studies, Figma prototypes and client reviews.",
            bullets = listOf("Visual Case Studies", "Design Portfolio", "Client Reviews", "Hire Me Button"),
            badge = "UI/UX Design",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#A78BFA"
        ),
        WebsiteCategory(
            id = "portfolio_developer",
            icon = "💻",
            title = "Software Developer",
            subtitle = "Full-Stack Devs, Mobile & Cloud Engineers",
            description = "Display GitHub repos, tech stack, deployed projects and resume download.",
            bullets = listOf("Tech Stack Badges", "Live Demo Links", "Resume Download", "Contact Form"),
            badge = "Tech & Dev",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#38BDF8"
        ),
        WebsiteCategory(
            id = "portfolio_agency",
            icon = "🚀",
            title = "Creative Digital Agency",
            subtitle = "Studios, Media Houses & Dev Agencies",
            description = "Agency portfolio highlighting client testimonials, service packages and quotes.",
            bullets = listOf("Client Logos", "Service Packages", "Case Studies", "Schedule Meeting"),
            badge = "Agency",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#818CF8"
        ),
        WebsiteCategory(
            id = "portfolio_consultant",
            icon = "👔",
            title = "Consultant & Freelancer",
            subtitle = "Business Advisors, Coaches & Solopreneurs",
            description = "Personal advisory brand with booking calendar and client results.",
            bullets = listOf("Consultation Booking", "Client Testimonials", "Pricing Tiers", "WhatsApp Chat"),
            badge = "Consultant",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#34D399"
        )
    )

    private val linktreeThemes = listOf(
        WebsiteCategory(
            id = "linktree_creator",
            icon = "🌿",
            title = "Minimal Creator Bio",
            subtitle = "Clean 1-page mobile landing card for creators",
            description = "Fast, clean bio page with all social, video and content links.",
            bullets = listOf("Fast Mobile Landing", "Social Links", "YouTube / Insta", "Custom Colors"),
            badge = "Minimal Bio",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#10B981"
        ),
        WebsiteCategory(
            id = "linktree_whatsapp",
            icon = "💬",
            title = "WhatsApp Quick Contact",
            subtitle = "Direct 1-tap WhatsApp chat and inquiries",
            description = "Optimized for businesses where customers connect directly via WhatsApp.",
            bullets = listOf("1-Tap WhatsApp", "Quick Order Chat", "Catalog Link", "Store Location"),
            badge = "WhatsApp Card",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#22C55E"
        ),
        WebsiteCategory(
            id = "linktree_influencer",
            icon = "📸",
            title = "Influencer Bio Page",
            subtitle = "Instagram, YouTube, Brand Collabs & Affiliate",
            description = "Showcase your top sponsored links, promo codes and collaboration inquiries.",
            bullets = listOf("Brand Deals Email", "Discount Promo Codes", "Featured Video", "Follow Links"),
            badge = "Influencer",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F43F5E"
        ),
        WebsiteCategory(
            id = "linktree_business",
            icon = "💼",
            title = "Corporate Digital Card",
            subtitle = "Smart digital visiting card for business owners",
            description = "Share contact details, Google Map directions, UPI payment and company profile.",
            bullets = listOf("Save vCard to Phone", "UPI Payment QR", "Google Map Pin", "PDF Brochure"),
            badge = "Business Card",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#00F0FF"
        )
    )

    private val digitalThemes = listOf(
        WebsiteCategory(
            id = "digital_ebook",
            icon = "📄",
            title = "eBooks & PDF Guides",
            subtitle = "Books, Whitepapers, Research & Guides",
            description = "Instant UPI checkout with auto-generated secure download link.",
            bullets = listOf("Instant Download Link", "UPI Verification", "Zero Shipping", "PDF Watermarking"),
            badge = "eBooks & Guides",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F59E0B"
        ),
        WebsiteCategory(
            id = "digital_course",
            icon = "🎓",
            title = "Courses & Masterclasses",
            subtitle = "Workshops, Video Courses & Bootcamps",
            description = "Course syllabus curriculum, student testimonials and instant enrollment.",
            bullets = listOf("Curriculum Breakdown", "Student Reviews", "Live Zoom Links", "Certificate"),
            badge = "Courses",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#6366F1"
        ),
        WebsiteCategory(
            id = "digital_software",
            icon = "💻",
            title = "Software & Presets",
            subtitle = "Lightroom Presets, Scripts, Code & Plugins",
            description = "Deliver digital assets, license keys and zip files directly upon payment.",
            bullets = listOf("License Key Delivery", "Zip File Download", "Version Updates", "Setup Guide"),
            badge = "Software",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#0284C7"
        ),
        WebsiteCategory(
            id = "digital_templates",
            icon = "🗂️",
            title = "Notion & Canva Kits",
            subtitle = "Productivity Dashboards, Canva & Figma Kits",
            description = "Ready-to-duplicate digital templates with video walkthrough tutorial.",
            bullets = listOf("Duplicate in 1-Click", "Walkthrough Video", "Free Updates", "Community Access"),
            badge = "Templates",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#EC4899"
        )
    )

    private val blogThemes = listOf(
        WebsiteCategory(
            id = "blog_tech",
            icon = "🚀",
            title = "Tech & Startups",
            subtitle = "Coding, AI, Product & Entrepreneurship",
            description = "Clean markdown reading typography with code syntax highlighting.",
            bullets = listOf("Code Highlighting", "Tech Category Tags", "Newsletter Form", "Author Bio"),
            badge = "Tech & AI",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#38BDF8"
        ),
        WebsiteCategory(
            id = "blog_lifestyle",
            icon = "🧘",
            title = "Lifestyle & Wellness",
            subtitle = "Health, Mindset, Yoga & Daily Habits",
            description = "Warm visual reading layout with beautiful image banners and tips.",
            bullets = listOf("Image Rich Layout", "Reading Time Tags", "Recipe / Routine Cards", "Social Sharing"),
            badge = "Lifestyle",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#EC4899"
        ),
        WebsiteCategory(
            id = "blog_travel",
            icon = "✈️",
            title = "Travel & Stories",
            subtitle = "Travel Guides, Destinations & Photography",
            description = "Photo-centric journal with destination guides and packing lists.",
            bullets = listOf("Full-width Photo Hero", "Destination Guides", "Map Pins", "Gear Checklist"),
            badge = "Travel Journal",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F59E0B"
        ),
        WebsiteCategory(
            id = "blog_finance",
            icon = "📈",
            title = "Finance & Business",
            subtitle = "Stock Market, Crypto, Real Estate & Wealth",
            description = "Data-focused editorial with market charts and investment breakdowns.",
            bullets = listOf("Market Breakdown", "Key Takeaways", "Weekly Newsletter", "Disclaimer Notes"),
            badge = "Finance Insights",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#10B981"
        )
    )

    private val shopThemes = listOf(
        WebsiteCategory(
            id = "shop_kirana",
            icon = "🌾",
            title = "Kirana & Dukaan",
            subtitle = "Local Grocery, Staples & Daily Essentials",
            description = "Neighborhood store with instant WhatsApp ordering and doorstep delivery.",
            bullets = listOf("Quick WhatsApp Order", "Local Delivery Radius", "Item Price List", "Store Timings"),
            badge = "Local Kirana",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#84CC16"
        ),
        WebsiteCategory(
            id = "shop_bakery",
            icon = "🍰",
            title = "Bakery & Cafe",
            subtitle = "Cakes, Pastries, Fresh Breads & Coffee",
            description = "Custom cake design ordering with date selector and flavor choices.",
            bullets = listOf("Custom Cake Booking", "Eggless / Vegan Tags", "Order for Today", "Menu Card"),
            badge = "Bakery & Sweets",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#F59E0B"
        ),
        WebsiteCategory(
            id = "shop_boutique",
            icon = "👗",
            title = "Clothing Boutique",
            subtitle = "Sarees, Suits, Lehengas & Designer Wear",
            description = "Boutique showcase with WhatsApp inquiry and custom tailoring.",
            bullets = listOf("WhatsApp Style Chat", "Fabric Details", "Tailoring Option", "Video Call Shopping"),
            badge = "Boutique",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html",
            accentColor = "#EC4899"
        ),
        WebsiteCategory(
            id = "shop_chemist",
            icon = "🩺",
            title = "Medical & Chemist",
            subtitle = "Pharmacy, Health Devices & First Aid",
            description = "WhatsApp prescription sending and neighborhood express delivery.",
            bullets = listOf("Send Prescription on WA", "Express Home Delivery", "Verified Chemist", "Emergency Call"),
            badge = "Chemist",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#10B981"
        )
    )

    private val showroomThemes = listOf(
        WebsiteCategory(
            id = "display_furniture",
            icon = "🛋️",
            title = "Furniture Showroom",
            subtitle = "Sofas, Dining, Beds & Office Furniture",
            description = "Catalog display with WhatsApp quote and custom fabric selector.",
            bullets = listOf("Dimensions & Specs", "WhatsApp Quotation", "Showroom Address", "Custom Fabric"),
            badge = "Furniture",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#A855F7"
        ),
        WebsiteCategory(
            id = "display_jewelry",
            icon = "💎",
            title = "Jewelry Showroom",
            subtitle = "Gold, Diamonds, Polki & Silver Jewelry",
            description = "High-trust luxury catalog with certified purity badges and consultation.",
            bullets = listOf("Purity Certification", "Book Video Viewing", "Price on Request", "Showroom Visit"),
            badge = "Jewelry",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html",
            accentColor = "#EAB308"
        ),
        WebsiteCategory(
            id = "display_realestate",
            icon = "🏠",
            title = "Real Estate Showcase",
            subtitle = "Villas, Apartments, Plots & Commercial",
            description = "Property floor plans, amenities, price ranges and site visit booking.",
            bullets = listOf("Floor Plan Blueprint", "Site Visit Booking", "Amenities List", "Location Map"),
            badge = "Real Estate",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#0284C7"
        ),
        WebsiteCategory(
            id = "display_auto",
            icon = "🏎️",
            title = "Automobile Showroom",
            subtitle = "Cars, Bikes, Electric Vehicles & Pre-Owned",
            description = "Vehicle specs, test drive scheduling and WhatsApp EMI quotes.",
            bullets = listOf("Test Drive Booking", "Vehicle Mileage Specs", "EMI Calculator", "Brochure PDF"),
            badge = "Automobile",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html",
            accentColor = "#EF4444"
        )
    )

    private val vastuThemes = listOf(
        WebsiteCategory(
            id = "vastu_brahmand",
            icon = "🕉️",
            title = "वास्तु ब्रह्मांड (Vastu Brahmand)",
            subtitle = "Self-Audit, 9x9 Mandala & Cosmic Grid Explorer",
            description = "Scientific and traditional Vastu Shastra with interactive 9x9 mandala grid, 10-minute home self-audit score, digital guides shop and WhatsApp consultations.",
            bullets = listOf("9x9 Purusha Mandala", "10-Min Home Audit Score", "Sunlight & Airflow Science", "Digital Guides & Yantras"),
            badge = "Vedic Hub",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/vastu-demo.html",
            accentColor = "#E9B44C"
        ),
        WebsiteCategory(
            id = "vastu_consultation",
            icon = "📜",
            title = "Vastu Expert Consultation",
            subtitle = "Home, Factory, Office & Plot Readings",
            description = "Personalized map review for flats, factories, shops and upcoming plots with zero structural demolition remedies.",
            bullets = listOf("Room-by-Room Report", "Zero-Demolition Fixes", "WhatsApp Drawing Review", "Commercial & Residential"),
            badge = "Expert Reading",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/vastu-demo.html#expert",
            accentColor = "#F08A3C"
        ),
        WebsiteCategory(
            id = "vastu_shop",
            icon = "🔱",
            title = "Vedic Yantras & Sacred Remedies",
            subtitle = "Bhojpatra Yantras, Pyramids, Shivalingams & Brass Rods",
            description = "Prana-pratishthit spiritual tools, energised gemstones and brass Vastu energy rods with instant order on WhatsApp.",
            bullets = listOf("Certified Parad Items", "Energised Brass Rods", "Direct UPI Payment", "1-Tap WhatsApp Order"),
            badge = "Vedic Tools",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/demo.html?theme=vastu",
            accentColor = "#F6D58E"
        ),
        WebsiteCategory(
            id = "vastu_audit",
            icon = "🧭",
            title = "10-Minute Home Vastu Score Audit",
            subtitle = "Interactive Score Calculator & Instant Advice",
            description = "Audit tool assessing main entrance, kitchen, master bedroom, puja corner and toilet with immediate scoring and advice.",
            bullets = listOf("Instant Score out of 12", "Color Gradient Meter", "Practical Room Fixes", "Compass Direction Guide"),
            badge = "Self-Audit",
            demoUrl = "https://yuviteltechnologies-art.github.io/freewebsite/vastu-demo.html#audit",
            accentColor = "#6FD3A0"
        )
    )

    private var selectedMainCategoryForSubTab: String? = null

    private fun getSubThemesForCategory(catId: String): List<WebsiteCategory> {
        return when (catId.lowercase()) {
            "vastu" -> vastuThemes
            "ecommerce" -> ecommerceThemes
            "portfolio" -> portfolioThemes
            "linktree" -> linktreeThemes
            "digital_product" -> digitalThemes
            "blogger" -> blogThemes
            "shop" -> shopThemes
            "display_store" -> showroomThemes
            else -> ecommerceThemes
        }
    }

    private val websiteCategories: List<WebsiteCategory>
        get() = ecommerceThemes + vastuThemes + portfolioThemes + linktreeThemes + digitalThemes + blogThemes + shopThemes + showroomThemes

    private fun applyWebsiteCategory(category: WebsiteCategory) {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please configure Sheet URL first", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Applying '${category.title}' to Live Website... ⏳", Toast.LENGTH_SHORT).show()

        val mainCatId = selectedMainCategoryForSubTab
            ?: if (vastuThemes.any { it.id == category.id } || category.id == "vastu") "vastu"
            else if (ecommerceThemes.any { it.id == category.id }) "ecommerce"
            else if (portfolioThemes.any { it.id == category.id }) "portfolio"
            else if (linktreeThemes.any { it.id == category.id }) "linktree"
            else if (digitalThemes.any { it.id == category.id }) "digital_product"
            else if (blogThemes.any { it.id == category.id }) "blogger"
            else if (shopThemes.any { it.id == category.id }) "shop"
            else if (showroomThemes.any { it.id == category.id }) "display_store"
            else "ecommerce"

        val body = mapOf(
            "action" to "updateSettings",
            "website_category" to mainCatId,
            "ecommerce_theme" to category.id,
            "template_id" to category.id,
            "theme" to category.id
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                        s["website_category"] = mainCatId
                        s["ecommerce_theme"] = category.id
                        s["template_id"] = category.id
                        s["theme"] = category.id
                        dashboardData = dashboardData?.copy(settings = s)
                        getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("selected_website_category", mainCatId)
                            .putString("selected_ecommerce_theme", category.id)
                            .putString("selected_theme_id", category.id)
                            .apply()
                        refreshCurrentTab()
                        Toast.makeText(this@MainActivity, "🎉 '${category.title}' Live Website Par Apply Ho Gaya!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Response code: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                    s["website_category"] = mainCatId
                    s["ecommerce_theme"] = category.id
                    s["template_id"] = category.id
                    s["theme"] = category.id
                    dashboardData = dashboardData?.copy(settings = s)
                    getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("selected_website_category", mainCatId)
                        .putString("selected_ecommerce_theme", category.id)
                        .putString("selected_theme_id", category.id)
                        .apply()
                    refreshCurrentTab()
                    Toast.makeText(this@MainActivity, "✓ '${category.title}' locally set ho gaya!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyWebsiteTemplate(templateId: String, templateTitle: String) {
        val cat = websiteCategories.find { it.id == templateId }
        if (cat != null) {
            applyWebsiteCategory(cat)
            return
        }
        val url = getSheetUrl()
        if (url.isEmpty()) return
        val body = mapOf("action" to "updateSettings", "template_id" to templateId, "theme" to templateId)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.service.updateSettings(fullUrl = url, body = body).execute()
            } catch (_: Exception) {}
        }
    }

    private fun createMainCategoryCard(item: MainCategoryItem, isActive: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = UiKit.cardDrawable(
                bgColor = if (isActive) Color.parseColor("#0C202F") else Color.parseColor("#0E1626"),
                strokeColor = if (isActive) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                strokeWidthDp = if (isActive) 2 else 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            setPadding(
                UiKit.dp(this@MainActivity, 12),
                UiKit.dp(this@MainActivity, 14),
                UiKit.dp(this@MainActivity, 12),
                UiKit.dp(this@MainActivity, 14)
            )
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedMainCategoryForSubTab = item.id
                refreshCurrentTab()
            }
        }

        // Icon Box
        val iconBox = TextView(this).apply {
            text = item.icon
            textSize = 28f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor(item.accentColor),
                strokeWidthDp = 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(
                UiKit.dp(this@MainActivity, 48),
                UiKit.dp(this@MainActivity, 48)
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        card.addView(iconBox)

        // Title
        val titleView = TextView(this).apply {
            text = item.title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 4)
            }
        }
        card.addView(titleView)

        // Status Badge (Active or theme count)
        val statusBadge = TextView(this).apply {
            text = if (isActive) "✓ ACTIVE NOW" else item.badgeText
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setTextColor(if (isActive) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))
            background = UiKit.cardDrawable(
                bgColor = if (isActive) Color.parseColor("#063321") else Color.parseColor("#141D2E"),
                strokeColor = if (isActive) Color.parseColor("#10B981") else Color.parseColor("#25354F"),
                strokeWidthDp = 1,
                radiusDp = 4f,
                context = this@MainActivity
            )
            setPadding(
                UiKit.dp(this@MainActivity, 8),
                UiKit.dp(this@MainActivity, 3),
                UiKit.dp(this@MainActivity, 8),
                UiKit.dp(this@MainActivity, 3)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 10)
            }
        }
        card.addView(statusBadge)

        // Spacer
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        card.addView(spacer)

        // "Open Themes ›" Action Button
        val openBtn = Button(this).apply {
            text = "${item.subCountText} ›"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#1D4ED8"),
                radiusDp = 6f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 36)
            )
            setOnClickListener {
                selectedMainCategoryForSubTab = item.id
                refreshCurrentTab()
            }
        }
        card.addView(openBtn)

        return card
    }

    private fun createCategoryRectGridCard(cat: WebsiteCategory, isActive: Boolean): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = UiKit.cardDrawable(
                bgColor = if (isActive) Color.parseColor("#0C202F") else Color.parseColor("#0E1626"),
                strokeColor = if (isActive) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                strokeWidthDp = if (isActive) 2 else 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            setPadding(
                UiKit.dp(this@MainActivity, 12),
                UiKit.dp(this@MainActivity, 14),
                UiKit.dp(this@MainActivity, 12),
                UiKit.dp(this@MainActivity, 14)
            )
        }

        // 1. Icon Box
        val iconBox = TextView(this).apply {
            text = cat.icon
            textSize = 26f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor(cat.accentColor),
                strokeWidthDp = 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(
                UiKit.dp(this@MainActivity, 46),
                UiKit.dp(this@MainActivity, 46)
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        card.addView(iconBox)

        // 2. Theme Name
        val titleView = TextView(this).apply {
            text = cat.title
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            maxLines = 2
            minLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        card.addView(titleView)

        // 3. Status Tag / Badge
        val statusBadge = TextView(this).apply {
            text = if (isActive) "✓ ACTIVE NOW" else cat.badge
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setTextColor(if (isActive) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))
            background = UiKit.cardDrawable(
                bgColor = if (isActive) Color.parseColor("#063321") else Color.parseColor("#141D2E"),
                strokeColor = if (isActive) Color.parseColor("#10B981") else Color.parseColor("#25354F"),
                strokeWidthDp = 1,
                radiusDp = 4f,
                context = this@MainActivity
            )
            setPadding(
                UiKit.dp(this@MainActivity, 8),
                UiKit.dp(this@MainActivity, 3),
                UiKit.dp(this@MainActivity, 8),
                UiKit.dp(this@MainActivity, 3)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
        }
        card.addView(statusBadge)

        // Spacer to push buttons to the bottom evenly
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        card.addView(spacer)

        // 4. Demo Link Button
        val demoBtn = UiKit.createSecondaryButton(this, "🌐 Demo ↗", cat.accentColor) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cat.demoUrl)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Browser could not be opened", Toast.LENGTH_SHORT).show()
            }
        }.apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 34)
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 6)
            }
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F172A"),
                strokeColor = Color.parseColor(cat.accentColor),
                strokeWidthDp = 1,
                radiusDp = 6f,
                context = this@MainActivity
            )
            setPadding(0, 0, 0, 0)
        }
        card.addView(demoBtn)

        // 5. Apply Button
        val applyBtn = Button(this).apply {
            if (isActive) {
                text = "✓ Currently Active"
                setTextColor(Color.parseColor("#A7F3D0"))
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#064E3B"),
                    strokeColor = Color.parseColor("#10B981"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                isEnabled = false
            } else {
                text = "⚡ Apply"
                setTextColor(Color.WHITE)
                background = UiKit.gradientButtonDrawable(
                    startColor = Color.parseColor("#2563EB"),
                    endColor = Color.parseColor("#1D4ED8"),
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                isEnabled = true
                setOnClickListener {
                    applyWebsiteCategory(cat)
                }
            }
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 38)
            )
        }
        card.addView(applyBtn)

        return card
    }

    // SUB-TAB 2: WEBSITE CATEGORIES & SUB-THEMES (2-TIER DRILLDOWN FLOW)
    private fun createThemeSelectionSubTab(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 32))
        }

        val activeMainCatId = selectedMainCategoryForSubTab
        if (activeMainCatId == null) {
            // STATE 1: MAIN CATEGORIES SELECTION
            container.addView(createSubTabBackHeader(
                title = "🎨 2nd: Website Category Selection",
                subtitle = "Apne business type ke according main category select karein aur uske andar themes explore karein:"
            ))

            val infoBanner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#081524"),
                    strokeColor = Color.parseColor("#00F0FF"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 14)
                }
            }
            val infoTitle = TextView(this).apply {
                text = "💡 Choose Main Category"
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
            }
            val infoSub = TextView(this).apply {
                text = "Kisi bhi category par tap karein, uske andar ke specialized themes open ho jayenge (Jaise E-Commerce me 15 themes):"
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
            }
            infoBanner.addView(infoTitle)
            infoBanner.addView(infoSub)
            container.addView(infoBanner)

            val currentActiveMainCat = (dashboardData?.settings?.get("website_category")
                ?: getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE).getString("selected_website_category", "ecommerce")
                ?: "ecommerce").trim().lowercase()

            for (pair in mainCategoryCatalog.chunked(2)) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = UiKit.dp(this@MainActivity, 10)
                    }
                }

                val item1 = pair[0]
                val isActive1 = (currentActiveMainCat == item1.id.lowercase())
                val card1 = createMainCategoryCard(item1, isActive1).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 5)
                    }
                }
                row.addView(card1)

                if (pair.size > 1) {
                    val item2 = pair[1]
                    val isActive2 = (currentActiveMainCat == item2.id.lowercase())
                    val card2 = createMainCategoryCard(item2, isActive2).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                            leftMargin = UiKit.dp(this@MainActivity, 5)
                        }
                    }
                    row.addView(card2)
                } else {
                    val spacer = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                            leftMargin = UiKit.dp(this@MainActivity, 5)
                        }
                    }
                    row.addView(spacer)
                }

                container.addView(row)
            }
        } else {
            // STATE 2: SUB-THEMES OF SELECTED MAIN CATEGORY
            val mainCat = mainCategoryCatalog.find { it.id == activeMainCatId } ?: mainCategoryCatalog[0]

            val subHeader = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
            }
            val backToMainBtn = TextView(this).apply {
                text = "⬅️ Wapas Main Categories Par Jayein"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#00F0FF"))
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#0F1E36"),
                    strokeColor = Color.parseColor("#00F0FF"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 8)
                }
                setOnClickListener {
                    selectedMainCategoryForSubTab = null
                    refreshCurrentTab()
                }
            }
            subHeader.addView(backToMainBtn)

            val titleView = TextView(this).apply {
                text = "${mainCat.icon} ${mainCat.title} (${mainCat.subCountText})"
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            val subtitleView = TextView(this).apply {
                text = "Apne business ke hisab se theme select karein aur 1-click me live apply karein:"
                textSize = 11.5f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, UiKit.dp(this@MainActivity, 3), 0, 0)
            }
            subHeader.addView(titleView)
            subHeader.addView(subtitleView)
            container.addView(subHeader)

            val currentActiveTheme = (dashboardData?.settings?.get("ecommerce_theme")
                ?: dashboardData?.settings?.get("theme")
                ?: dashboardData?.settings?.get("template_id")
                ?: getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE).getString("selected_ecommerce_theme", null)
                ?: getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE).getString("selected_theme_id", "electronics")
                ?: "electronics").trim().lowercase()

            val themes = getSubThemesForCategory(activeMainCatId)
            for (pair in themes.chunked(2)) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = UiKit.dp(this@MainActivity, 10)
                    }
                }

                val cat1 = pair[0]
                val isActive1 = (currentActiveTheme == cat1.id.lowercase())
                val card1 = createCategoryRectGridCard(cat1, isActive1).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 5)
                    }
                }
                row.addView(card1)

                if (pair.size > 1) {
                    val cat2 = pair[1]
                    val isActive2 = (currentActiveTheme == cat2.id.lowercase())
                    val card2 = createCategoryRectGridCard(cat2, isActive2).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                            leftMargin = UiKit.dp(this@MainActivity, 5)
                        }
                    }
                    row.addView(card2)
                } else {
                    val spacer = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                            leftMargin = UiKit.dp(this@MainActivity, 5)
                        }
                    }
                    row.addView(spacer)
                }

                container.addView(row)
            }
        }

        scroll.addView(container)
        return scroll
    }

    // ==========================================
    // 5-BUTTON WEBSITE CMS & STUDIO SUB-TABS
    // ==========================================

    private fun createSubTabBackHeader(title: String, subtitle: String): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
        }

        val backBtn = TextView(this).apply {
            text = "⬅️ Wapas Studio Menu Par Jayein"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#00F0FF"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F1E36"),
                strokeColor = Color.parseColor("#00F0FF"),
                strokeWidthDp = 1,
                radiusDp = 6f, // RECTANGULAR BOX
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 9), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 9))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
            setOnClickListener {
                syncVisibleInputsToPending()
                designSubTabIndex = -1
                refreshCurrentTab()
            }
        }
        container.addView(backBtn)

        val titleView = TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subView = TextView(this).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, 0)
        }
        container.addView(titleView)
        container.addView(subView)

        return container
    }

    // ==========================================
    // EXCEL / SPREADSHEET INPUT COMPONENT HELPERS
    // ==========================================
    private fun createExcelStyledInput(hintText: String, initialVal: String, inputType: Int = InputType.TYPE_CLASS_TEXT, multiline: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            setHintTextColor(Color.parseColor("#4B5563"))
            setText(initialVal)
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            this.inputType = if (multiline) (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE) else inputType
            if (multiline) {
                minLines = 2
                maxLines = 4
                gravity = Gravity.TOP or Gravity.START
            }
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#060B14"),
                strokeColor = Color.parseColor("#1F2D44"),
                strokeWidthDp = 1,
                radiusDp = 6f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createExcelRow(
        cellRef: String,
        title: String,
        subHint: String?,
        inputView: View,
        actionView: View? = null,
        previewView: View? = null,
        onSaveRow: (() -> Unit)? = null
    ): View {
        val rowContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0E1626"),
                strokeColor = Color.parseColor("#1B2940"),
                strokeWidthDp = 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 10)
            }
        }

        // Header: Monospace Excel Cell Tag [A1] + Title (weight 1) + [💾 Save] button
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 6)
            }
        }

        val cellTag = TextView(this).apply {
            text = " $cellRef "
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#10B981"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#062419"),
                strokeColor = Color.parseColor("#10B981"),
                strokeWidthDp = 1,
                radiusDp = 4f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 2), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 2))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        headerLayout.addView(cellTag)

        val titleView = TextView(this).apply {
            text = title
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        headerLayout.addView(titleView)

        if (onSaveRow != null) {
            val saveBtn = Button(this).apply {
                text = "💾 Save"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#A7F3D0"))
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#064E3B"),
                    strokeColor = Color.parseColor("#10B981"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 4))
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this@MainActivity, 32))
                setOnClickListener { onSaveRow() }
            }
            headerLayout.addView(saveBtn)
        }

        rowContainer.addView(headerLayout)

        // Main input view
        rowContainer.addView(inputView)

        // Optional action button (e.g. Gallery pick)
        if (actionView != null) {
            rowContainer.addView(actionView)
        }

        // Optional preview view (e.g. Image Preview)
        if (previewView != null) {
            rowContainer.addView(previewView)
        }

        // Sub hint
        if (!subHint.isNullOrEmpty()) {
            val hintView = TextView(this).apply {
                text = "↳ $subHint"
                textSize = 10.5f
                setTextColor(Color.parseColor("#64748B"))
                setPadding(0, UiKit.dp(this@MainActivity, 4), 0, 0)
            }
            rowContainer.addView(hintView)
        }

        return rowContainer
    }

    private fun saveIndividualSetting(key: String, value: String, label: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Pehle Google Sheet URL set karein", Toast.LENGTH_SHORT).show()
            return
        }

        pendingSettings[key] = value
        val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
        s[key] = value
        dashboardData = dashboardData?.copy(settings = s)

        Toast.makeText(this, "Saving '$label'... ⏳", Toast.LENGTH_SHORT).show()

        val body = mapOf(
            "action" to "updateSettings",
            key to value
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val res = call.execute()
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        Toast.makeText(this@MainActivity, "🎉 '$label' live Google Sheet par save ho gaya!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Save error: HTTP ${res.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✓ Local me update ho gaya (${e.localizedMessage})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveMultipleSettings(entries: Map<String, String>, label: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Pehle Google Sheet URL set karein", Toast.LENGTH_SHORT).show()
            return
        }

        for ((k, v) in entries) {
            pendingSettings[k] = v
        }
        val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
        s.putAll(entries)
        dashboardData = dashboardData?.copy(settings = s)

        Toast.makeText(this, "Saving '$label'... ⏳", Toast.LENGTH_SHORT).show()

        val body = mutableMapOf<String, String>()
        body["action"] = "updateSettings"
        body.putAll(entries)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val res = call.execute()
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful) {
                        Toast.makeText(this@MainActivity, "🎉 '$label' live Google Sheet par save ho gaya!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Save error: HTTP ${res.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "✓ Local me update ho gaya (${e.localizedMessage})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPreviewImage(urlOrB64: String, target: ImageView) {
        if (urlOrB64.isEmpty()) {
            target.visibility = View.GONE
            return
        }
        target.visibility = View.VISIBLE
        if (urlOrB64.startsWith("data:image")) {
            try {
                val b64 = urlOrB64.substringAfter("base64,")
                val decoded = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                target.setImageBitmap(bmp)
            } catch (_: Exception) {}
        } else if (urlOrB64.startsWith("http://") || urlOrB64.startsWith("https://")) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val input = java.net.URL(urlOrB64).openStream()
                    val bmp = BitmapFactory.decodeStream(input)
                    withContext(Dispatchers.Main) {
                        target.setImageBitmap(bmp)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // SUB-TAB 1: SITE MAIN DETAILS & CONTACT DIRECTORY (EXCEL SHEET INPUT STYLE WITH INDIVIDUAL SAVE BUTTONS)
    private fun createSiteMainDetailsSubTab(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 36))
        }

        form.addView(createSubTabBackHeader("🏢 1st: Site Main Details & Contact Directory", "Dukaan ki sabhi details yahan enter karein. Har row ke aage diye Save button se individual save karein:"))

        // Top Google Sheet Banner
        val sheetHeaderCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#061A12"),
                strokeColor = Color.parseColor("#059669"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
        }

        val sheetTitle = TextView(this).apply {
            text = "📗 Google Sheets Mode: Store & Contact Directory"
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#34D399"))
        }
        val formulaBar = TextView(this).apply {
            text = "fx | =STORE_DIRECTORY(Row, Field_Value)"
            textSize = 10.5f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
        }
        val sheetSub = TextView(this).apply {
            text = "Excel grid format: Jiske paas jo information ho wo daal kar uske aage bane Save button se turant save kar sakta hai:"
            textSize = 11f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, 0)
        }
        sheetHeaderCard.addView(sheetTitle)
        sheetHeaderCard.addView(formulaBar)
        sheetHeaderCard.addView(sheetSub)
        form.addView(sheetHeaderCard)

        // [A1] Store / Brand Name
        val initialBrand = pendingSettings["brand_name"] ?: dashboardData?.settings?.get("brand_name") ?: ""
        brandNameInput = createExcelStyledInput("e.g. SheetStore Pro ya Apni Dukaan Ka Naam", initialBrand)
        form.addView(createExcelRow(
            cellRef = "A1",
            title = "Store / Brand Name",
            subHint = "Website aur invoice me sabse upar dikhne wala store title.",
            inputView = brandNameInput,
            onSaveRow = {
                val v = brandNameInput.text.toString().trim()
                saveIndividualSetting("brand_name", v, "Brand Name")
            }
        ))

        // [A2] Brand Tagline
        val initialTagline = pendingSettings["brand_tagline"] ?: dashboardData?.settings?.get("brand_tagline") ?: ""
        taglineInput = createExcelStyledInput("e.g. 100% Quality Guaranteed • Best Prices", initialTagline)
        form.addView(createExcelRow(
            cellRef = "A2",
            title = "Brand Tagline",
            subHint = "Brand name ke niche dikhne wali short punchline.",
            inputView = taglineInput,
            onSaveRow = {
                val v = taglineInput.text.toString().trim()
                saveIndividualSetting("brand_tagline", v, "Brand Tagline")
            }
        ))

        // [A3] Logo Photo
        val initialLogo = pendingSettings["logo_url"] ?: dashboardData?.settings?.get("logo_url") ?: ""
        logoUrlInput = createExcelStyledInput("https://example.com/logo.png ya niche gallery se chunein", initialLogo)
        val pickLogoBtn = UiKit.createSecondaryButton(this, "📁 Phone Gallery Se Logo Chunein (300x300 px)", "#10B981") {
            openImagePicker(REQUEST_PICK_LOGO)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 38)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        liveLogoPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 70), UiKit.dp(this@MainActivity, 70)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
            background = UiKit.cardDrawable(Color.parseColor("#070E1A"), Color.parseColor("#1F2D44"), 1, 8f, this@MainActivity)
        }
        loadPreviewImage(initialLogo, liveLogoPreview!!)
        form.addView(createExcelRow(
            cellRef = "A3",
            title = "Store Official Logo (300x300 px)",
            subHint = "Website header, invoices & branding me dikhne wala store logo.",
            inputView = logoUrlInput,
            actionView = pickLogoBtn,
            previewView = liveLogoPreview,
            onSaveRow = {
                val v = logoUrlInput.text.toString().trim()
                saveIndividualSetting("logo_url", v, "Store Logo")
            }
        ))

        // [A4] WhatsApp Order Number
        val initialWa = pendingSettings["whatsapp_number"] ?: dashboardData?.settings?.get("whatsapp_number") ?: ""
        waNumberInput = createExcelStyledInput("e.g. 919876543210 (Country code sahit)", initialWa, InputType.TYPE_CLASS_PHONE)
        form.addView(createExcelRow(
            cellRef = "A4",
            title = "Primary WhatsApp Order Number",
            subHint = "Naye website orders ka WhatsApp alert is number par seedha prapt hoga.",
            inputView = waNumberInput,
            onSaveRow = {
                val v = waNumberInput.text.toString().trim()
                saveIndividualSetting("whatsapp_number", v, "WhatsApp Number")
            }
        ))

        // [A5] Alternative Contact Number
        val initialAlt = pendingSettings["alt_phone"] ?: dashboardData?.settings?.get("alt_phone") ?: ""
        altPhoneInput = createExcelStyledInput("e.g. 919811223344", initialAlt, InputType.TYPE_CLASS_PHONE)
        form.addView(createExcelRow(
            cellRef = "A5",
            title = "Alternative Contact Number",
            subHint = "Backup mobile number agar primary number busy ya unavailable ho.",
            inputView = altPhoneInput,
            onSaveRow = {
                val v = altPhoneInput.text.toString().trim()
                saveIndividualSetting("alt_phone", v, "Alternative Number")
            }
        ))

        // [A6] Customer Care Helpline Number
        val initialHelp = pendingSettings["helpline_number"] ?: dashboardData?.settings?.get("helpline_number") ?: ""
        helplineInput = createExcelStyledInput("e.g. 1800-123-4567 ya 919876543210", initialHelp, InputType.TYPE_CLASS_PHONE)
        form.addView(createExcelRow(
            cellRef = "A6",
            title = "Customer Care Helpline Number",
            subHint = "Support helpline number website header & footer me dikhaya jayega.",
            inputView = helplineInput,
            onSaveRow = {
                val v = helplineInput.text.toString().trim()
                saveIndividualSetting("helpline_number", v, "Helpline Number")
            }
        ))

        // [A7] Official Business Email ID
        val initialEmail = pendingSettings["business_email"] ?: dashboardData?.settings?.get("business_email") ?: ""
        businessEmailInput = createExcelStyledInput("e.g. support@mystore.com", initialEmail, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        form.addView(createExcelRow(
            cellRef = "A7",
            title = "Official Business Email ID",
            subHint = "Official email address customer inquiries & receipts ke liye.",
            inputView = businessEmailInput,
            onSaveRow = {
                val v = businessEmailInput.text.toString().trim()
                saveIndividualSetting("business_email", v, "Business Email")
            }
        ))

        // [A8] Shop / Business Physical Address
        val initialAddress = pendingSettings["business_address"] ?: dashboardData?.settings?.get("business_address") ?: ""
        businessAddressInput = createExcelStyledInput("Shop No, Street, Landmark, City, State, Pincode", initialAddress, multiline = true)
        form.addView(createExcelRow(
            cellRef = "A8",
            title = "Shop / Business Physical Address",
            subHint = "Website contact section aur billing invoices par dikhne wala address.",
            inputView = businessAddressInput,
            onSaveRow = {
                val v = businessAddressInput.text.toString().trim()
                saveIndividualSetting("business_address", v, "Business Address")
            }
        ))

        // [A9] Google Maps Location Link
        val initialMap = pendingSettings["map_url"] ?: dashboardData?.settings?.get("map_url") ?: ""
        mapUrlInput = createExcelStyledInput("e.g. https://maps.google.com/?q=28.6139,77.2090", initialMap)
        form.addView(createExcelRow(
            cellRef = "A9",
            title = "Google Maps Location Link",
            subHint = "Customer tap karke seedha Google Maps par aapki shop navigate kar sakein.",
            inputView = mapUrlInput,
            onSaveRow = {
                val v = mapUrlInput.text.toString().trim()
                saveIndividualSetting("map_url", v, "Google Maps Link")
            }
        ))

        // [A10] Store UPI ID
        val initialUpi = pendingSettings["upi_id"] ?: dashboardData?.settings?.get("upi_id") ?: ""
        upiIdInput = createExcelStyledInput("e.g. merchant@okaxis ya store@upi", initialUpi)
        form.addView(createExcelRow(
            cellRef = "A10",
            title = "Store UPI ID (Where Money is Received)",
            subHint = "Checkout ke dauran customer is UPI ID par direct payment bhejte hain.",
            inputView = upiIdInput,
            onSaveRow = {
                val v = upiIdInput.text.toString().trim()
                saveIndividualSetting("upi_id", v, "Store UPI ID")
            }
        ))

        // [A11] Payment UPI QR Code Photo
        val initialQr = pendingSettings["qr_code_url"] ?: dashboardData?.settings?.get("qr_code_url") ?: ""
        qrCodeUrlInput = createExcelStyledInput("https://example.com/qr.png ya niche gallery se chunein", initialQr)
        val pickQrBtn = UiKit.createSecondaryButton(this, "📁 Phone Gallery Se UPI QR Chunein", "#10B981") {
            openImagePicker(REQUEST_PICK_QR)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 38)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        liveQrPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 100), UiKit.dp(this@MainActivity, 100)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
            background = UiKit.cardDrawable(Color.parseColor("#070E1A"), Color.parseColor("#1F2D44"), 1, 8f, this@MainActivity)
        }
        loadPreviewImage(initialQr, liveQrPreview!!)
        form.addView(createExcelRow(
            cellRef = "A11",
            title = "Payment UPI QR Code Photo",
            subHint = "BharatPe / Paytm / PhonePe / Custom QR code checkout screen ke liye.",
            inputView = qrCodeUrlInput,
            actionView = pickQrBtn,
            previewView = liveQrPreview,
            onSaveRow = {
                val v = qrCodeUrlInput.text.toString().trim()
                saveIndividualSetting("qr_code_url", v, "Payment QR Code")
            }
        ))

        // [A12] Website Contact Us Form Switch
        val showForm = pendingSettings["show_contact_form"] ?: dashboardData?.settings?.get("show_contact_form") ?: "true"
        contactFormSwitch = Switch(this).apply {
            text = "Show Interactive Contact Form On Website"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            isChecked = showForm.equals("true", ignoreCase = true)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        form.addView(createExcelRow(
            cellRef = "A12",
            title = "Website Contact Us Form (Toggle)",
            subHint = "Active hone par website footer me lead / inquiry contact form display hoga.",
            inputView = contactFormSwitch,
            onSaveRow = {
                val v = if (contactFormSwitch.isChecked) "true" else "false"
                saveIndividualSetting("show_contact_form", v, "Contact Form Toggle")
            }
        ))

        // [A13] Top Announcement Bar
        val annContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val showAnn = pendingSettings["show_announcement"] ?: dashboardData?.settings?.get("show_announcement") ?: "true"
        announcementSwitch = Switch(this).apply {
            text = "Show Top Announcement Bar Strip"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            isChecked = showAnn.equals("true", ignoreCase = true)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        annContainer.addView(announcementSwitch)

        val initialAnn = pendingSettings["announcement_text"] ?: dashboardData?.settings?.get("announcement_text") ?: ""
        announcementInput = createExcelStyledInput("e.g. Diwali Dhamaka! Flat 50% OFF on all items", initialAnn)
        annContainer.addView(announcementInput)

        form.addView(createExcelRow(
            cellRef = "A13",
            title = "Top Announcement Strip & Offer Text",
            subHint = "Website ke sabse upar scroll hone wali promo offer strip.",
            inputView = annContainer,
            onSaveRow = {
                val show = if (announcementSwitch.isChecked) "true" else "false"
                val text = announcementInput.text.toString().trim()
                saveMultipleSettings(mapOf("show_announcement" to show, "announcement_text" to text), "Announcement Bar")
            }
        ))

        // [A14] CTA Button Text
        val initialCta = pendingSettings["cta_text"] ?: dashboardData?.settings?.get("cta_text") ?: ""
        ctaTextInput = createExcelStyledInput("e.g. Shop Collection / Order On WhatsApp", initialCta)
        form.addView(createExcelRow(
            cellRef = "A14",
            title = "Primary Call-To-Action (CTA) Button Text",
            subHint = "Hero banner aur shopping card ke main action button par likha text.",
            inputView = ctaTextInput,
            onSaveRow = {
                val v = ctaTextInput.text.toString().trim()
                saveIndividualSetting("cta_text", v, "CTA Button Text")
            }
        ))

        // [A15] Hero Promo Tagline / Badge
        val initialBadge = pendingSettings["hero_badge"] ?: dashboardData?.settings?.get("hero_badge") ?: ""
        heroBadgeInput = createExcelStyledInput("e.g. ⚡ High-Tech Electronics & Smart Gadgets Store", initialBadge)
        form.addView(createExcelRow(
            cellRef = "A15",
            title = "Hero Tagline / Promo Pill Badge",
            subHint = "Headline ke upar pill box me dikhne wala offer / brand tag.",
            inputView = heroBadgeInput,
            onSaveRow = {
                val v = heroBadgeInput.text.toString().trim()
                saveIndividualSetting("hero_badge", v, "Hero Badge Text")
            }
        ))

        // [A16] Hero Main Headline
        val initialHeroTitle = pendingSettings["hero_title"] ?: dashboardData?.settings?.get("hero_title") ?: ""
        heroTitleInput = createExcelStyledInput("e.g. Next-Gen Tech & Electronics At Genuine Direct Prices", initialHeroTitle, multiline = true)
        form.addView(createExcelRow(
            cellRef = "A16",
            title = "Hero Main Headline",
            subHint = "Website home page ka sabse bada primary headline text.",
            inputView = heroTitleInput,
            onSaveRow = {
                val v = heroTitleInput.text.toString().trim()
                saveIndividualSetting("hero_title", v, "Hero Headline")
            }
        ))

        // [A17] Hero Subheadline / Description
        val initialHeroSub = pendingSettings["hero_subtitle"] ?: dashboardData?.settings?.get("hero_subtitle") ?: ""
        heroSubtitleInput = createExcelStyledInput("e.g. Original brand warranties, fastest EMI & 0% fee UPI checkout.", initialHeroSub, multiline = true)
        form.addView(createExcelRow(
            cellRef = "A17",
            title = "Hero Subheadline / Description",
            subHint = "Headline ke niche store aur offers ka detail description text.",
            inputView = heroSubtitleInput,
            onSaveRow = {
                val v = heroSubtitleInput.text.toString().trim()
                saveIndividualSetting("hero_subtitle", v, "Hero Subheadline")
            }
        ))

        // [A18] Hero Background Banner Photo
        val initialBanner = pendingSettings["banner_url"] ?: dashboardData?.settings?.get("banner_url") ?: dashboardData?.settings?.get("hero_banner_url") ?: ""
        bannerUrlInput = createExcelStyledInput("https://example.com/banner.jpg ya niche gallery se chunein", initialBanner)
        val pickBannerBtn = UiKit.createSecondaryButton(this, "📁 Phone Gallery Se Banner Chunein (1200x500 px)", "#10B981") {
            openImagePicker(REQUEST_PICK_BANNER)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 38)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        liveBannerPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 90)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
            background = UiKit.cardDrawable(Color.parseColor("#070E1A"), Color.parseColor("#1F2D44"), 1, 8f, this@MainActivity)
        }
        loadPreviewImage(initialBanner, liveBannerPreview!!)
        form.addView(createExcelRow(
            cellRef = "A18",
            title = "Hero Background Banner Photo (1200x500 px)",
            subHint = "Website ke hero section ka background cover photo.",
            inputView = bannerUrlInput,
            actionView = pickBannerBtn,
            previewView = liveBannerPreview,
            onSaveRow = {
                val v = bannerUrlInput.text.toString().trim()
                saveMultipleSettings(mapOf("banner_url" to v, "hero_banner_url" to v), "Hero Banner Photo")
            }
        ))

        // Live Website Store Links Card
        val (webCard, webBody) = UiKit.createSectionContainer(this, "🌐 Live Website Store Links", "Online")
        val webUrl = "https://ayush131102.github.io/sheetstore/"
        val urlBox = TextView(this).apply {
            text = "🔗 $webUrl"
            textSize = 12.5f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor("#38BDF8"))
            background = UiKit.cardDrawable(Color.parseColor("#080D1A"), Color.parseColor("#1E293B"), 1, 8f, this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10))
        }
        webBody.addView(urlBox)

        val rowBtns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = UiKit.dp(this@MainActivity, 10)
            }
        }
        val openWebBtn = UiKit.createSecondaryButton(this, "🌐 Open Store", "#00F0FF") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
            } catch (e: Exception) {
                Toast.makeText(this, "Browser open karne me error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        val copyWebBtn = UiKit.createSecondaryButton(this, "📋 Copy Link", "#A78BFA") {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Website URL", webUrl)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Website URL clipboard par copy ho gaya! 📋", Toast.LENGTH_SHORT).show()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        rowBtns.addView(openWebBtn)
        rowBtns.addView(copyWebBtn)
        webBody.addView(rowBtns)
        form.addView(webCard)

        // Bulk Save Button (Excel emerald green gradient)
        val saveAllBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "💾 Save All 18 Fields Together (Bulk Live Sync)",
            startColor = "#059669",
            endColor = "#10B981"
        ) {
            saveWebsiteSettings()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 48)
            ).apply {
                topMargin = UiKit.dp(this@MainActivity, 12)
                bottomMargin = UiKit.dp(this@MainActivity, 20)
            }
        }
        form.addView(saveAllBtn)

        scroll.addView(form)
        return scroll
    }

    // SUB-TAB 3: FONTS & STYLING INFORMATION
    private fun createFontsAndInfoSubTab(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 36))
        }

        form.addView(createSubTabBackHeader("🔤 3rd: Fonts & Styling Information", "Heading fonts, website color themes aur hero headlines ko customise karein:"))

        // 1. Fonts & Custom Colors
        val (styleCard, styleBody) = UiKit.createSectionContainer(this, "🎨 Fonts & Colors", "Visuals")
        styleBody.addView(UiKit.createFieldLabel(this, "Heading Font Family (Tap Chip to Apply):"))
        val initialFont = pendingSettings["font_family"] ?: dashboardData?.settings?.get("font_family") ?: "Outfit"
        fontFamilyInput = UiKit.createStyledInput(this, "Outfit", initialFont)
        styleBody.addView(fontFamilyInput)

        val fontChips = createFontChips { fontName, label ->
            fontFamilyInput.setText(fontName)
            pendingSettings["font_family"] = fontName
            Toast.makeText(this, "Selected font: $label", Toast.LENGTH_SHORT).show()
        }
        styleBody.addView(fontChips)

        styleBody.addView(UiKit.createFieldLabel(this, "Header Background Color Hex:"))
        val initialHeaderColor = pendingSettings["header_color"] ?: dashboardData?.settings?.get("header_color") ?: "#080C1A"
        headerColorInput = UiKit.createStyledInput(this, "#080C1A", initialHeaderColor)
        styleBody.addView(headerColorInput)
        styleBody.addView(createColorChips { hex, name ->
            headerColorInput.setText(hex)
            pendingSettings["header_color"] = hex
            Toast.makeText(this, "Header: $name", Toast.LENGTH_SHORT).show()
        })

        styleBody.addView(UiKit.createFieldLabel(this, "Footer Background Color Hex:"))
        val initialFooterColor = pendingSettings["footer_color"] ?: dashboardData?.settings?.get("footer_color") ?: "#050811"
        footerColorInput = UiKit.createStyledInput(this, "#050811", initialFooterColor)
        styleBody.addView(footerColorInput)
        styleBody.addView(createColorChips { hex, name ->
            footerColorInput.setText(hex)
            pendingSettings["footer_color"] = hex
            Toast.makeText(this, "Footer: $name", Toast.LENGTH_SHORT).show()
        })
        form.addView(styleCard)

        // 2. Hero Headlines & Content
        val (heroCard, heroBody) = UiKit.createSectionContainer(this, "🚀 Hero Headlines & Content", "Showcase")
        heroBody.addView(UiKit.createFieldLabel(this, "Hero Main Headline:"))
        val initialHeroTitle = pendingSettings["hero_title"] ?: dashboardData?.settings?.get("hero_title") ?: ""
        heroTitleInput = UiKit.createStyledInput(this, "Main headline on website", initialHeroTitle, singleLine = false)
        heroBody.addView(heroTitleInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Hero Subtitle / Description:"))
        val initialHeroSub = pendingSettings["hero_subtitle"] ?: dashboardData?.settings?.get("hero_subtitle") ?: ""
        heroSubtitleInput = UiKit.createStyledInput(this, "Secondary explanation text", initialHeroSub, singleLine = false)
        heroBody.addView(heroSubtitleInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Hero Badge Text (e.g. ⚡ 50% OFF TODAY):"))
        val initialHeroBadge = pendingSettings["hero_badge"] ?: dashboardData?.settings?.get("hero_badge") ?: ""
        heroBadgeInput = UiKit.createStyledInput(this, "Badge label text", initialHeroBadge)
        heroBody.addView(heroBadgeInput)
        form.addView(heroCard)

        // Save Button
        val saveBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "🚀 Save Fonts & Info Live",
            startColor = "#2563EB",
            endColor = "#00F0FF"
        ) {
            saveWebsiteSettings()
        }
        form.addView(saveBtn)

        scroll.addView(form)
        return scroll
    }

    // SUB-TAB 4: WHOLE WEBSITE IMAGE UPDATER
    private fun createImageUpdaterSubTab(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 36))
        }

        form.addView(createSubTabBackHeader("🖼️ 4th: Whole Website Image Updater", "Website ke Brand Logo aur Hero Banner photos ko gallery se upload karein ya direct URL update karein (Dimensions mentioned):"))

        // 1. Logo Updater
        val (logoCard, logoBody) = UiKit.createSectionContainer(this, "🖼️ 1. Store Brand Logo", "Square 300x300")

        val logoSizeBadge = TextView(this).apply {
            text = "📐 Perfect Dimensions: 300 x 300 px (Square PNG / JPG)\nWebsite ke header aur footer me crisp & clear dikhega."
            textSize = 11.5f
            setTextColor(Color.parseColor("#38BDF8"))
            background = UiKit.cardDrawable(Color.parseColor("#0C2038"), Color.parseColor("#1D4ED8"), 1, 8f, this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8))
        }
        logoBody.addView(logoSizeBadge)

        logoBody.addView(UiKit.createFieldLabel(this, "Logo Image URL (ya niche Gallery se chunein):"))
        val initialLogo = pendingSettings["logo_url"] ?: dashboardData?.settings?.get("logo_url") ?: ""
        logoUrlInput = UiKit.createStyledInput(this, "https://example.com/logo.png", initialLogo)
        logoBody.addView(logoUrlInput)

        liveLogoPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 90), UiKit.dp(this@MainActivity, 90)).apply {
                topMargin = UiKit.dp(this@MainActivity, 8)
                bottomMargin = UiKit.dp(this@MainActivity, 8)
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = UiKit.cardDrawable(Color.parseColor("#0C2038"), Color.parseColor("#1D4ED8"), 1, 10f, this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6))
        }
        if (initialLogo.startsWith("data:image")) {
            try {
                val b64 = initialLogo.substringAfter("base64,")
                val decoded = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                liveLogoPreview?.setImageBitmap(bmp)
            } catch (_: Exception) {}
        }
        logoBody.addView(liveLogoPreview)

        val pickLogoBtn = UiKit.createSecondaryButton(this, "📁 Phone Gallery Se Logo Chunein (300x300 px)", "#38BDF8") {
            openImagePicker(REQUEST_PICK_LOGO)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 44)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        logoBody.addView(pickLogoBtn)
        form.addView(logoCard)

        // 2. Banner Updater
        val (bannerCard, bannerBody) = UiKit.createSectionContainer(this, "🖼️ 2. Hero Banner Showcase", "Landscape 1200x500")

        val bannerSizeBadge = TextView(this).apply {
            text = "📐 Perfect Dimensions: 1200 x 500 px (Landscape Banner)\nHero section me top products & offers showcase karne ke liye."
            textSize = 11.5f
            setTextColor(Color.parseColor("#A78BFA"))
            background = UiKit.cardDrawable(Color.parseColor("#1E1138"), Color.parseColor("#6D28D9"), 1, 8f, this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8))
        }
        bannerBody.addView(bannerSizeBadge)

        bannerBody.addView(UiKit.createFieldLabel(this, "Hero Banner Image URL (ya niche Gallery se chunein):"))
        val initialBanner = pendingSettings["banner_url"] ?: dashboardData?.settings?.get("banner_url") ?: ""
        bannerUrlInput = UiKit.createStyledInput(this, "https://example.com/banner.jpg", initialBanner)
        bannerBody.addView(bannerUrlInput)

        liveBannerPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 120)).apply {
                topMargin = UiKit.dp(this@MainActivity, 8)
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
            background = UiKit.cardDrawable(Color.parseColor("#1E1138"), Color.parseColor("#6D28D9"), 1, 10f, this@MainActivity)
        }
        if (initialBanner.startsWith("data:image")) {
            try {
                val b64 = initialBanner.substringAfter("base64,")
                val decoded = Base64.decode(b64, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                liveBannerPreview?.setImageBitmap(bmp)
            } catch (_: Exception) {}
        }
        bannerBody.addView(liveBannerPreview)

        val pickBannerBtn = UiKit.createSecondaryButton(this, "📁 Phone Gallery Se Banner Chunein (1200x500 px)", "#A78BFA") {
            openImagePicker(REQUEST_PICK_BANNER)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 44)).apply {
                topMargin = UiKit.dp(this@MainActivity, 6)
            }
        }
        bannerBody.addView(pickBannerBtn)
        form.addView(bannerCard)

        // 3. UPI QR Code Info
        val (qrCard, qrBody) = UiKit.createSectionContainer(this, "💳 3. Payment QR Code", "Auto-Generated")
        val upiId = pendingSettings["upi_id"] ?: dashboardData?.settings?.get("upi_id") ?: "Not Set"
        val qrInfo = TextView(this).apply {
            text = "📲 UPI ID: $upiId\nWebsite checkout ke samay customer ko auto-generated dynamic UPI QR dikhaya jata hai jisse instant payment hoti hai."
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            background = UiKit.cardDrawable(Color.parseColor("#080D1A"), Color.parseColor("#1E293B"), 1, 8f, this@MainActivity)
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10))
        }
        qrBody.addView(qrInfo)
        form.addView(qrCard)

        // Save Button
        val saveBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "🚀 Save All Website Images Live",
            startColor = "#2563EB",
            endColor = "#00F0FF"
        ) {
            saveWebsiteSettings()
        }
        form.addView(saveBtn)

        scroll.addView(form)
        return scroll
    }

    private fun createColorChips(onColorSelected: (String, String) -> Unit): LinearLayout {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
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
            Pair("🍷 Temple Silk", "#140204"),
            Pair("🤍 Clean Slate", "#F8FAFC")
        )

        for (item in palette) {
            val btn = Button(this).apply {
                text = item.first
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#1E293B"),
                    strokeColor = Color.parseColor("#334155"),
                    strokeWidthDp = 1,
                    radiusDp = 10f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, UiKit.dp(this@MainActivity, 8), 0)
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
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
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
                setTextColor(Color.parseColor("#38BDF8"))
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#1E293B"),
                    strokeColor = Color.parseColor("#334155"),
                    strokeWidthDp = 1,
                    radiusDp = 10f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, UiKit.dp(this@MainActivity, 8), 0)
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

    private fun syncVisibleInputsToPending() {
        if (::brandNameInput.isInitialized && brandNameInput.isAttachedToWindow) {
            pendingSettings["brand_name"] = brandNameInput.text.toString().trim()
        }
        if (::taglineInput.isInitialized && taglineInput.isAttachedToWindow) {
            pendingSettings["brand_tagline"] = taglineInput.text.toString().trim()
        }
        if (::logoUrlInput.isInitialized && logoUrlInput.isAttachedToWindow) {
            pendingSettings["logo_url"] = logoUrlInput.text.toString().trim()
        }
        if (::bannerUrlInput.isInitialized && bannerUrlInput.isAttachedToWindow) {
            val bUrl = bannerUrlInput.text.toString().trim()
            pendingSettings["banner_url"] = bUrl
            pendingSettings["hero_banner_url"] = bUrl
        }
        if (::fontFamilyInput.isInitialized && fontFamilyInput.isAttachedToWindow) {
            pendingSettings["font_family"] = fontFamilyInput.text.toString().trim()
        }
        if (::headerColorInput.isInitialized && headerColorInput.isAttachedToWindow) {
            pendingSettings["header_color"] = headerColorInput.text.toString().trim()
        }
        if (::footerColorInput.isInitialized && footerColorInput.isAttachedToWindow) {
            pendingSettings["footer_color"] = footerColorInput.text.toString().trim()
        }
        if (::heroBadgeInput.isInitialized && heroBadgeInput.isAttachedToWindow) {
            pendingSettings["hero_badge"] = heroBadgeInput.text.toString().trim()
        }
        if (::heroTitleInput.isInitialized && heroTitleInput.isAttachedToWindow) {
            pendingSettings["hero_title"] = heroTitleInput.text.toString().trim()
        }
        if (::heroSubtitleInput.isInitialized && heroSubtitleInput.isAttachedToWindow) {
            pendingSettings["hero_subtitle"] = heroSubtitleInput.text.toString().trim()
        }
        if (::ctaTextInput.isInitialized && ctaTextInput.isAttachedToWindow) {
            pendingSettings["cta_text"] = ctaTextInput.text.toString().trim()
        }
        if (::announcementInput.isInitialized && announcementInput.isAttachedToWindow) {
            pendingSettings["announcement_text"] = announcementInput.text.toString().trim()
        }
        if (::announcementSwitch.isInitialized && announcementSwitch.isAttachedToWindow) {
            pendingSettings["show_announcement"] = if (announcementSwitch.isChecked) "true" else "false"
        }
        if (::waNumberInput.isInitialized && waNumberInput.isAttachedToWindow) {
            pendingSettings["whatsapp_number"] = waNumberInput.text.toString().trim()
        }
        if (::upiIdInput.isInitialized && upiIdInput.isAttachedToWindow) {
            pendingSettings["upi_id"] = upiIdInput.text.toString().trim()
        }
        if (::altPhoneInput.isInitialized && altPhoneInput.isAttachedToWindow) {
            pendingSettings["alt_phone"] = altPhoneInput.text.toString().trim()
        }
        if (::helplineInput.isInitialized && helplineInput.isAttachedToWindow) {
            pendingSettings["helpline_number"] = helplineInput.text.toString().trim()
        }
        if (::businessEmailInput.isInitialized && businessEmailInput.isAttachedToWindow) {
            pendingSettings["business_email"] = businessEmailInput.text.toString().trim()
        }
        if (::businessAddressInput.isInitialized && businessAddressInput.isAttachedToWindow) {
            pendingSettings["business_address"] = businessAddressInput.text.toString().trim()
        }
        if (::mapUrlInput.isInitialized && mapUrlInput.isAttachedToWindow) {
            pendingSettings["map_url"] = mapUrlInput.text.toString().trim()
        }
        if (::qrCodeUrlInput.isInitialized && qrCodeUrlInput.isAttachedToWindow) {
            pendingSettings["qr_code_url"] = qrCodeUrlInput.text.toString().trim()
        }
        if (::contactFormSwitch.isInitialized && contactFormSwitch.isAttachedToWindow) {
            pendingSettings["show_contact_form"] = if (contactFormSwitch.isChecked) "true" else "false"
        }
    }

    private fun populateSettingsFields() {
        val s = dashboardData?.settings ?: return
        for ((k, v) in s) {
            if (!pendingSettings.containsKey(k)) {
                pendingSettings[k] = v
            }
        }
        if (::brandNameInput.isInitialized && brandNameInput.isAttachedToWindow) {
            brandNameInput.setText(pendingSettings["brand_name"] ?: s["brand_name"] ?: "")
        }
        if (::taglineInput.isInitialized && taglineInput.isAttachedToWindow) {
            taglineInput.setText(pendingSettings["brand_tagline"] ?: s["brand_tagline"] ?: "")
        }
        if (::logoUrlInput.isInitialized && logoUrlInput.isAttachedToWindow) {
            logoUrlInput.setText(pendingSettings["logo_url"] ?: s["logo_url"] ?: "")
        }
        if (::bannerUrlInput.isInitialized && bannerUrlInput.isAttachedToWindow) {
            bannerUrlInput.setText(pendingSettings["banner_url"] ?: s["banner_url"] ?: "")
        }
        if (::fontFamilyInput.isInitialized && fontFamilyInput.isAttachedToWindow) {
            fontFamilyInput.setText(pendingSettings["font_family"] ?: s["font_family"] ?: "")
        }
        if (::headerColorInput.isInitialized && headerColorInput.isAttachedToWindow) {
            headerColorInput.setText(pendingSettings["header_color"] ?: s["header_color"] ?: "")
        }
        if (::footerColorInput.isInitialized && footerColorInput.isAttachedToWindow) {
            footerColorInput.setText(pendingSettings["footer_color"] ?: s["footer_color"] ?: "")
        }
        if (::heroBadgeInput.isInitialized && heroBadgeInput.isAttachedToWindow) {
            heroBadgeInput.setText(pendingSettings["hero_badge"] ?: s["hero_badge"] ?: "")
        }
        if (::heroTitleInput.isInitialized && heroTitleInput.isAttachedToWindow) {
            heroTitleInput.setText(pendingSettings["hero_title"] ?: s["hero_title"] ?: "")
        }
        if (::heroSubtitleInput.isInitialized && heroSubtitleInput.isAttachedToWindow) {
            heroSubtitleInput.setText(pendingSettings["hero_subtitle"] ?: s["hero_subtitle"] ?: "")
        }
        if (::ctaTextInput.isInitialized && ctaTextInput.isAttachedToWindow) {
            ctaTextInput.setText(pendingSettings["cta_text"] ?: s["cta_text"] ?: "")
        }
        if (::announcementInput.isInitialized && announcementInput.isAttachedToWindow) {
            announcementInput.setText(pendingSettings["announcement_text"] ?: s["announcement_text"] ?: "")
        }
        if (::announcementSwitch.isInitialized && announcementSwitch.isAttachedToWindow) {
            val show = pendingSettings["show_announcement"] ?: s["show_announcement"] ?: "true"
            announcementSwitch.isChecked = show.equals("true", ignoreCase = true)
        }
        if (::waNumberInput.isInitialized && waNumberInput.isAttachedToWindow) {
            waNumberInput.setText(pendingSettings["whatsapp_number"] ?: s["whatsapp_number"] ?: "")
        }
        if (::upiIdInput.isInitialized && upiIdInput.isAttachedToWindow) {
            upiIdInput.setText(pendingSettings["upi_id"] ?: s["upi_id"] ?: "")
        }
        if (::altPhoneInput.isInitialized && altPhoneInput.isAttachedToWindow) {
            altPhoneInput.setText(pendingSettings["alt_phone"] ?: s["alt_phone"] ?: "")
        }
        if (::helplineInput.isInitialized && helplineInput.isAttachedToWindow) {
            helplineInput.setText(pendingSettings["helpline_number"] ?: s["helpline_number"] ?: "")
        }
        if (::businessEmailInput.isInitialized && businessEmailInput.isAttachedToWindow) {
            businessEmailInput.setText(pendingSettings["business_email"] ?: s["business_email"] ?: "")
        }
        if (::businessAddressInput.isInitialized && businessAddressInput.isAttachedToWindow) {
            businessAddressInput.setText(pendingSettings["business_address"] ?: s["business_address"] ?: "")
        }
        if (::mapUrlInput.isInitialized && mapUrlInput.isAttachedToWindow) {
            mapUrlInput.setText(pendingSettings["map_url"] ?: s["map_url"] ?: "")
        }
        if (::qrCodeUrlInput.isInitialized && qrCodeUrlInput.isAttachedToWindow) {
            qrCodeUrlInput.setText(pendingSettings["qr_code_url"] ?: s["qr_code_url"] ?: "")
        }
        if (::contactFormSwitch.isInitialized && contactFormSwitch.isAttachedToWindow) {
            val show = pendingSettings["show_contact_form"] ?: s["show_contact_form"] ?: "true"
            contactFormSwitch.isChecked = show.equals("true", ignoreCase = true)
        }
    }

    private fun saveWebsiteSettings() {
        val url = getSheetUrl()
        if (url.isEmpty()) {
            Toast.makeText(this, "Please set Sheet URL first", Toast.LENGTH_SHORT).show()
            return
        }

        syncVisibleInputsToPending()

        val s = dashboardData?.settings ?: emptyMap()
        val body = mutableMapOf<String, String>()
        body["action"] = "updateSettings"
        body.putAll(s)
        body.putAll(pendingSettings)

        // Default fallbacks for safety
        if (!body.containsKey("brand_name") || body["brand_name"].isNullOrEmpty()) {
            body["brand_name"] = "SheetStore Pro"
        }

        Toast.makeText(this, "Updating website...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Website updated live successfully! 🎉", Toast.LENGTH_LONG).show()
                        fetchDashboard(silent = true)
                    } else {
                        Toast.makeText(this@MainActivity, "Update failed: HTTP ${response.code()}", Toast.LENGTH_SHORT).show()
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
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 32))
        }

        // Header Row: Title + Add Product Button
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
        }

        val heading = TextView(this).apply {
            text = "📦 Products & Catalog"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(heading)

        val addBtn = Button(this).apply {
            text = "+ Add Product"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#059669"),
                endColor = Color.parseColor("#047857"),
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 6))
            setOnClickListener { showAddProductDialog() }
        }
        topRow.addView(addBtn)
        scrollContent.addView(topRow)

        val subtext = TextView(this).apply {
            text = "Selling price badlein ya product ko live on/off karein. Sab changes instant website par reflect hote hain:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16))
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

    private fun showAddProductDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1F2D48"),
                strokeWidthDp = 1,
                radiusDp = 18f,
                context = this@MainActivity
            )
        }

        val titleInput = UiKit.createStyledInput(this, "Product Title (e.g. Silk Saree / E-book)")
        val catInput = UiKit.createStyledInput(this, "Category (e.g. Saree, Ebook, Vastu)")
        val sellingInput = UiKit.createStyledInput(this, "Selling Price in ₹ (e.g. 499)")
        val originalInput = UiKit.createStyledInput(this, "Original MRP in ₹ (e.g. 999)")
        val imageInput = UiKit.createStyledInput(this, "Image URL (https://...)")
        val descInput = UiKit.createStyledInput(this, "Short Description", singleLine = false)

        dialogLayout.addView(UiKit.createFieldLabel(this, "Product Title:"))
        dialogLayout.addView(titleInput)
        dialogLayout.addView(UiKit.createFieldLabel(this, "Category:"))
        dialogLayout.addView(catInput)
        dialogLayout.addView(UiKit.createFieldLabel(this, "Offer Price (₹):"))
        dialogLayout.addView(sellingInput)
        dialogLayout.addView(UiKit.createFieldLabel(this, "Original MRP (₹):"))
        dialogLayout.addView(originalInput)
        dialogLayout.addView(UiKit.createFieldLabel(this, "Product Image URL:"))
        dialogLayout.addView(imageInput)
        dialogLayout.addView(UiKit.createFieldLabel(this, "Description:"))
        dialogLayout.addView(descInput)

        AlertDialog.Builder(this)
            .setTitle("✨ Naya Product Add Karein")
            .setView(dialogLayout)
            .setPositiveButton("Add Product 🚀") { _, _ ->
                val title = titleInput.text.toString().trim()
                val cat = catInput.text.toString().trim().ifEmpty { "General" }
                val selling = sellingInput.text.toString().trim()
                val original = originalInput.text.toString().trim()
                val img = imageInput.text.toString().trim()
                val desc = descInput.text.toString().trim()

                if (title.isEmpty() || selling.isEmpty()) {
                    Toast.makeText(this, "Title aur Price zaroor bharein!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                addNewProductToSheet(title, cat, selling, original, img, desc)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addNewProductToSheet(title: String, cat: String, selling: String, original: String, img: String, desc: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        val newId = "prod-" + System.currentTimeMillis()
        val body = mapOf(
            "action" to "addProduct",
            "id" to newId,
            "title" to title,
            "category" to cat,
            "price_selling" to selling,
            "price_original" to original,
            "thumbnail" to img,
            "description" to desc,
            "is_active" to "TRUE"
        )

        Toast.makeText(this, "Adding $title to Google Sheet...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.addProduct(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MainActivity, "Product added successfully! 🎉", Toast.LENGTH_LONG).show()
                        fetchDashboard()
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to add product", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
    // WEBSITE CMS & STUDIO (4 DEDICATED RECTANGULAR MENU BLOCKS)
    // ==========================================

    private data class StudioMenuItem(
        val index: Int,
        val icon: String,
        val title: String,
        val subtitle: String,
        val badge: String,
        val accentColor: String
    )

    private fun createStudioMenuCard(item: StudioMenuItem): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 6f, // RECTANGULAR BOX
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 12)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                designSubTabIndex = item.index
                refreshCurrentTab()
            }
        }

        // Left Icon Box: Rectangular Box
        val iconBox = TextView(this).apply {
            text = item.icon
            textSize = 24f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor(item.accentColor),
                strokeWidthDp = 1,
                radiusDp = 6f, // RECTANGULAR BOX
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 48), UiKit.dp(this@MainActivity, 48)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 14)
            }
        }
        card.addView(iconBox)

        // Middle Content (Title + Badge + Subtitle)
        val textContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = item.title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(titleView)

        val badgePill = TextView(this).apply {
            text = item.badge
            textSize = 9.5f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.parseColor(item.accentColor))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0C2038"),
                strokeColor = Color.parseColor(item.accentColor),
                strokeWidthDp = 1,
                radiusDp = 4f, // RECTANGULAR BOX
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 3), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 3))
        }
        topRow.addView(badgePill)
        textContainer.addView(topRow)

        val subView = TextView(this).apply {
            text = item.subtitle
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setLineSpacing(2f, 1.15f)
            setPadding(0, UiKit.dp(this@MainActivity, 3), UiKit.dp(this@MainActivity, 4), 0)
        }
        textContainer.addView(subView)
        card.addView(textContainer)

        // Right Arrow Indicator
        val arrow = TextView(this).apply {
            text = "›"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#475569"))
            setPadding(UiKit.dp(this@MainActivity, 6), 0, 0, 0)
        }
        card.addView(arrow)

        return card
    }

    private fun createSiteStudioMenuHub(): View {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 32))
        }

        // Header Title Box (Rectangular Box)
        val headerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F172A"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 6f, // RECTANGULAR BOX
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 14)
            }
        }
        val heading = TextView(this).apply {
            text = "🛠️ Website Site Studio"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val subHeading = TextView(this).apply {
            text = "Website ke kisi bhi section ko customize karne ke liye niche diye gaye 4 rectangular buttons me se kisi par tap karein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, 0)
        }
        headerCard.addView(heading)
        headerCard.addView(subHeading)
        container.addView(headerCard)

        // The 4 Rectangular Menu Blocks
        val menuItems = listOf(
            StudioMenuItem(
                index = 0,
                icon = "🏢",
                title = "1st: Site Main Details & Contact Directory",
                subtitle = "Brand Name, WhatsApp, Phone, Address, Map, UPI, QR & Contact Form (Individual Save)",
                badge = "Excel Grid • 14 Fields",
                accentColor = "#00F0FF"
            ),
            StudioMenuItem(
                index = 1,
                icon = "🎨",
                title = "2nd: Website Category & Themes",
                subtitle = "E-Commerce Main Category (15 Industry Themes: Electronics, Fashion, Health, Grocery, etc.)",
                badge = "15 Themes",
                accentColor = "#A78BFA"
            ),
            StudioMenuItem(
                index = 2,
                icon = "🔤",
                title = "3rd: Fonts & Styling Information",
                subtitle = "Heading Font Family, Header/Footer Colors & Hero Headlines",
                badge = "Design & Fonts",
                accentColor = "#F59E0B"
            ),
            StudioMenuItem(
                index = 3,
                icon = "🖼️",
                title = "4th: Whole Website Image Updater",
                subtitle = "Upload Brand Logo (300x300 px), Hero Banner (1200x500 px) & UPI QR",
                badge = "Photos & Media",
                accentColor = "#38BDF8"
            )
        )

        for (item in menuItems) {
            container.addView(createStudioMenuCard(item))
        }

        scroll.addView(container)
        return scroll
    }

    private fun createWebsiteDesignAndCustomTab(): View {
        return when (designSubTabIndex) {
            0 -> createSiteMainDetailsSubTab()
            1 -> createThemeSelectionSubTab()
            2 -> createFontsAndInfoSubTab()
            3 -> createImageUpdaterSubTab()
            else -> createSiteStudioMenuHub()
        }
    }

    // ==========================================
    // MODERN BOTTOM NAVIGATION DOCK (3 TABS)
    // ==========================================
    private fun createBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10))
            elevation = UiKit.dp(this@MainActivity, 8).toFloat()

            addView(createNavItem("🛒 Orders", 0))
            addView(createNavItem("🛠️ Site Studio", 1))
            addView(createNavItem("📦 Products", 2))
        }
    }

    private fun createNavItem(label: String, index: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(UiKit.dp(this@MainActivity, 2), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 2), UiKit.dp(this@MainActivity, 6))
            setTextColor(if (currentTab == index) Color.parseColor("#00F0FF") else Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                if (!isSheetVerified()) {
                    Toast.makeText(this@MainActivity, "Pehle Google Sheet verify karein! 🔒", Toast.LENGTH_SHORT).show()
                }
                switchTab(index)
            }
        }
    }

    private fun switchTab(tabIndex: Int) {
        if (tabIndex == 1 && currentTab != 1) {
            designSubTabIndex = -1
        }
        currentTab = tabIndex
        contentContainer.removeAllViews()

        val tabView = when (tabIndex) {
            0 -> createOrdersTab()
            1 -> createWebsiteDesignAndCustomTab()
            2 -> createProductsTab()
            4 -> createSettingsAndHubTab()
            else -> createOrdersTab()
        }
        contentContainer.addView(tabView)
        updateNavSelection()
    }

    private fun refreshCurrentTab() {
        switchTab(currentTab)
    }

    private fun updateNavSelection() {
        for (i in 0 until navBar.childCount) {
            val btn = navBar.getChildAt(i) as? Button ?: continue
            val isSel = (currentTab == i)
            btn.setTextColor(if (isSel) Color.parseColor("#00F0FF") else Color.parseColor("#94A3B8"))
            btn.background = if (isSel) {
                UiKit.cardDrawable(
                    bgColor = Color.parseColor("#152238"),
                    strokeColor = Color.parseColor("#00F0FF"),
                    strokeWidthDp = 1,
                    radiusDp = 12f,
                    context = this
                )
            } else {
                null
            }
        }
    }

    override fun onBackPressed() {
        if (currentTab == 1 && designSubTabIndex == 1 && selectedMainCategoryForSubTab != null) {
            selectedMainCategoryForSubTab = null
            refreshCurrentTab()
            return
        }
        if (currentTab == 1 && designSubTabIndex != -1) {
            syncVisibleInputsToPending()
            designSubTabIndex = -1
            refreshCurrentTab()
            return
        }
        super.onBackPressed()
    }

    // ==========================================
    // DATA FETCHING & SESSION HELPERS
    // ==========================================
    private fun fetchDashboard(silent: Boolean = false) {
        val rawUrl = getSheetUrl().trim()
        if (rawUrl.isEmpty()) return

        val url = if (rawUrl.contains("action=")) {
            rawUrl
        } else if (rawUrl.contains("?")) {
            "$rawUrl&action=getAdminDashboard"
        } else {
            "$rawUrl?action=getAdminDashboard"
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.getAdminDashboard(fullUrl = url)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        dashboardData = response.body()
                        updateStatsCard()

                        val orders = dashboardData?.orders ?: emptyList()
                        if (::ordersAdapter.isInitialized) {
                            ordersAdapter.updateData(orders)
                        }
                        processNewOrdersForNotification(orders)

                        dashboardData?.products?.let {
                            if (::productsAdapter.isInitialized) {
                                productsAdapter.updateData(it)
                            }
                        }

                        populateSettingsFields()
                        val brand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
                        if (::topBrandNameView.isInitialized) topBrandNameView.text = brand
                        if (::topAvatarView.isInitialized) topAvatarView.text = if (brand.isNotEmpty()) brand.take(2).uppercase() else "SS"

                        if (!silent) {
                            Toast.makeText(this@MainActivity, "Data synced! (${orders.size} orders loaded)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                if (!silent) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // ==========================================
    // LOCAL PUSH NOTIFICATIONS & AUTO-POLLING
    // ==========================================
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ORDER_CHANNEL_ID,
                "SheetStore Order Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Naye customer orders aane par turant alert notification"
                enableLights(true)
                lightColor = Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
            }
        }
    }

    private fun loadKnownOrderIds() {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("known_order_ids", null)
        if (saved != null) {
            knownOrderIds.addAll(saved)
        }
    }

    private fun saveKnownOrderIds() {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("known_order_ids", HashSet(knownOrderIds)).apply()
    }

    private fun processNewOrdersForNotification(orders: List<Order>) {
        if (orders.isEmpty()) return

        if (knownOrderIds.isEmpty()) {
            // First launch: cache existing orders so user is not spammed with historical orders
            orders.forEach { knownOrderIds.add(it.orderId) }
            saveKnownOrderIds()
            return
        }

        var newOrderFound = false
        for (order in orders) {
            if (!knownOrderIds.contains(order.orderId)) {
                knownOrderIds.add(order.orderId)
                newOrderFound = true
                triggerLocalOrderNotification(order)
            }
        }
        if (newOrderFound) {
            saveKnownOrderIds()
        }
    }

    private fun triggerLocalOrderNotification(order: Order) {
        val cleanId = order.orderId.replace("#", "").trim()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = "com.sheetstore.admin.ACTION_OPEN_ORDERS"
            data = Uri.parse("sheetstore://orders/$cleanId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", 0)
            putExtra("order_id", cleanId)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            cleanId.hashCode(),
            intent,
            flags
        )

        val notifText = "👤 ${order.customerName} (${if (order.customerPhone.isNotEmpty()) order.customerPhone else "Customer"})\n🛒 ${order.product} — ₹${order.amount}\n💳 UTR: ${if (order.utr.isNotEmpty()) order.utr else "Pending"}\n⚡ Tap karein order approve karne ke liye!"

        val notification = NotificationCompat.Builder(this, ORDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("🎉 Naya Order Aaya! (${order.orderId})")
            .setContentText("${order.customerName} • ₹${order.amount} • ${order.product}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.parseColor("#00F0FF"))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(cleanId.hashCode(), notification)
    }

    private fun startAutoPolling() {
        autoPollJob?.cancel()
        autoPollJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(20_000) // Poll Google Sheet every 20 seconds
                if (isUserLoggedIn() && isSheetVerified()) {
                    fetchDashboard(silent = true)
                }
            }
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
            setTextColor(Color.WHITE)
            background = UiKit.inputDrawable(this@MainActivity, false)
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 12))
        }

        AlertDialog.Builder(this)
            .setTitle("Google Sheet Web App URL")
            .setMessage("Google Sheet me Code.gs deploy karke mila hua Web App URL yahan paste karein:")
            .setView(input)
            .setPositiveButton("Save & Connect") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    saveSheetUrl(url)
                    getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE).edit()
                        .putBoolean("is_sheet_verified", true).apply()
                    Toast.makeText(this, "URL Saved & Verified! Fetching data...", Toast.LENGTH_SHORT).show()
                    showDashboardHome()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getUpiId(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val upi = prefs.getString("store_upi_id", "") ?: ""
        if (upi.isNotEmpty()) return upi
        return dashboardData?.settings?.get("upi_id") ?: "9907522413@ybl"
    }

    private fun showStoreQuickHubAndSettingsDialog() {
        switchTab(4)
    }

    private fun openAccountFullScreenView() {
        showProfileSubscriptionDialog()
    }

    private fun createSettingsAndHubTab(): View {
        val dialogScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 40))
        }

        // Top Navigation Action Bar inside settings view
        val navHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16))
        }

        val backBtn = Button(this).apply {
            text = "← Orders"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#1E293B"),
                strokeColor = Color.parseColor("#334155"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
            setOnClickListener {
                switchTab(0)
            }
        }
        navHeader.addView(backBtn)

        val titleText = TextView(this).apply {
            text = "⚙️ Store Settings & Hub"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        navHeader.addView(titleText)

        val logoutBtn = Button(this).apply {
            text = "🚪 Log Out"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#EF4444"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#271216"),
                strokeColor = Color.parseColor("#7F1D1D"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 6))
            setOnClickListener {
                performLogout()
            }
        }
        navHeader.addView(logoutBtn)
        layout.addView(navHeader)

        // 0. STORE OWNER LICENSE CARD
        val ownerCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1F2D48"),
                strokeWidthDp = 1,
                radiusDp = 16f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }
        val ownerName = getLoggedInName().ifEmpty { "Store Owner" }
        val ownerPhone = getLoggedInPhone().ifEmpty { "9907522413" }
        val ownerEmail = getLoggedInEmail().ifEmpty { "Registered Merchant" }
        val ownerId = getUserId().ifEmpty { "PRO-MERCHANT" }

        val ownerHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        val ownerAvatar = TextView(this).apply {
            text = ownerName.take(2).uppercase()
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#00F0FF"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 40), UiKit.dp(this@MainActivity, 40)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 10)
            }
        }
        ownerHeaderRow.addView(ownerAvatar)

        val ownerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val ownerTitle = TextView(this).apply {
            text = ownerName
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        ownerCol.addView(ownerTitle)

        val ownerSub = TextView(this).apply {
            text = "📞 $ownerPhone • ✉️ $ownerEmail"
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
        }
        ownerCol.addView(ownerSub)
        ownerHeaderRow.addView(ownerCol)

        val verifiedPill = UiKit.createPillBadge(this, "● Verified Merchant", Color.parseColor("#10B981"), Color.parseColor("#064E3B"), Color.parseColor("#10B981"))
        ownerHeaderRow.addView(verifiedPill)
        ownerCard.addView(ownerHeaderRow)

        val idNote = TextView(this).apply {
            text = "🆔 User ID: $ownerId • License: Lifetime Active"
            textSize = 10.5f
            setTextColor(Color.parseColor("#64748B"))
        }
        ownerCard.addView(idNote)
        layout.addView(ownerCard)

        // 1. BRAND NAME
        val brandLabel = TextView(this).apply {
            text = "🏷️ STORE / BRAND NAME"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(brandLabel)
        val currentBrand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
        val brandInput = UiKit.createStyledInput(this, "Store ka naam daalein", currentBrand)
        layout.addView(brandInput)

        // 2. STORE LOGO & APP ICON (WITH RECOMMENDED SIZE & WEBSITE HEADER PREVIEW)
        val logoLabel = TextView(this).apply {
            text = "🖼️ STORE LOGO & APP ICON"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(logoLabel)

        val currentLogo = dashboardData?.settings?.get("logo_url") ?: ""

        val logoRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }

        val logoPreviewImg = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 50), UiKit.dp(this@MainActivity, 50)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 10)
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#1E293B"),
                strokeColor = Color.parseColor("#38BDF8"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 4))
        }
        activeHubLogoPreview = logoPreviewImg

        val uploadLogoBtn = UiKit.createSecondaryButton(this, "📁 Upload from Gallery", "#38BDF8") {
            openImagePicker(REQUEST_PICK_LOGO_HUB)
        }
        uploadLogoBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 44), 1f)

        logoRow.addView(logoPreviewImg)
        logoRow.addView(uploadLogoBtn)
        layout.addView(logoRow)

        val logoInput = UiKit.createStyledInput(this, "https://... logo link ya Gallery se upload karein", currentLogo)
        activeHubLogoInput = logoInput
        layout.addView(logoInput)

        // --- RECOMMENDED SIZE INFO BOX (As requested by user!) ---
        val sizeInfoCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#16233B"),
                strokeColor = Color.parseColor("#2563EB"),
                strokeWidthDp = 1,
                radiusDp = 12f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = UiKit.dp(this@MainActivity, 8)
                bottomMargin = UiKit.dp(this@MainActivity, 10)
            }
        }
        val sizeTitle = TextView(this).apply {
            text = "📐 Recommended Logo Dimensions & Specs:"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F5DE7A"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        sizeInfoCard.addView(sizeTitle)

        val sizeSpecs = TextView(this).apply {
            text = "• Square Logo / App Icon: 512 x 512 px (1:1 Ratio)\n• Header Bar Logo: 240 x 70 px (Horizontal Banner)\n• Best Format: PNG (Transparent Background) ya High-Res JPG\n• File Size: 100 KB – 2 MB ke beech best rehta hai."
            textSize = 11f
            setTextColor(Color.parseColor("#CBD5E1"))
            setLineSpacing(5f, 1.15f)
        }
        sizeInfoCard.addView(sizeSpecs)
        layout.addView(sizeInfoCard)

        // --- LIVE WEBSITE HEADER MOCKUP PREVIEW BOX (As requested by user!) ---
        val mockupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#080D1A"),
                strokeColor = Color.parseColor("#1E2D48"),
                strokeWidthDp = 1,
                radiusDp = 14f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }

        val mockupTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        val mockupTitle = TextView(this).apply {
            text = "👁️ Live Website Header Preview"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val mockupLivePill = UiKit.createPillBadge(this, "Simulated Bar", Color.parseColor("#38BDF8"), Color.parseColor("#082F49"), Color.parseColor("#0284C7"))
        mockupTitleRow.addView(mockupTitle)
        mockupTitleRow.addView(mockupLivePill)
        mockupCard.addView(mockupTitleRow)

        val navSimBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0B1120"),
                strokeColor = Color.parseColor("#334155"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 8))
        }

        val mockMenuIcon = TextView(this).apply {
            text = "☰"
            textSize = 16f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, UiKit.dp(this@MainActivity, 12), 0)
        }
        navSimBar.addView(mockMenuIcon)

        val mockHeaderLogoImg = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 110), UiKit.dp(this@MainActivity, 32)).apply {
                weight = 1f
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        activeHubMockupLogo = mockHeaderLogoImg
        navSimBar.addView(mockHeaderLogoImg)

        val mockActions = TextView(this).apply {
            text = "🛒   👤"
            textSize = 14f
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(UiKit.dp(this@MainActivity, 10), 0, 0, 0)
        }
        navSimBar.addView(mockActions)
        mockupCard.addView(navSimBar)

        val mockupCaption = TextView(this).apply {
            text = "✨ Website par customer ke mobile par top header aise render hoga."
            textSize = 10.5f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, UiKit.dp(this@MainActivity, 6), 0, 0)
        }
        mockupCard.addView(mockupCaption)
        layout.addView(mockupCard)

        // Load existing logo into previews
        if (currentLogo.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stream = java.net.URL(currentLogo).openStream()
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream.close()
                    withContext(Dispatchers.Main) {
                        if (bmp != null) {
                            logoPreviewImg.setImageBitmap(bmp)
                            mockHeaderLogoImg.setImageBitmap(bmp)
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        // 3. WHATSAPP BUSINESS SUPPORT
        val waLabel = TextView(this).apply {
            text = "💬 WHATSAPP BUSINESS NUMBER"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(waLabel)
        val currentWa = dashboardData?.settings?.get("whatsapp_number") ?: getLoggedInPhone().ifEmpty { "9907522413" }
        val waInput = UiKit.createStyledInput(this, "jaise: 9907522413", currentWa)
        layout.addView(waInput)

        val testWaBtn = UiKit.createSecondaryButton(this, "📲 Test WhatsApp Chat", "#10B981") {
            val wa = waInput.text.toString().trim()
            if (wa.isNotEmpty()) {
                try {
                    val uri = Uri.parse("https://wa.me/$wa?text=${Uri.encode("Namaste! Yeh WhatsApp test message hai.")}")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                }
            }
        }
        testWaBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this@MainActivity, 38)
        ).apply {
            bottomMargin = UiKit.dp(this@MainActivity, 16)
        }
        layout.addView(testWaBtn)

        // 4. LIVE WEBSITE URL & LIVE READY-MADE DEMOS
        val webCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#131B2E"),
                strokeColor = Color.parseColor("#1E2D4A"),
                strokeWidthDp = 1,
                radiusDp = 16f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }

        val webLabel = TextView(this).apply {
            text = "🌐 LIVE WEBSITE URL"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        webCard.addView(webLabel)
        val currentWebUrl = getWebsiteLiveUrl()
        val webInput = UiKit.createStyledInput(this, "https://...", currentWebUrl)
        webCard.addView(webInput)

        // Action Buttons for Website
        val webBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        val openWebBtn = Button(this).apply {
            text = "🌐 Open Website ↗"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#1D4ED8"),
                radiusDp = 10f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 38), 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 6)
            }
            setOnClickListener {
                val url = webInput.text.toString().trim().ifEmpty { getWebsiteLiveUrl() }
                if (url.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Could not open browser", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val copyWebBtn = UiKit.createSecondaryButton(this, "📋 Copy Link", "#38BDF8") {
            val url = webInput.text.toString().trim().ifEmpty { getWebsiteLiveUrl() }
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Website URL", url)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this@MainActivity, "Website link copied! ✓", Toast.LENGTH_SHORT).show()
        }
        copyWebBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f)
        webBtnRow.addView(openWebBtn)
        webBtnRow.addView(copyWebBtn)
        webCard.addView(webBtnRow)

        // Ready-made Live Demos Grid
        val demosLabel = TextView(this).apply {
            text = "🎯 READY-MADE LIVE STORE DEMOS (1-CLICK VIEW):"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#F5DE7A"))
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 8))
        }
        webCard.addView(demosLabel)

        val demoButtonsCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Demo 1: Digital Products Store
        val digitalDemoBtn = UiKit.createSecondaryButton(this, "💻 Digital Products Store Demo ↗", "#38BDF8") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yuviteltechnologies-art.github.io/freewebsite/demo.html")))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Could not open demo", Toast.LENGTH_SHORT).show()
            }
        }
        digitalDemoBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this@MainActivity, 38)
        ).apply {
            bottomMargin = UiKit.dp(this@MainActivity, 6)
        }
        demoButtonsCol.addView(digitalDemoBtn)

        // Demo 2: NOOLAM Saree Studio
        val sareeDemoBtn = UiKit.createSecondaryButton(this, "🥻 NOOLAM Saree Studio Demo ↗", "#D4AF37") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html")))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Could not open demo", Toast.LENGTH_SHORT).show()
            }
        }
        sareeDemoBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this@MainActivity, 38)
        ).apply {
            bottomMargin = UiKit.dp(this@MainActivity, 6)
        }
        demoButtonsCol.addView(sareeDemoBtn)

        // Demo 3: All Demos Showcase
        val allDemosBtn = UiKit.createSecondaryButton(this, "🚀 All Demos Selector & Themes ↗", "#10B981") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yuviteltechnologies-art.github.io/freewebsite/demos.html")))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Could not open demo", Toast.LENGTH_SHORT).show()
            }
        }
        allDemosBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this@MainActivity, 38)
        )
        demoButtonsCol.addView(allDemosBtn)

        webCard.addView(demoButtonsCol)
        layout.addView(webCard)

        // 5. UPI QR CODE & UPI ID (REAL TIME PREVIEW)
        val upiCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#131B2E"),
                strokeColor = Color.parseColor("#1E2D4A"),
                strokeWidthDp = 1,
                radiusDp = 16f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }
        val upiHeader = TextView(this).apply {
            text = "📲 STORE UPI ID & LIVE QR CODE"
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        upiCard.addView(upiHeader)

        val currentUpi = dashboardData?.settings?.get("upi_id") ?: getUpiId()
        val upiInput = UiKit.createStyledInput(this, "jaise: 9907522413@ybl", currentUpi)
        upiCard.addView(upiInput)

        // Real-time QR preview box
        val qrBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.WHITE,
                strokeColor = Color.parseColor("#E2E8F0"),
                strokeWidthDp = 1,
                radiusDp = 12f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 160), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        val qrImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 136), UiKit.dp(this@MainActivity, 136))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val qrSubText = TextView(this).apply {
            text = "Live UPI QR Preview"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#334155"))
            gravity = Gravity.CENTER
        }
        qrBox.addView(qrImageView)
        qrBox.addView(qrSubText)
        upiCard.addView(qrBox)

        fun refreshQrImage(upi: String) {
            val cleanUpi = upi.trim()
            if (cleanUpi.isEmpty()) return
            val qrData = "upi://pay?pa=$cleanUpi&pn=SheetStore&am=499&cu=INR&tn=SheetStoreOrder"
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=" + Uri.encode(qrData)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val stream = java.net.URL(qrUrl).openStream()
                    val bmp = BitmapFactory.decodeStream(stream)
                    stream.close()
                    withContext(Dispatchers.Main) {
                        if (bmp != null) qrImageView.setImageBitmap(bmp)
                    }
                } catch (e: Exception) {}
            }
        }
        refreshQrImage(currentUpi)

        upiInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                refreshQrImage(s?.toString() ?: "")
            }
        })

        val upiNote = TextView(this).apply {
            text = "💡 Yehi QR code aur UPI ID aapki website par checkout ke waqt customer ko show hoga."
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
        }
        upiCard.addView(upiNote)
        layout.addView(upiCard)

        // 6. GOOGLE SHEET BACKEND SETTINGS
        val sheetLabel = TextView(this).apply {
            text = "📊 GOOGLE SHEET WEB APP URL"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(sheetLabel)
        val sheetInput = UiKit.createStyledInput(this, "https://script.google.com/macros/s/.../exec", getSheetUrl())
        layout.addView(sheetInput)

        val sheetBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 20))
        }
        val openSheetBtn = UiKit.createSecondaryButton(this, "📊 Open Sheet ↗", "#38BDF8") {
            val sUrl = getGoogleSpreadsheetUrl()
            if (sUrl.isNotEmpty()) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Could not open sheet", Toast.LENGTH_SHORT).show()
                }
            }
        }
        openSheetBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f).apply {
            rightMargin = UiKit.dp(this@MainActivity, 6)
        }
        val testSyncBtn = UiKit.createSecondaryButton(this, "🔄 Test Sync", "#10B981") {
            fetchDashboard()
            Toast.makeText(this@MainActivity, "Syncing data from Google Sheet...", Toast.LENGTH_SHORT).show()
        }
        testSyncBtn.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this, 38), 1f)
        sheetBtnRow.addView(openSheetBtn)
        sheetBtnRow.addView(testSyncBtn)
        layout.addView(sheetBtnRow)

        // 7. SAVE BUTTON
        var dialogRef: AlertDialog? = null
        val saveBtn = Button(this).apply {
            text = "💾 Save & Update Everything Live"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#059669"),
                endColor = Color.parseColor("#047857"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 46)
            )
            setOnClickListener {
                val newBrand = brandInput.text.toString().trim()
                val newLogo = logoInput.text.toString().trim()
                val newWeb = webInput.text.toString().trim()
                val newUpi = upiInput.text.toString().trim()
                val newWa = waInput.text.toString().trim()
                val newSheet = sheetInput.text.toString().trim()

                if (newSheet.isNotEmpty()) {
                    saveSheetUrl(newSheet)
                }
                val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    if (newUpi.isNotEmpty()) putString("store_upi_id", newUpi)
                    if (newWeb.isNotEmpty()) putString("website_live_url", newWeb)
                    if (newWa.isNotEmpty()) putString("logged_in_phone", newWa)
                    if (newBrand.isNotEmpty()) putString("logged_in_name", newBrand)
                    if (newLogo.isNotEmpty()) putString("store_logo_url", newLogo)
                    apply()
                }

                // Update top app bar views immediately
                if (newBrand.isNotEmpty()) {
                    topBrandNameView.text = newBrand
                    topAvatarView.text = newBrand.take(2).uppercase()
                }

                // Sync to Google Sheet Backend Live
                val url = getSheetUrl()
                val body = mapOf(
                    "action" to "updateSettings",
                    "brand_name" to newBrand,
                    "logo_url" to newLogo,
                    "website_live_url" to newWeb,
                    "upi_id" to newUpi,
                    "whatsapp_number" to newWa
                )
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val resp = ApiClient.service.updateSettings(fullUrl = url, body = body).execute()
                        withContext(Dispatchers.Main) {
                            if (resp.isSuccessful) {
                                val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                                s["brand_name"] = newBrand
                                s["logo_url"] = newLogo
                                s["website_live_url"] = newWeb
                                s["upi_id"] = newUpi
                                s["whatsapp_number"] = newWa
                                dashboardData = dashboardData?.copy(settings = s)
                                Toast.makeText(this@MainActivity, "🎉 Store details & settings live update ho gaye!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Saved locally! (Sheet network: ${e.message})", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Toast.makeText(this@MainActivity, "Updating live settings...", Toast.LENGTH_SHORT).show()
                switchTab(0)
            }
        }
        layout.addView(saveBtn)

        dialogScroll.addView(layout)
        return dialogScroll
    }

    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("🚪 Log Out")
            .setMessage("Kya aap sach me Admin portal se log out karna chahte hain?")
            .setPositiveButton("Haan, Log Out") { _, _ ->
                clearAuthSession()
                val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("is_sheet_verified", false).putBoolean("is_theme_selected", false).apply()
                Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                showFullScreenLoginScreen()
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

    private fun getWebsiteLiveUrl(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val url = prefs.getString("website_live_url", "") ?: ""
        if (url.isNotEmpty()) return url
        return dashboardData?.settings?.get("website_live_url") ?: "https://yuviteltechnologies-art.github.io/freewebsite/"
    }

    private fun getGoogleSpreadsheetUrl(): String {
        val direct = dashboardData?.spreadsheetUrl ?: dashboardData?.settings?.get("spreadsheet_url")
        if (!direct.isNullOrEmpty()) return direct
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("google_spreadsheet_url", "") ?: getSheetUrl()
    }

    private fun getLoggedInPhone(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("logged_in_phone", "") ?: ""
    }

    private fun getUserId(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("user_id", "") ?: ""
    }

    private fun getGithubRepoUrl(): String {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        return prefs.getString("github_repo_url", "") ?: ""
    }

    private fun clearAuthSession() {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("logged_in_name")
            .remove("logged_in_email")
            .remove("logged_in_phone")
            .remove("user_id")
            .remove("website_live_url")
            .remove("github_repo_url")
            .remove("google_spreadsheet_url")
            .putBoolean("is_sheet_verified", false)
            .apply()
    }

    private fun saveAuthSession(user: AuthUser, sheetUrl: String?) {
        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        val edit = prefs.edit()
            .putString("logged_in_name", user.name ?: "Store Owner")
            .putString("logged_in_email", user.email ?: "")
            .putString("logged_in_phone", user.phone ?: "")
            .putString("user_id", user.id ?: "")
            .putString("website_live_url", user.websiteLiveUrl ?: "")
            .putString("github_repo_url", user.githubRepoUrl ?: "")
        if (!sheetUrl.isNullOrEmpty()) {
            edit.putString("sheet_web_app_url", sheetUrl)
        }
        edit.apply()
    }

    private fun showAuthDialog(authBtn: Button) {
        openAccountFullScreenView()
    }

    private fun performMasterLogin(
        emailOrPhone: String,
        pass: String,
        authBtn: Button,
        callback: ((Boolean, String) -> Unit)? = null
    ) {
        Toast.makeText(this, "Verifying credentials...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.login(
                    fullUrl = MASTER_AUTH_URL,
                    email = emailOrPhone,
                    pass = pass
                )
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success && body.user != null) {
                            val user = body.user
                            val fetchedSheetUrl = user.sheetUrl

                            saveAuthSession(user, fetchedSheetUrl)
                            authBtn.text = "👤 ${user.name ?: "User"}"

                            if (!fetchedSheetUrl.isNullOrEmpty() && fetchedSheetUrl.contains("script.google.com")) {
                                getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE).edit()
                                    .putBoolean("is_sheet_verified", true)
                                    .apply()
                                Toast.makeText(this@MainActivity, "Namaste ${user.name}! Dukaan Unlocked ✓", Toast.LENGTH_LONG).show()
                                if (callback != null) {
                                    callback(true, "")
                                } else {
                                    showDashboardHome()
                                }
                            } else {
                                Toast.makeText(this@MainActivity, "Login Success! Sheet Pending.", Toast.LENGTH_LONG).show()
                                if (callback != null) {
                                    callback(true, "")
                                } else {
                                    showFullScreenLoginScreen()
                                }
                            }
                        } else {
                            val msg = "Galat Mobile Number ya Password!"
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            callback?.invoke(false, msg)
                        }
                    } else {
                        val msg = "Server verification failed (Code: ${response.code()})"
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        callback?.invoke(false, msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = "Network error: ${e.localizedMessage}"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    callback?.invoke(false, msg)
                }
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
                        REQUEST_PICK_LOGO_HUB -> {
                            val scaled = scaleBitmap(originalBitmap, 400, 400)
                            val base64 = bitmapToBase64(scaled)
                            activeHubLogoPreview?.setImageBitmap(scaled)
                            activeHubMockupLogo?.setImageBitmap(scaled)
                            topAvatarView.text = ""
                            topAvatarView.background = BitmapDrawable(resources, scaled)

                            Toast.makeText(this, "Uploading logo to Google Drive... ⏳", Toast.LENGTH_SHORT).show()

                            val sheetUrl = getSheetUrl()
                            if (sheetUrl.isNotEmpty()) {
                                val body = mapOf(
                                    "action" to "uploadImage",
                                    "filename" to "store_logo_${System.currentTimeMillis()}.jpg",
                                    "base64" to base64,
                                    "mimeType" to "image/jpeg",
                                    "updateLogo" to "true"
                                )
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val resp = ApiClient.service.uploadImage(sheetUrl, body).execute()
                                        withContext(Dispatchers.Main) {
                                            if (resp.isSuccessful && resp.body()?.success == true) {
                                                val driveUrl = resp.body()?.url ?: ""
                                                if (driveUrl.isNotEmpty()) {
                                                    activeHubLogoInput?.setText(driveUrl)
                                                    val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                                                    s["logo_url"] = driveUrl
                                                    dashboardData = dashboardData?.copy(settings = s)
                                                    getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                                                        .edit().putString("store_logo_url", driveUrl).apply()
                                                    Toast.makeText(this@MainActivity, "🎉 Logo Google Drive me save ho gaya aur live update ho gaya!", Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                val dataUrl = "data:image/jpeg;base64,$base64"
                                                activeHubLogoInput?.setText(dataUrl)
                                                Toast.makeText(this@MainActivity, "Logo photo chuni gayi! (Local preview set)", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            val dataUrl = "data:image/jpeg;base64,$base64"
                                            activeHubLogoInput?.setText(dataUrl)
                                            Toast.makeText(this@MainActivity, "Logo local preview set ho gaya!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                val dataUrl = "data:image/jpeg;base64,$base64"
                                activeHubLogoInput?.setText(dataUrl)
                            }
                        }
                        REQUEST_PICK_LOGO -> {
                            val scaled = scaleBitmap(originalBitmap, 300, 300)
                            val base64 = bitmapToBase64(scaled)
                            val dataUrl = "data:image/jpeg;base64,$base64"
                            pendingSettings["logo_url"] = dataUrl
                            if (::logoUrlInput.isInitialized && logoUrlInput.isAttachedToWindow) {
                                logoUrlInput.setText(dataUrl)
                            }
                            liveLogoPreview?.setImageBitmap(scaled)
                            liveLogoPreview?.visibility = View.VISIBLE
                            Toast.makeText(this, "Logo photo chuni gayi! (300x300 px)", Toast.LENGTH_SHORT).show()
                        }
                        REQUEST_PICK_BANNER -> {
                            val scaled = scaleBitmap(originalBitmap, 1200, 500)
                            val base64 = bitmapToBase64(scaled)
                            val dataUrl = "data:image/jpeg;base64,$base64"
                            pendingSettings["banner_url"] = dataUrl
                            if (::bannerUrlInput.isInitialized && bannerUrlInput.isAttachedToWindow) {
                                bannerUrlInput.setText(dataUrl)
                            }
                            liveBannerPreview?.setImageBitmap(scaled)
                            liveBannerPreview?.visibility = View.VISIBLE
                            Toast.makeText(this, "Banner photo chuni gayi! (1200x500 px)", Toast.LENGTH_SHORT).show()
                        }
                        REQUEST_PICK_QR -> {
                            val scaled = scaleBitmap(originalBitmap, 400, 400)
                            val base64 = bitmapToBase64(scaled)
                            val dataUrl = "data:image/jpeg;base64,$base64"
                            pendingSettings["qr_code_url"] = dataUrl
                            if (::qrCodeUrlInput.isInitialized && qrCodeUrlInput.isAttachedToWindow) {
                                qrCodeUrlInput.setText(dataUrl)
                            }
                            liveQrPreview?.setImageBitmap(scaled)
                            liveQrPreview?.visibility = View.VISIBLE
                            Toast.makeText(this, "UPI QR Code photo chuni gayi!", Toast.LENGTH_SHORT).show()
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
        // Sensitive permissions are voluntary and requested via showAutoSyncSettingsDialog()
    }
}
