# 🚀 SheetStore Pro: Complete Master Workflow & Step-by-Step Architecture Guide

Yeh guide aapke poore system ka **Master Roadmap** hai. Isme shuru se aakhiri tak har step detail me likha hai taaki aapko baar-baar kuch dhoondhna na pade. Is file me aap aage chalke apne naye steps bhi asaani se add kar sakte hain.

---

## 📌 Quick Reference Details (Ek Nazar Me Sabhi Links)

| Chiz | Detail / Link |
|---|---|
| **Live Website (Sales Landing Page)** | [https://yuviteltechnologies-art.github.io/freewebsite/](https://yuviteltechnologies-art.github.io/freewebsite/) |
| **Live Demo Store (Digital Products)** | [https://yuviteltechnologies-art.github.io/freewebsite/demo.html](https://yuviteltechnologies-art.github.io/freewebsite/demo.html) |
| **Success / Download Unlock Page** | [https://yuviteltechnologies-art.github.io/freewebsite/success.html](https://yuviteltechnologies-art.github.io/freewebsite/success.html) |
| **GitHub Repository** | `https://github.com/yuviteltechnologies-art/freewebsite.git` (Branch: `main`) |
| **Active Google Sheet Web App URL** | `https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec` |
| **Android Admin Studio Project** | `g:\Vastu_website\android-admin-app` |
| **Browser SMS/UPI Test Simulator** | `g:\Vastu_website\android-companion\test_simulator.html` |

---

## 🏗️ Poore System Ka 5-Tier Architecture

```
[ Tier 1: Customer Facing ]
      │
      ▼
Website (GitHub Pages - Free Hosting)
   ├── index.html (Main Sales Page + 5 Themes)
   ├── demo.html (E-Commerce Product Catalog)
   └── success.html (Download Delivery Page)
      │
      │  (JSON API - GET / POST)
      ▼
[ Tier 2: Cloud Backend & Database ]
Google Apps Script (Code.gs) + Google Sheet
   ├── Tab 1: 'Settings' (Theme, Brand Name, UPI, WhatsApp, Badges)
   ├── Tab 2: 'Products' (Pricing, Titles, Active/Inactive)
   └── Tab 3: 'Orders' (Live Orders, UTR Numbers, PAID / PENDING)
      ▲
      │  (Retrofit REST API)
      ▼
[ Tier 3: Mobile Admin Control ]
Android Admin App (Kotlin - Android Studio)
   ├── Tab 1: 🛒 Orders & Live Revenue (1-Tap Approve)
   ├── Tab 2: 🎨 5 Ready-made Website Themes (1-Click Switch)
   ├── Tab 3: 🌐 Website Branding & Settings (Name, Phone, UPI)
   └── Tab 4: 📦 Product Catalog & Price Manager
      ▲
      │  (Background BroadcastReceiver & NotificationListener)
      ▼
[ Tier 4: Zero-Touch Payment Reconciliation ]
Android Background SMS & UPI Listener
   ├── Reads incoming SMS from Bank (SBI, HDFC, ICICI, Axis, etc.)
   ├── Reads Push Notifications from PhonePe, GPay, Paytm Business
   └── Extracts 12-digit UTR & Amount -> Posts directly to Google Sheet
```

---

## 📋 STEP-BY-STEP IMPLEMENTATION & WORKFLOW

---

### Phase 1: Website Layer (Frontend & GitHub Pages)
**Uddeshya:** Bina domain aur hosting ke 100% free lifetime website run karna.

- **Files:**
  - `index.html`: Main landing page jo services/products present karta hai.
  - `demo.html`: Product catalog jisme instant UPI modal aur checkout flow hai.
  - `success.html`: Order complete hone par download links ya courses unlock karne ka page.
- **Kaise Kaam Karta Hai:**
  1. Tailwind CSS aur Google Fonts CDN se load hote hain (koi external build system nahi chahiye).
  2. Website open hote hi Google Sheet ke Web App URL se settings (`brand_name`, `template_id`, `whatsapp_number`, `upi_id`) fetch karti hai.
  3. Agar customer offline ho ya pehli baar visit kare, toh `localStorage` me stored cache se instant render hoti hai bina kisi flicker ke.
- **Deployment:**
  - Code GitHub repository me push hota hai:
    ```bash
    git add .
    git commit -m "update website"
    git push origin main
    ```
  - GitHub Pages automatic 30 second me live site update kar deta hai.

---

### Phase 2: Database & Backend Layer (Google Sheet + Apps Script)
**Uddeshya:** Kisi paid database ya server ke bina Google Sheet ko database aur API server banana.

- **File:** `Code.gs`
- **Setup Steps (One-Time):**
  1. Ek nayi Google Sheet banayein: [sheets.new](https://sheets.new)
  2. Upar menu me **Extensions** ➔ **Apps Script** par jayein.
  3. `Code.gs` ka pura code Apps Script editor me paste karein.
  4. Top bar me function dropdown se **`setupAutoTabs`** select karke **Run** dabayein:
     - Google Sheet me 3 tabs automatically ban jayenge:
       - **`Settings`**: Keys like `brand_name`, `template_id`, `whatsapp_number`, `upi_id`, etc.
       - **`Products`**: 4 sample products (Title, Category, Price Original, Price Selling, Active).
       - **`Orders`**: Columns for Order ID, Date, Name, Phone, Product, Amount, UTR, Status, Quick Verify `[✓]`.
  5. Top right me **Deploy** ➔ **New deployment** ➔ **Web app** select karein:
     - **Execute as:** `Me`
     - **Who has access:** `Anyone`
     - Click **Deploy** aur jo **Web App URL** mile use copy kar lein.

---

### Phase 3: Website Ko Google Sheet Se Connect Karna
**Uddeshya:** Website ka sara data (store name, products, orders) live Google Sheet se connect karna.

1. Website ke code me `storeConfig.sheetUrl` me apna Web App URL set hota hai.
2. Demo store open karke footer me **"Sheet Settings"** par click karein aur apna Web App URL paste karke save karein.
3. Ab website open hone par:
   - Saare products `Products` sheet se dynamic load honge.
   - Logo, Brand Name, UPI ID aur Theme `Settings` sheet se live aayenge.

---

### Phase 4: Mobile Admin App (Android Studio)
**Uddeshya:** Mobile phone se website ko manage karna bina computer khole.

- **Project Location:** `g:\Vastu_website\android-admin-app`
- **Android Studio Me Run Kaise Karein:**
  1. Android Studio kholein ➔ **Open** ➔ `g:\Vastu_website\android-admin-app` folder select karein.
  2. Gradle Sync complete hone dein.
  3. Top bar me **Run (Green Play Button)** dabakar apne phone ya emulator me install karein.
  4. App khulne par top right me **⚙️ Sheet URL** button dabayein aur apna Web App URL paste karein.
- **App Ke 4 Main Sections:**
  1. **🛒 Orders Tab:**
     - Aaj ki total sales aur revenue dikhata hai.
     - Live incoming orders aate hain.
     - Har order ke aage **"Approve [✓]"** button hota hai.
  2. **🎨 Themes Tab (1-Click Switcher):**
     - 5 ready-made themes ke cards (Cyber Blue, Vedic Gold, Minimal Light, Neon Violet, Emerald Pro).
     - Kisi bhi template par **"Apply This Template ⚡"** dabate hi website ka look poori duniya me turant change ho jata hai!
  3. **🌐 Settings Tab:**
     - Brand Name, Tagline, WhatsApp Number, UPI ID aur Announcement Text live update karein.
  4. **📦 Products Tab:**
     - Har product ka Selling Price aur Original Price badlein.
     - Active / Inactive switch karein.

---

### Phase 5: Payment & Verification Ka Pura Flow (Zero-Touch)
**Uddeshya:** Customer payment kare aur bina kisi human delay ke order instant verify ho jaye.

```
Customer Website Par Aata Hai
          │
          ▼
Product Select Karta Hai (₹499)
          │
          ▼
Checkout Modal Me Name & WhatsApp Number Dikhata Hai
          │
          ▼
UPI QR Code Scan Karke Apne GPay / PhonePe / Paytm Se Pay Karta Hai
          │
          ▼
Payment Ke Baad 12-Digit UTR / UPI Ref Number Website Par Enter Karta Hai
          │
          ▼
Website Turant Google Sheet Me Order Entry Bhejti Hai (Status = PENDING)
          │
          ├──────────────────────────────────────────────────────┐
          │                                                      │
          ▼ (Method 1: Automatic Android Verifier)               ▼ (Method 2: Manual)
Bank Ka SMS Ya PhonePe/GPay Notification Aata Hai         Aap Android Admin App Me
Android App Background Me UTR & Amount Read Karti Hai      "Approve [✓]" Button Dabate Hain
Aur Google Sheet Ko Webhook Post Karti Hai                 Ya Google Sheet Me Checkbox Tick Karte Hain
          │                                                      │
          ├──────────────────────────────────────────────────────┘
          │
          ▼
Google Sheet Me Order Status ho jata hai: PAID (Green)
          │
          ▼
Website Har 3 Second Me Poll Kar Rahi Hai
Jaise Hi Status "PAID" Milta Hai:
🎉 Screen Par Confetti Phoot-ti Hai Aur Instant success.html Par Redirect Hoti Hai!
```

---

## 🎨 5 Ready-Made Templates Ki Details

| Template ID | Name & Style | Best For | Key Visuals |
|---|---|---|---|
| `modern-blue` | **⚡ Modern Cyber Blue** | Software, SaaS, Tech, Digital Goods | Dark Slate (`#080C1A`), Electric Blue (`#2563EB`), Cyan Glow |
| `vedic-gold` | **🕉️ Royal Vedic Gold** | Vastu, Astrology, Kundli, Mandir & Spiritual Luxury | Deep Plum Navy (`#090510`), Radiant Gold (`#F59E0B`), Cinzel Serif Heading |
| `minimal-light` | **🛍️ Clean Minimal Light** | D2C Clean Store, Physical Retail, Fashion | Crisp White (`#FFFFFF`), Charcoal Slate (`#0F172A`), Emerald Green (`#059669`) |
| `neon-violet` | **👾 Cyberpunk Neon Violet** | AI Prompts, Notion Kits, Video LUTs, Gaming | Pitch Black (`#030107`), Electric Violet (`#8B5CF6`), Neon Pink (`#EC4899`) |
| `emerald-pro` | **🌿 Emerald Business Pro** | Financial Consulting, eBooks, High-Ticket B2B | Dark Pine Slate (`#03140F`), Vibrant Mint (`#10B981`), Corporate Trust Badges |

---

## 🔌 API Actions Quick Cheatsheet (Code.gs Reference)

Aap kisi bhi client (App, Postman, curl, website) se in endpoints ko call kar sakte hain:

### 1. Dashboard Data Lena (Orders + Settings + Products)
- **URL:** `WebAppURL?action=getAdminDashboard`
- **Method:** `GET` or `POST`
- **Output:** `{ success: true, stats: {...}, orders: [...], settings: {...}, products: [...] }`

### 2. Website Settings Update Karna (Theme, Name, UPI, etc.)
- **URL:** `WebAppURL`
- **Method:** `POST`
- **JSON Body:**
  ```json
  {
    "action": "updateSettings",
    "template_id": "vedic-gold",
    "brand_name": "My Store",
    "whatsapp_number": "919999999999",
    "upi_id": "merchant@upi"
  }
  ```

### 3. Customer Order Create Karna
- **URL:** `WebAppURL?action=createOrder&order_id=#ORD-1234&name=Rahul&phone=9876543210&product=Kit&amount=499&utr=412984920194`
- **Method:** `GET` or `POST`

### 4. Order Status Check Karna (Website 3-Second Polling)
- **URL:** `WebAppURL?action=checkOrderStatus&order_id=#ORD-1234&utr=412984920194`
- **Method:** `GET`
- **Output:** `{ success: true, status: "PAID" }` ya `{ status: "PENDING" }`

### 5. Bank SMS / Push Notification Se Auto-Verification
- **URL:** `WebAppURL`
- **Method:** `POST`
- **JSON Body:**
  ```json
  {
    "action": "bankNotification",
    "raw_text": "Credited with Rs. 499.00 on 06-Nov-26 by UPI Ref 412984920194"
  }
  ```

### 6. Order Ko Direct Approve Karna (Admin App Button)
- **URL:** `WebAppURL?action=approveOrder&order_id=#ORD-1234`
- **Method:** `GET` or `POST`

---

## 🔮 Future Expansion (Aage Naye Features Kaise Add Karein)

Aap is template me aage naye steps aur features asaani se add kar sakte hain. Niche kuch planned extensions hain jise aap checklist ki tarah use kar sakte hain:

- [ ] **Feature 1: Naya 6th Template Add Karna**
  - `index.html` aur `demo.html` me `[data-theme="new-theme-id"]` ke CSS variables define karein.
  - `MainActivity.kt` me `availableTemplates` list me 1 naya `WebTheme` entry add karein.
- [ ] **Feature 2: Dynamic Banner Image Changer**
  - Settings sheet me `banner_url` key add karein.
  - Android App me image URL input box lagayein jo live banner badal de.
- [ ] **Feature 3: Custom Domain Connect Karna**
  - GitHub Pages settings me jaakar apna custom `.com` ya `.in` domain point karein (CNAME record).
- [ ] **Feature 4: Automatic Invoice / PDF Generation**
  - Apps Script me `handleMarkPaid` ke andar Google Docs template se automated PDF receipt banakar customer ke WhatsApp / Email par trigger karna.
- [ ] **Feature 5: WhatsApp Cloud API Integration**
  - Order verify hote hi WhatsApp Business official API se download link automatic message bhejna.

---

> **Note:** Is file ko aap project me hamesha reference ke liye use kar sakte hain:  
> 📁 File Path: `g:\Vastu_website\MASTER_WORKFLOW_GUIDE.md`
