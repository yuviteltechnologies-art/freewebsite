# SheetStore Pro - Setup & Complete 3-Level Verification Guide

Aapki website ab **3 Powerful Payment Verification Methods** ke sath 100% ready hai:
- **Level 1 (Basic):** Direct WhatsApp Proof (Zero setup)
- **Level 2 (Smart):** 1-Tap Google Sheet Mobile Checkbox (Pre-installed Google Sheets app)
- **Level 3 (Pro):** **Android Auto-Verifier Companion App (100% Zero-Touch Bank SMS & Notification Reader)**

---

## 🚀 Live Website Links
- **Main Sales Page:** 👉 [freewebsite](https://yuviteltechnologies-art.github.io/freewebsite/)
- **Live Demo Store:** 👉 [demo.html](https://yuviteltechnologies-art.github.io/freewebsite/demo.html)
- **Launchpad / Success:** 👉 [success.html](https://yuviteltechnologies-art.github.io/freewebsite/success.html)
- **Android Simulator:** 👉 [test_simulator.html](file:///g:/Vastu_website/android-companion/test_simulator.html)

---

## ⚡ 1-Minute Auto Setup (Apps Script)

Aapko koi manual sheet banane ki zaroorat nahi hai:
1. Google Sheet open karein ➔ **Extensions** ➔ **Apps Script**.
2. Hamari [`Code.gs`](file:///g:/Vastu_website/Code.gs) file ka pura code copy-paste karein.
3. Dropdown me function select karein: **`setupAutoTabs`** aur **Run** dabayein!
4. Google Sheet me teeno tabs (**`Settings`**, **`Products`**, aur **`Orders`**) apne aap Checkbox formatting ke sath create ho jayenge!
5. Fir upar blue button **Deploy** ➔ **New deployment** ➔ **Web app** select karein:
   - **Execute as:** `Me`
   - **Who has access:** `Anyone`
6. Click **Deploy** aur **Web app URL** copy kar lein.
7. Website ke footer me **"Sheet Settings"** par click karein aur apna **WhatsApp Number**, **UPI ID** aur **Web App URL** paste karke **Save** daba dein!

---

## 📱 Verification Level 2: 1-Tap Google Sheets Mobile App

1. Customer website par payment karke 12-digit UTR enter karta hai.
2. `Orders` sheet me new row add ho jati hai with status `PENDING` aur column 9 me Checkbox `[  ]`.
3. Bank notification aane par aap phone me **Google Sheets App** kholte hain aur customer row ke aage bas **`[✓]` Checkbox tick** kar dete hain!
4. Website har 3 second me check karti hai aur jaise hi tick hota hai, website par confetti phoot-ti hai aur download unlock ho jata hai!

---

## 🤖 Verification Level 3: Android Auto-Verifier App (100% Zero-Touch)

Agar aap chahte hain ki **aapko Google Sheets app bhi na kholni pade** aur Bank SMS aate hi 5-10 second me bina phone touch kiye automatically order unlock ho jaye:

### Option A: 1-Minute No-Code via MacroDroid (Recommended)
1. Google Play Store se free app **MacroDroid** download karein.
2. App me **Add Macro (+)** par click karein:
   - **Trigger (+):** `SMS Received` ➔ Any Content (ya sender contains `SBI`, `HDFC`, `AXIS`, `PhonePe`, `Paytm`, `UPI`).
   - **Action (+):** `Applications` ➔ `Open Website / HTTP GET`
   - **URL:** `Aapka_Google_Sheet_Web_App_URL?action=bankNotification&raw_text=[sms_message]`
3. Macro Save karein! 
4. Ab jab bhi bank SMS aayega, MacroDroid turant Google Sheet ko UTR aur Amount bhej dega aur customer ka access 100% automatically unlock ho jayega!

### Option B: Dedicated Native Kotlin Android App
- Source code [`android-companion/`](file:///g:/Vastu_website/android-companion/README.md) folder me diya gaya hai.
- Isme `SmsReceiver.kt` aur `UpiNotificationListener.kt` background me silently run hote hain aur GPay, PhonePe, Paytm notifications ko live read karte hain.

---

## 🧪 Browser Simulator Se Test Karein
Aap bina real payment kiye test kar sakte hain:
- Browser me [`android-companion/test_simulator.html`](file:///g:/Vastu_website/android-companion/test_simulator.html) open karein.
- Sample HDFC/SBI bank SMS select karein aur **"Simulate Android Webhook"** dabayein.
- Aapki Google Sheet me order turant **PAID (Green)** mark ho jayega!
