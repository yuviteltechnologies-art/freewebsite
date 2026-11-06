# SheetStore Pro - Android Admin App (Android Studio Project)

Yeh dedicated **Android Studio Native App** aapke pure e-commerce system ko aapke smartphone se control karne ke liye design kiya gaya hai.

---

## 🌟 App Ke Main Features

### 1. 🛒 Live Orders & Revenue Dashboard
- **Real-Time Revenue Stats:** Today's Sales (₹) aur Total Orders (Paid vs Pending).
- **Incoming Orders List:** Har customer ka Naam, Mobile, Amount, Product aur 12-digit UTR number.
- **1-Tap Manual Approval:** Agar kisi order ka SMS nahi aaya, toh app se bas **"Approve Order [✓]"** button dabate hi customer ki website screen live unlock ho jati hai!
- **Pull-to-Refresh:** Screen niche kheench kar instant data refresh karein.

### 2. 🌐 Website Customizer (Live Changes)
App me baith kar hi aap website ki details live update kar sakte hain:
- **Website Ka Naam (Brand Name):** e.g. SheetStore Pro ya VastuGeo
- **Brand Tagline:** e.g. Zero Hosting & Domain Store
- **WhatsApp Order Number:** 919876543210
- **UPI ID:** payments@okhdfcbank
- **Top Announcement Bar:** e.g. Special 80% OFF Today!
- Button dabate hi bina coding ke **website par live text change** ho jata hai!

### 3. 🏷️ Products & Price Manager
- Store ke sabhi digital products ki list.
- **Direct Price Editing:** Selling Price (Offer Price) aur MRP directly app me edit karein aur "Update Price" dabayein.
- **Active / Inactive Toggle:** Kisi product ko temporary chhupana ya show karna.

### 4. 🤖 Background Bank SMS & UPI Listener
- **Background SMS Receiver (`SmsReceiver.kt`):** HDFC, SBI, ICICI, AXIS, PNB bank credit SMS aate hi 12-digit UTR auto-capture karke verify karta hai.
- **UPI Push Notification Listener (`UpiNotificationListener.kt`):** PhonePe, Google Pay, Paytm, BHIM notifications aate hi payment background me auto-verify ho jata hai!

---

## 🚀 Android Studio Me Kaise Open Aur Run Karein?

1. **Android Studio Open Karein:**
   - Click karein: **File** ➔ **Open**.
   - Navigate karein aur select karein folder:
     `G:\Vastu_website\android-admin-app`
2. **Gradle Sync:**
   - Android Studio project load karega aur Retrofit/Coroutines libraries download karega (Wait 1 minute).
3. **App Run Karein:**
   - Apna Android Phone USB cable se connect karein (USB Debugging ON karein) YA Android Emulator run karein.
   - Upar green **Play (Run ▶)** button dabayein!
4. **First Time Setup (30 Seconds):**
   - App khulte hi SMS permissions allow karein.
   - Upar **"⚙️ Sheet URL"** button dabakar apna Google Sheet Web App URL paste kar dein.
   - Done! Aapka app live connect ho jayega!

---

## 📁 Project Architecture & File Structure

```text
android-admin-app/
├── app/
│   ├── build.gradle.kts          # Dependencies: Retrofit, Coroutines, Material 3
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions: INTERNET, RECEIVE_SMS, NOTIFICATIONS
│       ├── java/com/sheetstore/admin/
│       │   ├── models/           # Data classes: Order, Stats, Product, Settings
│       │   ├── network/          # ApiClient & ApiService (Retrofit REST Client)
│       │   ├── parser/           # UpiParser.kt (12-digit UTR Regex Engine)
│       │   ├── services/         # Background SMS & UPI Notification Listeners
│       │   └── ui/               # MainActivity, OrdersAdapter, ProductsAdapter
│       └── res/
│           └── values/           # Colors, Strings, Themes
├── build.gradle.kts              # Top-level Gradle config
└── settings.gradle.kts           # Module inclusions
```

