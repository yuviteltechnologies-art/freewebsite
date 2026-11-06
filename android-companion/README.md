# SheetStore Pro - Android UPI Auto-Verifier Companion App

Yeh Android app aapke phone me aane wale **Bank SMS** aur **UPI Notifications (PhonePe, GPay, Paytm, BHIM, HDFC, SBI, ICICI, etc.)** ko automatically read karti hai, unme se **12-digit UTR** aur **Amount** extract karti hai, aur aapke **Google Sheet Web App URL** par instant bhej deti hai.

Isse website par customer ka order **sirf 5-10 second me 100% automatically verify** ho jata hai — bina aapke phone touch kiye!

---

## ⚡ 2 Tarike Hain Setup Karne Ke:

### 🟢 Tarika 1: 1-Minute No-Code Setup via MacroDroid (Sabse Aasan & Free)
Aapko koi coding ya Android Studio ki zaroorat nahi hai. Google Play Store se **MacroDroid** (Free) install karein:

1. Play Store se **MacroDroid - Device Automation** download karein.
2. App kholkar **Add Macro (+)** par click karein.
3. **Triggers (+):**
   - Select karein: **SMS Received** ➔ Any Content (ya filter: sender contains `HDFC`, `SBI`, `AXIS`, `PhonePe`, `Paytm`, `UPI`).
   - *Optional:* Notification Trigger ➔ Notification Received from GPay/PhonePe.
4. **Actions (+):**
   - Select karein: **Applications** ➔ **Open Website / HTTP GET/POST**
   - Method: `POST` (ya `GET`)
   - URL: `Aapka_Google_Sheet_Web_App_URL?action=bankNotification`
   - Content Body / Param:
     ```json
     {
       "action": "bankNotification",
       "raw_text": "[sms_message]"
     }
     ```
5. Macro ko naam dein **"UPI Auto Verifier"** aur **Save** kar lein!

Ab jaise hi koi aapko UPI payment karega aur bank ka SMS aayega, MacroDroid bina aapke phone touch kiye 1 second me Google Sheet ko alert bhej dega, aur website turant unlock ho jayegi!

---

### 💻 Tarika 2: Native Android Studio App (Custom Build)

Agar aap apni khud ki dedicated Android APK build karna chahte hain, toh is folder me pura native Kotlin source code diya gaya hai:

- [`app/src/main/AndroidManifest.xml`](file:///g:/Vastu_website/android-companion/app/src/main/AndroidManifest.xml) — Permissions (`RECEIVE_SMS`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `INTERNET`)
- [`app/src/main/java/com/sheetstore/verifier/MainActivity.kt`](file:///g:/Vastu_website/android-companion/app/src/main/java/com/sheetstore/verifier/MainActivity.kt) — App Dashboard UI (Sheet URL input, Test button, Live Log)
- [`app/src/main/java/com/sheetstore/verifier/SmsReceiver.kt`](file:///g:/Vastu_website/android-companion/app/src/main/java/com/sheetstore/verifier/SmsReceiver.kt) — Background Bank SMS listener
- [`app/src/main/java/com/sheetstore/verifier/UpiNotificationListener.kt`](file:///g:/Vastu_website/android-companion/app/src/main/java/com/sheetstore/verifier/UpiNotificationListener.kt) — PhonePe/GPay/Paytm Push Notification listener
- [`app/src/main/java/com/sheetstore/verifier/UpiParser.kt`](file:///g:/Vastu_website/android-companion/app/src/main/java/com/sheetstore/verifier/UpiParser.kt) — Regex engine jo 12-digit UTR aur ₹ Amount extract karta hai

---

## 🧪 Browser Simulator me Test Karein
Aap bina Android phone ke bhi test kar sakte hain!
Browser me [`test_simulator.html`](file:///g:/Vastu_website/android-companion/test_simulator.html) open karein aur koi bhi sample bank SMS daalkar dekhein ki Google Sheet me kaise auto-verify hota hai!

