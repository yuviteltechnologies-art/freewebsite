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

    private var currentTab = 0 // 0: Orders, 1: Niche & Themes, 2: Customizer, 3: Products
    private var dashboardData: DashboardResponse? = null

    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var authBtn: Button
    private lateinit var headerNichePill: TextView
    private lateinit var topAvatarView: TextView
    private lateinit var topBrandNameView: TextView

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
        private const val MASTER_AUTH_URL = "https://script.google.com/macros/s/AKfycbyyzqzTbww2QQeQffEjY6ckDGMjgeLBVwd4KwlID99ZkXxTQiuKOzNqXJbr_fKcNZ6t/exec"
    }

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
        val topBar = createTopAppBar()
        root.addView(topBar)

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

        // Flow check: If user hasn't completed multi-niche onboarding, show Onboarding Wizard!
        if (!isOnboardingCompleted()) {
            contentContainer.addView(createOnboardingView())
            updateNavSelection()
        } else if (!isSheetVerified()) {
            contentContainer.addView(createRestrictedLockScreen())
            updateNavSelection()
            showAuthDialog(authBtn)
        } else {
            switchTab(0)
            fetchDashboard()
        }

        // Start Background Auto-Polling for incoming orders
        startAutoPolling()
        handleIntentTab(intent)
    }

    override fun onResume() {
        super.onResume()
        if (isOnboardingCompleted() && isSheetVerified()) {
            fetchDashboard(silent = true)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentTab(intent)
    }

    private fun handleIntentTab(intent: Intent?) {
        val tab = intent?.getIntExtra("open_tab", -1) ?: -1
        if (tab >= 0) {
            switchTab(tab)
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
                showStoreQuickHubAndSettingsDialog()
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
            text = "⚡ Store Hub & QR ▾"
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

        // Profile Button
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, UiKit.dp(this@MainActivity, 34)).apply {
                rightMargin = UiKit.dp(this@MainActivity, 8)
            }
            setOnClickListener { showAuthDialog(this) }
        }
        topBar.addView(authBtn)

        // Sheet URL Settings Icon
        val settingsBtn = Button(this).apply {
            text = "⚙️"
            textSize = 14f
            setTextColor(Color.WHITE)
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#1E293B"),
                strokeColor = Color.parseColor("#334155"),
                strokeWidthDp = 1,
                radiusDp = 10f,
                context = this@MainActivity
            )
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 34), UiKit.dp(this@MainActivity, 34))
            setOnClickListener { showStoreQuickHubAndSettingsDialog() }
        }
        topBar.addView(settingsBtn)

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
                contentContainer.addView(createRestrictedLockScreen())
            } else {
                switchTab(0)
                fetchDashboard()
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
        val sheetUrl = getSheetUrl()
        return verified && email.isNotEmpty() && sheetUrl.isNotEmpty()
    }

    private fun unlockAppAndLoadDashboard() {
        authBtn.text = "👤 ${getLoggedInName()}"
        switchTab(0)
        fetchDashboard()
    }

    private fun createRestrictedLockScreen(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F19"))
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 36), UiKit.dp(this@MainActivity, 24), UiKit.dp(this@MainActivity, 36))
        }

        val lockBadge = TextView(this).apply {
            text = "🔒"
            textSize = 34f
            gravity = Gravity.CENTER
            background = UiKit.cardDrawable(
                bgColor = Color.parseColor("#451A03"),
                strokeColor = Color.parseColor("#D97706"),
                strokeWidthDp = 1,
                radiusDp = 50f,
                context = this@MainActivity
            )
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16))
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(this@MainActivity, 72), UiKit.dp(this@MainActivity, 72)).apply {
                bottomMargin = UiKit.dp(this@MainActivity, 20)
            }
        }
        container.addView(lockBadge)

        val lockTitle = TextView(this).apply {
            text = "Google Sheet Verification Zaroori Hai"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        container.addView(lockTitle)

        val lockDesc = TextView(this).apply {
            text = "Aapki dukaan tabhi access hogi jab aapki Google Sheet aapke registered WhatsApp Mobile Number ya Email se verify aur connect ho jayegi."
            textSize = 12.5f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setLineSpacing(6f, 1.2f)
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 24))
        }
        container.addView(lockDesc)

        // Status Verification Card
        val (statusCard, statusBody) = UiKit.createSectionContainer(this, "🔐 Verification Checklist", "Pending")
        val isLoggedIn = getLoggedInEmail().isNotEmpty()
        val isVerified = isSheetVerified()

        val step1 = TextView(this).apply {
            text = if (isLoggedIn) "✓ 1. Mobile Number: ${getLoggedInPhone().ifEmpty { getLoggedInEmail() }} (Verified)" else "⚠️ 1. Mobile Number: Not Logged In"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isLoggedIn) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 8))
        }
        statusBody.addView(step1)

        val step2 = TextView(this).apply {
            text = if (isVerified) "✓ 2. Google Sheet Web App: Connected" else "❌ 2. Google Sheet Web App: Pending"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(if (isVerified) Color.parseColor("#10B981") else Color.parseColor("#EF4444"))
        }
        statusBody.addView(step2)
        container.addView(statusCard)

        // Button 1: Verify via Mobile Number
        val loginBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "🔐 Mobile Number Se Verify Karein 🚀",
            startColor = "#2563EB",
            endColor = "#1D4ED8"
        ) {
            showAuthDialog(authBtn)
        }
        container.addView(loginBtn)

        // Button 2: Open Dashboard to Complete Step 2
        val dashBtn = UiKit.createPrimaryButton(
            context = this,
            btnText = "🌐 Dashboard me Step 2 Connect Karein ↗",
            startColor = "#059669",
            endColor = "#047857"
        ) {
            try {
                val url = "https://yuviteltechnologies-art.github.io/freewebsite/dashboard.html"
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Could not open browser", Toast.LENGTH_SHORT).show()
            }
        }
        dashBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = UiKit.dp(this@MainActivity, 12)
        }
        container.addView(dashBtn)

        // Button 3: Manual Sheet URL Paste
        val manualBtn = UiKit.createSecondaryButton(
            context = this,
            btnText = "⚙️ Manual Web App URL Daalein"
        ) {
            showSheetUrlDialog()
        }
        manualBtn.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = UiKit.dp(this@MainActivity, 12)
        }
        container.addView(manualBtn)

        scroll.addView(container)
        return scroll
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
                        fetchDashboard()
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

    private fun approveOrderByUtr(utr: String) {
        val url = getSheetUrl()
        if (url.isEmpty()) return

        Toast.makeText(this, "Approving UTR $utr in Google Sheet...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val call = ApiClient.service.approveOrderByUtr(
                    fullUrl = url,
                    utr = utr
                )
                val response = call.execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        dashboardData?.orders?.find { it.utr == utr }?.let { order ->
                            order.status = "PAID"
                            order.verified = true
                        }
                        ordersAdapter.notifyDataSetChanged()
                        fetchDashboard()
                        Toast.makeText(this@MainActivity, "🎉 UTR $utr Marked as PAID! ✓", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Approval failed or UTR not found", Toast.LENGTH_SHORT).show()
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

        // Action Button
        val applyBtn = Button(this).apply {
            if (isActive) {
                text = "Current Active Website Theme ✓"
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
                text = "Apply This Theme ⚡"
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
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, UiKit.dp(this@MainActivity, 42))
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
            setPadding(UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 10), UiKit.dp(this@MainActivity, 8), UiKit.dp(this@MainActivity, 12))
            elevation = UiKit.dp(this@MainActivity, 8).toFloat()

            addView(createNavItem("🛒 Orders", 0))
            addView(createNavItem("🎯 Themes", 1))
            addView(createNavItem("🛠️ Customizer", 2))
            addView(createNavItem("📦 Products", 3))
        }
    }

    private fun createNavItem(label: String, index: Int): Button {
        return Button(this).apply {
            text = label
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.TRANSPARENT)
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
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", 0)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            order.orderId.hashCode(),
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
        manager.notify(order.orderId.hashCode(), notification)
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
                    unlockAppAndLoadDashboard()
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
        val dialogScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 18), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 24))
        }

        // Header with icon
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 12))
        }
        val titleText = TextView(this).apply {
            text = "⚡ Store Details & Quick Hub"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val liveBadge = UiKit.createPillBadge(this, "● Live Synced", Color.parseColor("#10B981"), Color.parseColor("#064E3B"), Color.parseColor("#10B981"))
        headerRow.addView(titleText)
        headerRow.addView(liveBadge)
        layout.addView(headerRow)

        val subHeader = TextView(this).apply {
            text = "Aapki website URL, Logo, UPI QR Code, WhatsApp number aur Google Sheet sab yahan se live update honge:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16))
        }
        layout.addView(subHeader)

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

        // 2. STORE LOGO
        val logoLabel = TextView(this).apply {
            text = "🖼️ STORE LOGO (IMAGE URL)"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, UiKit.dp(this@MainActivity, 6), 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(logoLabel)
        val currentLogo = dashboardData?.settings?.get("logo_url") ?: ""
        val logoInput = UiKit.createStyledInput(this, "https://... logo image link", currentLogo)
        layout.addView(logoInput)

        // 3. WEBSITE LIVE URL & QUICK ACTIONS
        val webLabel = TextView(this).apply {
            text = "🌐 LIVE WEBSITE URL"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#38BDF8"))
            setPadding(0, UiKit.dp(this@MainActivity, 6), 0, UiKit.dp(this@MainActivity, 6))
        }
        layout.addView(webLabel)
        val currentWebUrl = getWebsiteLiveUrl()
        val webInput = UiKit.createStyledInput(this, "https://...", currentWebUrl)
        layout.addView(webInput)

        // Action Buttons for Website
        val webBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 16))
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
        layout.addView(webBtnRow)

        // 4. UPI QR CODE & UPI ID (REAL TIME PREVIEW)
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

        // 5. WHATSAPP BUSINESS SUPPORT
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

        // 6. GOOGLE SHEET BACKEND
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
            text = "💾 Save & Update Live Website & Sheet"
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
                                Toast.makeText(this@MainActivity, "🎉 Store details & QR code live update ho gaye!", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Saved locally! (Sheet network: ${e.message})", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Toast.makeText(this@MainActivity, "Updating live settings...", Toast.LENGTH_SHORT).show()
                dialogRef?.dismiss()
            }
        }
        layout.addView(saveBtn)

        dialogScroll.addView(layout)

        dialogRef = AlertDialog.Builder(this)
            .setView(dialogScroll)
            .setNegativeButton("Close", null)
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
        val currentEmail = getLoggedInEmail()
        if (currentEmail.isNotEmpty()) {
            val prefs = getSharedPreferences("SheetStoreAdminPrefs", Context.MODE_PRIVATE)
            val name = prefs.getString("logged_in_name", "User") ?: "User"
            val phone = getLoggedInPhone()
            val userId = getUserId()
            val webUrl = getWebsiteLiveUrl()
            val sheetUrl = getGoogleSpreadsheetUrl()

            val infoLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16))
            }

            val ownerText = TextView(this).apply {
                text = "👤 Store Owner: $name\n📞 WhatsApp: $phone\n✉️ Email: $currentEmail\n🆔 User ID: $userId"
                textSize = 13f
                setTextColor(Color.WHITE)
                setLineSpacing(10f, 1f)
                setPadding(0, 0, 0, UiKit.dp(this@MainActivity, 14))
            }
            infoLayout.addView(ownerText)

            val openSiteBtn = UiKit.createPrimaryButton(this, "🌐 Open Live Website ↗", "#2563EB", "#1D4ED8") {
                if (webUrl.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                    } catch (e: Exception) {}
                }
            }
            infoLayout.addView(openSiteBtn)

            val openSheetBtn = UiKit.createSecondaryButton(this, "📊 Open Google Sheet ↗", "#38BDF8") {
                if (sheetUrl.isNotEmpty()) {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sheetUrl)))
                    } catch (e: Exception) {}
                }
            }
            openSheetBtn.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = UiKit.dp(this@MainActivity, 10)
            }
            infoLayout.addView(openSheetBtn)

            AlertDialog.Builder(this)
                .setTitle("⚡ Store Account Hub")
                .setView(infoLayout)
                .setPositiveButton("Close", null)
                .setNegativeButton("🚪 Log Out") { _, _ ->
                    clearAuthSession()
                    authBtn.text = "👤 Login"
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    contentContainer.removeAllViews()
                    contentContainer.addView(createRestrictedLockScreen())
                    updateNavSelection()
                }
                .show()
        } else {
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16), UiKit.dp(this@MainActivity, 20), UiKit.dp(this@MainActivity, 16))
            }

            val emailInput = UiKit.createStyledInput(this, "WhatsApp Mobile No. ya Email (e.g. 9876543210)")
            val passInput = UiKit.createStyledInput(this, "Password").apply {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            dialogLayout.addView(UiKit.createFieldLabel(this, "Mobile / Email:"))
            dialogLayout.addView(emailInput)
            dialogLayout.addView(UiKit.createFieldLabel(this, "Password:"))
            dialogLayout.addView(passInput)

            AlertDialog.Builder(this)
                .setTitle("🔐 Mobile Number Login")
                .setView(dialogLayout)
                .setPositiveButton("Verify & Unlock") { _, _ ->
                    val emailOrPhone = emailInput.text.toString().trim()
                    val pass = passInput.text.toString().trim()
                    if (emailOrPhone.isNotEmpty()) {
                        performMasterLogin(emailOrPhone, pass, authBtn)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun performMasterLogin(emailOrPhone: String, pass: String, authBtn: Button) {
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
                                unlockAppAndLoadDashboard()
                            } else {
                                Toast.makeText(this@MainActivity, "Login Success! Sheet Pending.", Toast.LENGTH_LONG).show()
                                contentContainer.removeAllViews()
                                contentContainer.addView(createRestrictedLockScreen())
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Server verification failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
