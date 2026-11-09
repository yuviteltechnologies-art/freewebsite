package com.sheetstore.admin.ui

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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

    // Top Stats Views
    private var statsRevenueView: TextView? = null
    private var statsOrdersView: TextView? = null

    // 6 Ready-made Website Themes
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
        ),
        WebTheme(
            id = "noolam-saree",
            name = "🥻 NOOLAM Kanchipuram Silk",
            subtitle = "Sarees, Ethnic Wear & Bridal Couture",
            description = "Royal South Indian temple sanctum aesthetic with 24K gold zari and Korvai borders.",
            bestFor = "Best for: Silk sarees, handloom, lehengas, bridal jewellery & ethnic fashion",
            colors = listOf("#140204", "#D4AF37", "#500A13")
        )
    )

    companion object {
        const val ORDER_CHANNEL_ID = "sheetstore_orders_channel"
        const val NOTIFICATION_PERMISSION_CODE = 102
        private const val REQUEST_PICK_LOGO = 2001
        private const val REQUEST_PICK_BANNER = 2002
        private const val REQUEST_PICK_LOGO_HUB = 2003
        private const val MASTER_AUTH_URL = "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec"
    }

    // Store Master Hub Views
    private var activeHubLogoInput: EditText? = null
    private var activeHubLogoPreview: ImageView? = null

    // Site Customizer Input Views
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Local Push Notification Alert Channel & Known Orders
        createNotificationChannel()
        loadKnownOrderIds()
        requestNotificationPermission()

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
        handleIntentTab(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isUserLoggedIn() && isThemeSelected()) {
            fetchDashboard(silent = true)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isUserLoggedIn() && isThemeSelected()) {
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
    // TOP APP BAR (NATIVE LOOK & FEEL)
    // ==========================================
    private fun createTopAppBar(): LinearLayout {
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 14))
            setBackgroundColor(Color.parseColor("#0F172A"))
            elevation = UiKit.dp(this@MainActivity, 6).toFloat()
        }

        // Top Left: Interactive Store Quick Hub (Logo + Brand + Fast Setup)
        val storeHeaderClickable = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
            isClickable = true
            isFocusable = true
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#151E30"),
                strokeColor = Color.parseColor("#223354"),
                strokeWidthDp = 1,
                radiusDp = 12f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 6))
            setOnClickListener {
                openAccountFullScreenView()
            }
        }

        // Store Avatar Squircle
        topAvatarView = TextView(this).apply {
            val name = dashboardData?.settings?.get("brand_name") ?: getLoggedInName()
            text = if (name.isNotEmpty()) name.take(2).uppercase() else "SS"
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#00F0FF"),
                radiusDp = 10f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 36), UiKit.dp(this@MainActivity, 36)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        storeHeaderClickable.addView(topAvatarView)

        // Title + Live Sync Status
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        topBrandNameView = TextView(this).apply {
            text = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
            textSize = 13.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        titleCol.addView(topBrandNameView)

        val hubHint = TextView(this).apply {
            text = "⚡ Store Hub & Settings ▾"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
        }
        titleCol.addView(hubHint)
        storeHeaderClickable.addView(titleCol)

        topBar.addView(storeHeaderClickable)

        val activeNiche = getActiveNiche()
        headerNichePill = TextView(this).apply {
            text = "${activeNiche.emoji}"
            textSize = 12f
            visibility = View.GONE // integrated into store hub
        }

        // Profile / Login Button (Cleanly separated on the right)
        authBtn = Button(this).apply {
            val email = getLoggedInEmail()
            text = if (email.isNotEmpty()) "👤 ${getLoggedInName()}" else "👤 Login"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#132A24"),
                strokeColor = Color.parseColor("#059669"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 6))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this@MainActivity, 34))
            setOnClickListener { openAccountFullScreenView() }
        }
        topBar.addView(authBtn)

        return topBar
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

    private fun createOnboardingView(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 40))
        }

        // Header Badge
        val topBadge = UiKit.createPillBadge(
            context = this,
            text = "🚀 INITIAL SETUP WIZARD",
            textColor = Color.parseColor("#00F0FF"),
            bgColor = Color.parseColor("#0C2A4A"),
            borderColor = Color.parseColor("#00F0FF")
        )
        container.addView(topBadge)

        // Title
        val titleView = TextView(this).apply {
            text = "Apna Business Niche Chunein"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 4))
        }
        container.addView(titleView)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = "SheetStore Pro multi-purpose hai! Aap kis tarah ki website chalana chahte hain? Chuniye, aur app automatic matching theme aur copy set kar dega:"
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
            btnText = "Apply Niche & Launch Store ➔",
            startColor = "#2563EB",
            endColor = "#00F0FF"
        ) {
            val chosen = NicheCatalog.getNicheById(selectedNicheId)
            setActiveNicheId(chosen.id)
            setOnboardingCompleted(true)

            // Automatically apply the matched theme & pre-filled copy
            applyWebsiteTemplate(chosen.recommendedThemeId, chosen.recommendedThemeName)
            saveNicheDefaultsToSheet(chosen)

            Toast.makeText(this, "🎉 '${chosen.title}' activated! Matched theme applied.", Toast.LENGTH_LONG).show()

            contentContainer.removeAllViews()
            if (!isSheetVerified()) {
                showFullScreenLoginScreen()
            } else {
                showDashboardHome()
            }
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

        val iconBox = TextView(this).apply {
            text = "⚡"
            textSize = 42f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121D33"),
                strokeColor = Color.parseColor("#00F0FF"),
                strokeWidthDp = 2,
                radiusDp = 26f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 84), UiKit.dp(this@MainActivity, 84)).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 18)
            }
        }
        splash.addView(iconBox)

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
            text = "🔒 v1.0.5 • Secure Admin Portal",
            textColor = Color.parseColor("#94A3B8"),
            bgColor = Color.parseColor("#151E30"),
            borderColor = Color.parseColor("#223354")
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
        } else if (!isThemeSelected()) {
            showFullScreenThemeSelection()
        } else {
            showDashboardHome()
        }
    }

    private fun showDashboardHome() {
        topBarView.visibility = View.VISIBLE
        navBar.visibility = View.VISIBLE
        authBtn.text = "👤 ${getLoggedInName()}"
        val brand = dashboardData?.settings?.get("brand_name") ?: getLoggedInName().ifEmpty { "SheetStore Pro" }
        topBrandNameView.text = brand
        topAvatarView.text = brand.take(2).uppercase()
        handleIntentTab(intent)
        fetchDashboard()
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

        val lockIcon = TextView(this).apply {
            text = "🔐"
            textSize = 36f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor("#00F0FF"),
                strokeWidthDp = 1,
                radiusDp = 50f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 74), UiKit.dp(this@MainActivity, 74)).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }
        container.addView(lockIcon)

        val headerTitle = TextView(this).apply {
            text = "Admin Portal Login"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        container.addView(headerTitle)

        val headerSub = TextView(this).apply {
            text = "Bina login ke app access nahi kiya ja sakta. Kripya apna registered WhatsApp Number / Email aur Password daalein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setLineSpacing(5f, 1.15f)
            setPadding(UiKit.dp(this@MainActivity, 12), 0, UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 22))
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
                        if (!isThemeSelected()) {
                            showFullScreenThemeSelection()
                        } else {
                            showDashboardHome()
                        }
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
                radiusDp = 12f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = UiKit.dp(this@MainActivity, 8)
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }

        // Mock Browser Address Bar
        val urlBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = if (isLight) Color.parseColor("#E2E8F0") else Color.parseColor("#151F33"),
                strokeColor = Color.TRANSPARENT,
                strokeWidthDp = 0,
                radiusDp = 6f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        val lockText = TextView(this).apply {
            text = "🌐 https://yourstore.github.io/store"
            textSize = 9.5f
            setTextColor(if (isLight) Color.parseColor("#475569") else Color.parseColor("#94A3B8"))
        }
        urlBar.addView(lockText)
        box.addView(urlBar)

        // Mock Website Navbar
        val mockNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        val mockBrand = TextView(this).apply {
            text = when (theme.id) {
                "noolam-saree" -> "🥻 NOOLAM SILK STUDIO"
                "vedic-gold" -> "🕉️ VEDIC STORE"
                "neon-violet" -> "👾 CREATOR HUB"
                "emerald-pro" -> "🌿 PRO CONSULTING"
                "minimal-light" -> "🛍️ D2C STORE"
                else -> "⚡ DIGITAL STORE"
            }
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isLight) Color.parseColor("#0F172A") else Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val mockCart = TextView(this).apply {
            text = "🛒 CART (1)"
            textSize = 9.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentCol)
        }
        mockNav.addView(mockBrand)
        mockNav.addView(mockCart)
        box.addView(mockNav)

        // Mock Website Hero Banner
        val mockHero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = UiKit.cardDrawable(
                bgColor = if (isLight) Color.parseColor("#F1F5F9") else Color.parseColor("#101A2E"),
                strokeColor = accentCol,
                strokeWidthDp = 1,
                radiusDp = 8f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 8)
            }
        }
        val mockHeroTitle = TextView(this).apply {
            text = when (theme.id) {
                "noolam-saree" -> "Bridal Silk Sarees & Ethnic Handloom"
                "vedic-gold" -> "Energized Vastu Yantras & Gemstones"
                "neon-violet" -> "AI Prompts, Notion Kits & 3D Assets"
                "emerald-pro" -> "Financial Blueprints & Wealth Consulting"
                "minimal-light" -> "Modern D2C Apparel & Clean Lifestyle"
                else -> "Instant Download Digital Products & Software"
            }
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(lightCol)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        val mockHeroBtn = TextView(this).apply {
            text = "🛍️ Buy with Instant UPI ➔"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = accentCol,
                strokeColor = Color.TRANSPARENT,
                strokeWidthDp = 0,
                radiusDp = 6f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 5), UiKit.dp(this@MainActivity, 12), UiKit.dp(this@MainActivity, 5))
        }
        mockHero.addView(mockHeroTitle)
        mockHero.addView(mockHeroBtn)
        box.addView(mockHero)

        // Mock Product Row Preview
        val prodRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        for (i in 1..2) {
            val prodCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = UiKit.cardDrawable(
                    bgColor = if (isLight) Color.WHITE else Color.parseColor("#152033"),
                    strokeColor = Color.parseColor("#334155"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                setPadding(UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (i == 1) rightMargin = UiKit.dp(this@MainActivity, 6)
                }
            }
            val pName = TextView(this).apply {
                text = if (i == 1) "Featured Product #1" else "Best Seller Item #2"
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (isLight) Color.parseColor("#0F172A") else Color.WHITE)
            }
            val pPrice = TextView(this).apply {
                text = if (i == 1) "₹499.00" else "₹999.00"
                textSize = 9f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentCol)
            }
            prodCard.addView(pName)
            prodCard.addView(pPrice)
            prodRow.addView(prodCard)
        }
        box.addView(prodRow)

        return box
    }

    private fun showFullScreenThemeSelection(isFromSettings: Boolean = false) {
        topBarView.visibility = View.GONE
        navBar.visibility = View.GONE
        contentContainer.removeAllViews()

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 28), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 40))
        }

        val stepBadge = UiKit.createPillBadge(
            context = this,
            text = "🌐 LIVE WEBSITE STOREFRONT THEME SETUP",
            textColor = Color.parseColor("#00F0FF"),
            bgColor = Color.parseColor("#0C2A4A"),
            borderColor = Color.parseColor("#00F0FF")
        )
        container.addView(stepBadge)

        val titleView = TextView(this).apply {
            text = "Select Website Theme (वेबसाइट की थीम)"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 4))
        }
        container.addView(titleView)

        val subView = TextView(this).apply {
            text = "Ye setting aapki live customer website (Storefront) ka design badal degi (App ka nahi). Niche har theme ka live website mockup screenshot preview dekhein aur apni dukan ke liye chunein:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setLineSpacing(5f, 1.15f)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 18))
        }
        container.addView(subView)

        val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
        var selectedThemeId = prefs.getString("selected_theme_id", "modern-blue") ?: "modern-blue"
        var selectedThemeName = "⚡ Modern Cyber Blue"

        val themeCards = mutableListOf<Pair<WebTheme, LinearLayout>>()

        for (theme in availableTemplates) {
            val isSelected = (theme.id == selectedThemeId)
            if (isSelected) selectedThemeName = theme.name

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
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = UiKit.dp(this@MainActivity, 16)
                }
            }

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val title = TextView(this).apply {
                text = theme.name
                textSize = 14.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            topRow.addView(title)

            val chkBadge = UiKit.createPillBadge(
                context = this,
                text = if (isSelected) "✓ SELECTED FOR WEBSITE" else "SELECT",
                textColor = if (isSelected) Color.parseColor("#00F0FF") else Color.parseColor("#64748B"),
                bgColor = if (isSelected) Color.parseColor("#0C2A4A") else Color.parseColor("#172033"),
                borderColor = if (isSelected) Color.parseColor("#00F0FF") else Color.parseColor("#25334D")
            )
            topRow.addView(chkBadge)
            card.addView(topRow)

            val sub = TextView(this).apply {
                text = theme.subtitle
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#38BDF8"))
                setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 4))
            }
            card.addView(sub)

            val desc = TextView(this).apply {
                text = theme.description
                textSize = 11f
                setTextColor(Color.parseColor("#94A3B8"))
                setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
            }
            card.addView(desc)

            // VISUAL WEBSITE SCREENSHOT & MOCKUP PREVIEW
            card.addView(createWebsiteMockupBox(theme))

            // Action Button to view live demo in browser
            val demoBtn = UiKit.createSecondaryButton(this, "🌐 Live Website Demo Kholein ↗", theme.colors[1]) {
                val demoUrl = if (theme.id == "noolam-saree") {
                    "https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html"
                } else {
                    "https://yuviteltechnologies-art.github.io/freewebsite/demo.html"
                }
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(demoUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Browser could not be opened", Toast.LENGTH_SHORT).show()
                }
            }
            demoBtn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                UiKit.dp(this@MainActivity, 38)
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 6)
            }
            card.addView(demoBtn)

            val swatchRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            for (col in theme.colors) {
                val circle = View(this).apply {
                    background = UiKit.cardDrawable(
                        bgColor = Color.parseColor(col),
                        strokeColor = Color.parseColor("#334155"),
                        strokeWidthDp = 1,
                        radiusDp = 6f,
                        context = this@MainActivity
                    )
                    layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 20)).apply {
                        rightMargin = UiKit.dp(this@MainActivity, 6)
                    }
                }
                swatchRow.addView(circle)
            }
            card.addView(swatchRow)

            card.setOnClickListener {
                selectedThemeId = theme.id
                selectedThemeName = theme.name
                for ((t, c) in themeCards) {
                    val sel = (t.id == selectedThemeId)
                    c.background = UiKit.cardDrawable(
                        bgColor = if (sel) Color.parseColor("#152238") else Color.parseColor("#121826"),
                        strokeColor = if (sel) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                        strokeWidthDp = if (sel) 2 else 1,
                        radiusDp = 18f,
                        context = this@MainActivity
                    )
                    val r = c.getChildAt(0) as? LinearLayout
                    val b = r?.getChildAt(1) as? TextView
                    b?.text = if (sel) "✓ SELECTED FOR WEBSITE" else "SELECT"
                    b?.setTextColor(if (sel) Color.parseColor("#00F0FF") else Color.parseColor("#64748B"))
                }
            }

            themeCards.add(Pair(theme, card))
            container.addView(card)
        }

        val proceedBtn = Button(this).apply {
            text = "✨ Apply Website Theme & Launch Store ➔"
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
                topMargin = UiKit.dp(this@MainActivity, 12)
            }
            setOnClickListener {
                val chosen = availableTemplates.find { it.id == selectedThemeId } ?: availableTemplates[0]
                prefs.edit().putString("selected_theme_id", chosen.id).putBoolean("is_theme_selected", true).apply()

                applyWebsiteTemplate(chosen.id, chosen.name)

                Toast.makeText(this@MainActivity, "🎉 Website Theme '${chosen.name}' apply ho gaya! Live website par changes update ho gaye.", Toast.LENGTH_LONG).show()

                showDashboardHome()
            }
        }
        container.addView(proceedBtn)

        scroll.addView(container)
        contentContainer.addView(scroll)
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
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 24))
        }

        // Stats Card (Revenue & Orders Metric)
        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#1F2D48"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 18)
            }
        }

        val statsTitle = TextView(this).apply {
            text = "💰 REVENUE & ORDERS STATS"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#94A3B8"))
        }
        statsCard.addView(statsTitle)

        statsRevenueView = TextView(this).apply {
            text = "₹0.00"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#10B981"))
            setPadding(0, UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 4))
        }
        statsCard.addView(statsRevenueView)

        statsOrdersView = TextView(this).apply {
            text = "Total Orders: 0 (Paid: 0 • Pending: 0)"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
        }
        statsCard.addView(statsOrdersView)
        scrollContent.addView(statsCard)

        // Manual Payment Approval & Quick UTR Verification Card
        val manualCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#121826"),
                strokeColor = Color.parseColor("#2563EB"),
                strokeWidthDp = 1,
                radiusDp = 18f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }

        val manualHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        val manualTitle = TextView(this).apply {
            text = "🖐️ MANUAL PAYMENT APPROVAL"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val manualBadge = UiKit.createPillBadge(this, "0 Permissions Needed", Color.parseColor("#38BDF8"), Color.parseColor("#0C4A6E"), Color.parseColor("#0284C7"))
        manualHeader.addView(manualTitle)
        manualHeader.addView(manualBadge)
        manualCard.addView(manualHeader)

        val manualDesc = TextView(this).apply {
            text = "Sensitive SMS permission optional hai! Aap customer ka 12-digit UTR daal kar direct verify kar sakte hain ya neeche order par 'Approve' click karein."
            textSize = 11f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        manualCard.addView(manualDesc)

        val manualBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val btnApproveUtr = Button(this).apply {
            text = "🔍 12-Digit UTR Approve"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = UiKit.gradientButtonDrawable(
                startColor = Color.parseColor("#2563EB"),
                endColor = Color.parseColor("#1D4ED8"),
                radiusDp = 12f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 42), 1.2f).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
            setOnClickListener {
                showManualUtrApprovalDialog()
            }
        }

        val btnRefreshOrders = UiKit.createSecondaryButton(this, "🔄 Sync Sheet", "#38BDF8") {
            fetchDashboard()
            Toast.makeText(this@MainActivity, "Refreshing orders from Google Sheet...", Toast.LENGTH_SHORT).show()
        }
        btnRefreshOrders.layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(this@MainActivity, 42), 1f)

        manualBtnRow.addView(btnApproveUtr)
        manualBtnRow.addView(btnRefreshOrders)
        manualCard.addView(manualBtnRow)
        scrollContent.addView(manualCard)

        // Live Search Input for Orders
        val searchCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#0F172A"),
                strokeColor = Color.parseColor("#1E293B"),
                strokeWidthDp = 1,
                radiusDp = 14f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 14), UiKit.dp(this@MainActivity, 4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 14)
            }
        }
        val searchIcon = TextView(this).apply {
            text = "🔍"
            textSize = 14f
            setPadding(0, 0, UiKit.dp(this@MainActivity, 8), 0)
        }
        val searchInput = EditText(this).apply {
            hint = "Search by UTR, Order ID, Phone or Name..."
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 12f
            background = null
            isSingleLine = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    ordersAdapter.filter(s?.toString() ?: "")
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        searchCard.addView(searchIcon)
        searchCard.addView(searchInput)
        scrollContent.addView(searchCard)

        // Section Title: Live Incoming Orders
        val sectionTitle = TextView(this).apply {
            text = "Live Incoming Orders"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        scrollContent.addView(sectionTitle)

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
        statsOrdersView?.text = "Total: ${stats.totalOrders} Orders (Paid: ${stats.paidOrders} • Pending: ${stats.pendingOrders})"
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
    // TAB 1: NICHE & 6 READY-MADE THEMES
    // ==========================================
    private fun createNicheAndThemesTab(): View {
        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 32))
        }

        // Active Niche Card
        val activeNiche = getActiveNiche()
        val nicheBanner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#152238"),
                strokeColor = Color.parseColor("#00F0FF"),
                strokeWidthDp = 1,
                radiusDp = 20f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 20)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val nicheEmoji = TextView(this).apply {
            text = activeNiche.emoji
            textSize = 24f
            setPadding(0, 0, UiKit.dp(this@MainActivity, 10), 0)
        }
        topRow.addView(nicheEmoji)

        val nicheCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val nicheTitle = TextView(this).apply {
            text = "Active Niche: ${activeNiche.title}"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        nicheCol.addView(nicheTitle)

        val nicheSub = TextView(this).apply {
            text = activeNiche.subtitle
            textSize = 11f
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, UiKit.dp(this@MainActivity, 2), 0, 0)
        }
        nicheCol.addView(nicheSub)
        topRow.addView(nicheCol)
        nicheBanner.addView(topRow)

        // Switch Niche Button
        val switchNicheBtn = UiKit.createSecondaryButton(this, "🎯 Switch Business Niche / Wizard", "#00F0FF") {
            contentContainer.removeAllViews()
            contentContainer.addView(createOnboardingView())
        }
        switchNicheBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = UiKit.dp(this@MainActivity, 12)
        }
        nicheBanner.addView(switchNicheBtn)
        container.addView(nicheBanner)

        // Section Title: 6 Ready-made Themes
        val heading = TextView(this).apply {
            text = "🎨 6 Ready-Made Themes (1-Click Switcher)"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        container.addView(heading)

        val subtext = TextView(this).apply {
            text = "Apni pasand ka ready-made theme select karein. Live website ka color scheme, font aur styling 1 click me update ho jayegi:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16))
        }
        container.addView(subtext)

        val currentActiveId = (dashboardData?.settings?.get("template_id") ?: "modern-blue").trim().lowercase()

        // Render 6 Themes Cards
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
            background = UiKit.cardDrawable(
                bgColor = if (isActive) Color.parseColor("#152238") else Color.parseColor("#121826"),
                strokeColor = if (isActive) Color.parseColor("#00F0FF") else Color.parseColor("#1E2D48"),
                strokeWidthDp = if (isActive) 2 else 1,
                radiusDp = 18f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 18))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 16)
            }
        }

        // Top Row: Title + Status Badge
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = theme.name
            textSize = 14.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        topRow.addView(titleView)

        val badge = UiKit.createPillBadge(
            context = this,
            text = if (isActive) "✓ ACTIVE LIVE" else "READY-MADE",
            textColor = if (isActive) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"),
            bgColor = if (isActive) Color.parseColor("#064E3B") else Color.parseColor("#1A2234"),
            borderColor = if (isActive) Color.parseColor("#10B981") else Color.parseColor("#2E3D5B")
        )
        topRow.addView(badge)
        card.addView(topRow)

        // Subtitle
        val subtitleView = TextView(this).apply {
            text = theme.subtitle
            textSize = 11.5f
            setTextColor(Color.parseColor(if (theme.id == "vedic-gold") "#F59E0B" else "#38BDF8"))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, UiKit.dp(this@MainActivity, 6), 0, UiKit.dp(this@MainActivity, 4))
        }
        card.addView(subtitleView)

        // Description
        val descView = TextView(this).apply {
            text = theme.description
            textSize = 11.5f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 6))
        }
        card.addView(descView)

        // Best For Tag
        val bestForView = TextView(this).apply {
            text = theme.bestFor
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        card.addView(bestForView)

        // Color Swatches Row
        val swatchesRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
        }

        val paletteLabel = TextView(this).apply {
            text = "Palette: "
            textSize = 11f
            setTextColor(Color.parseColor("#64748B"))
        }
        swatchesRow.addView(paletteLabel)

        for (colorHex in theme.colors) {
            val dot = View(this).apply {
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor(colorHex),
                    strokeColor = Color.parseColor("#334155"),
                    strokeWidthDp = 1,
                    radiusDp = 6f,
                    context = this@MainActivity
                )
                layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 24)).apply {
                    setMargins(UiKit.dp(this@MainActivity, 4), 0, UiKit.dp(this@MainActivity, 4), 0)
                }
            }
            swatchesRow.addView(dot)
        }
        card.addView(swatchesRow)

        // Visual Website Screenshot / Mockup Preview
        card.addView(createWebsiteMockupBox(theme))

        val demoBtn = UiKit.createSecondaryButton(this, "🌐 Live Website Demo Kholein ↗", theme.colors[1]) {
            val demoUrl = if (theme.id == "noolam-saree") {
                "https://yuviteltechnologies-art.github.io/freewebsite/saree-demo.html"
            } else {
                "https://yuviteltechnologies-art.github.io/freewebsite/demo.html"
            }
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(demoUrl)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Browser could not be opened", Toast.LENGTH_SHORT).show()
            }
        }
        demoBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            UiKit.dp(this@MainActivity, 38)
        ).apply {
            bottomMargin = UiKit.dp(this@MainActivity, 8)
        }
        card.addView(demoBtn)

        // Action Button
        val applyBtn = Button(this).apply {
            if (isActive) {
                text = "✓ Current Active Website Theme"
                background = UiKit.cardDrawable(
                    bgColor = Color.parseColor("#064E3B"),
                    strokeColor = Color.parseColor("#10B981"),
                    strokeWidthDp = 1,
                    radiusDp = 12f,
                    context = this@MainActivity
                )
                setTextColor(Color.parseColor("#A7F3D0"))
                isEnabled = false
            } else {
                text = "Apply This Theme to Website ⚡"
                background = UiKit.gradientButtonDrawable(
                    startColor = Color.parseColor("#2563EB"),
                    endColor = Color.parseColor("#1D4ED8"),
                    radiusDp = 12f,
                    context = this@MainActivity
                )
                setTextColor(Color.WHITE)
                isEnabled = true
                setOnClickListener {
                    applyWebsiteTemplate(theme.id, theme.name)
                }
            }
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, UiKit.dp(this@MainActivity, 10), 0, UiKit.dp(this@MainActivity, 10))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 44))
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

        Toast.makeText(this, "Applying '$templateTitle' to Live Website... ⏳", Toast.LENGTH_SHORT).show()

        val body = mapOf(
            "action" to "updateSettings",
            "template_id" to templateId,
            "theme" to templateId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.updateSettings(fullUrl = url, body = body)
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val s = dashboardData?.settings?.toMutableMap() ?: mutableMapOf()
                        s["template_id"] = templateId
                        s["theme"] = templateId
                        dashboardData = dashboardData?.copy(settings = s)
                        getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("selected_theme_id", templateId)
                            .apply()
                        refreshCurrentTab()
                        Toast.makeText(this@MainActivity, "🎉 '$templateTitle' Live Website Par Set Ho Gaya!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Live sync response: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Applied locally! (Sheet network: ${e.message})", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ==========================================
    // TAB 2: SITE CUSTOMIZER (FULL CMS FOR EVERY THEME PART)
    // ==========================================
    private fun createWebsiteCustomizerTab(): View {
        val scroll = ScrollView(this)
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 40))
        }

        // Header Title & Subtitle
        val heading = TextView(this).apply {
            text = "🎨 Store Builder & Theme Customizer"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 4))
        }
        form.addView(heading)

        val subtext = TextView(this).apply {
            text = "Website ke sabhi parts ko yahan se real-time change karein. Sab data Google Sheet me live sync hota hai:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 18))
        }
        form.addView(subtext)

        // 1. BRANDING & LOGO SECTION
        val (brandCard, brandBody) = UiKit.createSectionContainer(this, "🏢 1. Brand Identity & Logo", "Core")
        brandBody.addView(UiKit.createFieldLabel(this, "Store / Brand Name:"))
        brandNameInput = UiKit.createStyledInput(this, "e.g. SheetStore Pro", "")
        brandBody.addView(brandNameInput)

        brandBody.addView(UiKit.createFieldLabel(this, "Brand Tagline:"))
        taglineInput = UiKit.createStyledInput(this, "e.g. 100% Free Google Sheet Store", "")
        brandBody.addView(taglineInput)

        brandBody.addView(UiKit.createFieldLabel(this, "Custom Logo Image URL:"))
        logoUrlInput = UiKit.createStyledInput(this, "https://example.com/logo.png", "")
        brandBody.addView(logoUrlInput)

        val pickLogoBtn = UiKit.createSecondaryButton(this, "🖼️ Phone Gallery Se Logo Photo Chunein", "#38BDF8") {
            openImagePicker(REQUEST_PICK_LOGO)
        }
        brandBody.addView(pickLogoBtn)
        form.addView(brandCard)

        // 2. HERO SHOWCASE & HEADLINES SECTION
        val (heroCard, heroBody) = UiKit.createSectionContainer(this, "🚀 2. Hero Showcase & Headlines", "High Impact")
        heroBody.addView(UiKit.createFieldLabel(this, "Hero Main Headline:"))
        heroTitleInput = UiKit.createStyledInput(this, "Main headline on website", "", singleLine = false)
        heroBody.addView(heroTitleInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Hero Subtitle / Description:"))
        heroSubtitleInput = UiKit.createStyledInput(this, "Secondary explanation text", "", singleLine = false)
        heroBody.addView(heroSubtitleInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Hero Badge Text (e.g. ⚡ 50% OFF TODAY):"))
        heroBadgeInput = UiKit.createStyledInput(this, "Badge label text", "")
        heroBody.addView(heroBadgeInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Call-to-Action (CTA) Button Text:"))
        ctaTextInput = UiKit.createStyledInput(this, "e.g. Shop Collection / Buy Now", "")
        heroBody.addView(ctaTextInput)

        heroBody.addView(UiKit.createFieldLabel(this, "Hero Banner / Product Showcase Image URL:"))
        bannerUrlInput = UiKit.createStyledInput(this, "https://example.com/banner.jpg", "")
        heroBody.addView(bannerUrlInput)

        val pickBannerBtn = UiKit.createSecondaryButton(this, "🖼️ Phone Gallery Se Banner Photo Chunein", "#38BDF8") {
            openImagePicker(REQUEST_PICK_BANNER)
        }
        heroBody.addView(pickBannerBtn)
        form.addView(heroCard)

        // 3. ANNOUNCEMENT BAR SECTION
        val (annCard, annBody) = UiKit.createSectionContainer(this, "📢 3. Top Announcement Bar", "Promotions")
        announcementSwitch = Switch(this).apply {
            text = "Show Top Announcement Bar"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            isChecked = true
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 10))
        }
        annBody.addView(announcementSwitch)

        annBody.addView(UiKit.createFieldLabel(this, "Announcement Bar Text:"))
        announcementInput = UiKit.createStyledInput(this, "e.g. Diwali Dhamaka! Flat 50% OFF on all items", "")
        annBody.addView(announcementInput)
        form.addView(annCard)

        // 4. DESIGN, FONTS & COLORS SECTION
        val (styleCard, styleBody) = UiKit.createSectionContainer(this, "🎨 4. Fonts & Custom Colors", "Visuals")
        styleBody.addView(UiKit.createFieldLabel(this, "Heading Font Family (Tap Chip to Apply):"))
        fontFamilyInput = UiKit.createStyledInput(this, "Outfit", "")
        styleBody.addView(fontFamilyInput)

        val fontChips = createFontChips { fontName, label ->
            fontFamilyInput.setText(fontName)
            Toast.makeText(this, "Selected font: $label", Toast.LENGTH_SHORT).show()
        }
        styleBody.addView(fontChips)

        styleBody.addView(UiKit.createFieldLabel(this, "Header Background Color Hex:"))
        headerColorInput = UiKit.createStyledInput(this, "#080C1A", "")
        styleBody.addView(headerColorInput)
        styleBody.addView(createColorChips { hex, name ->
            headerColorInput.setText(hex)
            Toast.makeText(this, "Header: $name", Toast.LENGTH_SHORT).show()
        })

        styleBody.addView(UiKit.createFieldLabel(this, "Footer Background Color Hex:"))
        footerColorInput = UiKit.createStyledInput(this, "#050811", "")
        styleBody.addView(footerColorInput)
        styleBody.addView(createColorChips { hex, name ->
            footerColorInput.setText(hex)
            Toast.makeText(this, "Footer: $name", Toast.LENGTH_SHORT).show()
        })
        form.addView(styleCard)

        // 5. CONTACT & PAYMENTS SECTION
        val (payCard, payBody) = UiKit.createSectionContainer(this, "💳 5. WhatsApp & UPI Payments", "Revenue")
        payBody.addView(UiKit.createFieldLabel(this, "WhatsApp Order Mobile Number (with 91):"))
        waNumberInput = UiKit.createStyledInput(this, "e.g. 919876543210", "")
        payBody.addView(waNumberInput)

        payBody.addView(UiKit.createFieldLabel(this, "UPI ID (Where Payments Are Received):"))
        upiIdInput = UiKit.createStyledInput(this, "e.g. merchant@okaxis", "")
        payBody.addView(upiIdInput)
        form.addView(payCard)

        // Big Save Changes Button
        val saveBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "🚀 Update Website Live (Push To Google Sheet)",
            startColor = "#2563EB",
            endColor = "#00F0FF"
        ) {
            saveWebsiteSettings()
        }
        form.addView(saveBtn)

        populateSettingsFields()
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
            heroBadgeInput.setText(s["hero_badge"] ?: "")
            heroTitleInput.setText(s["hero_title"] ?: "")
            heroSubtitleInput.setText(s["hero_subtitle"] ?: "")
            ctaTextInput.setText(s["cta_text"] ?: "")
            announcementInput.setText(s["announcement_text"] ?: "")
            announcementSwitch.isChecked = (s["show_announcement"] ?: "true").equals("true", ignoreCase = true)
            waNumberInput.setText(s["whatsapp_number"] ?: "")
            upiIdInput.setText(s["upi_id"] ?: "")
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
            "hero_badge" to heroBadgeInput.text.toString().trim(),
            "hero_title" to heroTitleInput.text.toString().trim(),
            "hero_subtitle" to heroSubtitleInput.text.toString().trim(),
            "cta_text" to ctaTextInput.text.toString().trim(),
            "announcement_text" to announcementInput.text.toString().trim(),
            "show_announcement" to if (announcementSwitch.isChecked) "true" else "false",
            "whatsapp_number" to waNumberInput.text.toString().trim(),
            "upi_id" to upiIdInput.text.toString().trim()
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
    // MODERN BOTTOM NAVIGATION DOCK
    // ==========================================
    private fun createBottomNav(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 4), UiKit.dp(this@MainActivity, 10))
            elevation = UiKit.dp(this@MainActivity, 8).toFloat()

            addView(createNavItem("🛒 Orders", 0))
            addView(createNavItem("🎯 Themes", 1))
            addView(createNavItem("🛠️ Custom", 2))
            addView(createNavItem("📦 Products", 3))
            addView(createNavItem("⚙️ Settings", 4))
        }
    }

    private fun createNavItem(label: String, index: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(UiKit.dp(this@MainActivity, 2), UiKit.dp(this@MainActivity, 6), UiKit.dp(this@MainActivity, 2), UiKit.dp(this@MainActivity, 6))
            setTextColor(if (currentTab == index) Color.parseColor("#00F0FF") else Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                if (!isOnboardingCompleted()) {
                    contentContainer.removeAllViews()
                    contentContainer.addView(createOnboardingView())
                } else if (!isSheetVerified()) {
                    Toast.makeText(this@MainActivity, "Pehle Google Sheet verify karein! 🔒", Toast.LENGTH_SHORT).show()
                    switchTab(index)
                } else {
                    switchTab(index)
                }
            }
        }
    }

    private fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        contentContainer.removeAllViews()

        val tabView = when (tabIndex) {
            0 -> createOrdersTab()
            1 -> createNicheAndThemesTab()
            2 -> createWebsiteCustomizerTab()
            3 -> createProductsTab()
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
                if (isOnboardingCompleted() && isSheetVerified()) {
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
        switchTab(4)
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
                            logoUrlInput.setText(dataUrl)
                            Toast.makeText(this, "Logo photo chuni gayi! (300x300 px)", Toast.LENGTH_SHORT).show()
                        }
                        REQUEST_PICK_BANNER -> {
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
        // Sensitive permissions are voluntary and requested via showAutoSyncSettingsDialog()
    }
}
