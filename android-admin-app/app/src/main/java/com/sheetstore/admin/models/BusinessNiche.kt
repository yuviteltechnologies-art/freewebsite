package com.sheetstore.admin.models

/**
 * Multi-purpose Business Niches Supported by SheetStore Pro
 */
data class BusinessNiche(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val recommendedThemeId: String,
    val recommendedThemeName: String,
    val features: List<String>,
    val defaultHeroTitle: String,
    val defaultHeroSubtitle: String,
    val defaultTagline: String,
    val defaultBadge: String
)

object NicheCatalog {
    val allNiches = listOf(
        BusinessNiche(
            id = "ecommerce",
            emoji = "🛍️",
            title = "Online Selling & E-Commerce",
            subtitle = "Physical products, clothing, retail store, electronics & local shop",
            recommendedThemeId = "minimal-light",
            recommendedThemeName = "Clean Minimal Light",
            features = listOf("Product Catalog", "Cart & Checkout", "Cash on Delivery / UPI", "Fast Delivery Badges"),
            defaultHeroTitle = "Apni Dukaan Online Shuru Karein — Best Deals Everyday",
            defaultHeroSubtitle = "High quality verified products direct aapke darwaje par. Order via WhatsApp ya UPI.",
            defaultTagline = "100% Quality & Verified Local Store",
            defaultBadge = "⚡ FAST DELIVERY & CASH ON DELIVERY"
        ),
        BusinessNiche(
            id = "digital",
            emoji = "⚡",
            title = "Digital Products & SaaS",
            subtitle = "E-books, software, cheat sheets, Notion templates & code kits",
            recommendedThemeId = "modern-blue",
            recommendedThemeName = "Modern Cyber Blue",
            features = listOf("Instant File Download", "Zero Shipping Cost", "Direct UPI QR (0% Fee)", "License Keys"),
            defaultHeroTitle = "Shopify aur WordPress ka Monthly Kharcha Band Karein.",
            defaultHeroSubtitle = "Google Sheet ko apna backend banayein aur digital products instant deliver karein.",
            defaultTagline = "Zero Hosting & Domain Digital Store",
            defaultBadge = "⚡ 100% DIGITAL • INSTANT ACCESS"
        ),
        BusinessNiche(
            id = "vastu",
            emoji = "🕉️",
            title = "वास्तु ब्रह्मांड (Vastu & Spiritual)",
            subtitle = "9x9 Mandala Grid, 10-Min Home Audit Score, Vastu Shastra & Remedies",
            recommendedThemeId = "vastu_brahmand",
            recommendedThemeName = "वास्तु ब्रह्मांड (Vastu Brahmand)",
            features = listOf("9x9 Purusha Mandala", "10-Min Home Audit Score", "Sunlight & Airflow Science", "WhatsApp Consultations"),
            defaultHeroTitle = "अपने घर का वास्तु, अब आप खुद कीजिए",
            defaultHeroSubtitle = "कंपास, धूप और हवा जैसी नापी जा सकने वाली चीज़ों से अपने घर की दिशाएँ जाँचिए। 9x9 वास्तु पुरुष मंडल व सेल्फ-ऑडिट टूल।",
            defaultTagline = "वैज्ञानिक व प्रामाणिक वैदिक वास्तु शास्त्र",
            defaultBadge = "🕉️ प्रामाणिक वास्तु शास्त्र • खुद करें, खुद समझें"
        ),
        BusinessNiche(
            id = "linktree",
            emoji = "🌲",
            title = "Linktree & Digital Visiting Card",
            subtitle = "Bio link, WhatsApp card, social profiles, payment QR & mini store",
            recommendedThemeId = "neon-violet",
            recommendedThemeName = "Cyberpunk Neon Violet",
            features = listOf("All-in-one Bio Links", "1-Tap WhatsApp Contact", "Direct UPI QR Payment", "Save Contact (vCard)"),
            defaultHeroTitle = "Connect With Me — All Links & Products in One Place",
            defaultHeroSubtitle = "Follow my social handles, browse top products, and pay directly via UPI.",
            defaultTagline = "Official Bio & Digital Business Card",
            defaultBadge = "🌲 VERIFIED DIGITAL BIO CARD"
        ),
        BusinessNiche(
            id = "portfolio",
            emoji = "💼",
            title = "Personal Website & Agency Portfolio",
            subtitle = "Freelancers, developers, designers, consultants & client bookings",
            recommendedThemeId = "emerald-pro",
            recommendedThemeName = "Emerald Business Pro",
            features = listOf("Service Packages", "Case Study Showcase", "Client WhatsApp Inquiries", "High-Trust B2B Branding"),
            defaultHeroTitle = "High-Performance Digital Solutions & Web Design",
            defaultHeroSubtitle = "We build fast, scalable, and high-converting websites for ambitious businesses.",
            defaultTagline = "Design, Development & Growth Studio",
            defaultBadge = "🌿 TOP RATED AGENCY & FREELANCER"
        ),
        BusinessNiche(
            id = "saree",
            emoji = "🥻",
            title = "NOOLAM Luxury Ethnic & Saree Studio",
            subtitle = "Kanchipuram silk sarees, bridal lehengas, handlooms & boutique couture",
            recommendedThemeId = "noolam-saree",
            recommendedThemeName = "NOOLAM Kanchipuram Silk",
            features = listOf("6 Moods Architecture", "Try the Look Bundle Builder", "Interactive Clothes Rail", "3D Tilt Silk Arc"),
            defaultHeroTitle = "Woven for the Divine — Pure Kanchipuram Silk Sarees",
            defaultHeroSubtitle = "Centuries of temple artisan heritage, pure Korvai silk, and authentic 24K gold zari.",
            defaultTagline = "Royal South Indian Silk Heirlooms",
            defaultBadge = "🥻 PURE KANCHIPURAM SILK MARK CERTIFIED"
        )
    )

    fun getNicheById(id: String): BusinessNiche {
        return allNiches.find { it.id.equals(id, ignoreCase = true) } ?: allNiches[1] // Default to Digital
    }
}

