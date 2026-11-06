# 🛒 Zero-Knowledge Guide: Apni Website, Google Sheet Aur Mobile App Chalane Ka Asaan Tareeka

> **Namaste!** Agar aapko coding ya technical baaton ka bilkul bhi idea nahi hai (0% knowledge), toh bhi koi chinta nahi!  
> Yeh guide bilkul aam bolchaal ki **Hinglish** me likhi gayi hai. Bas jaise-jaise yahan likha hai, waisa-waisa click karte jayein.

---

## 💡 Sabse Pehle Samajhte Hain: Yeh Pura System Hai Kya?

Aap is poore system ko ek aam dukaan ki tarah samjhein:

1. **Website ([index.html](file:///g:/Vastu_website/index.html)):**  
   Yeh aapki **Dukaan ka Counter** hai jo internet par sabhi logon ko dikhta hai. Customer yahan aakar product dekhta hai aur khareedta hai. Iska koi monthly kharcha nahi hai (0% hosting, 0% domain fee).

2. **Dynamic QR Code Payment System:**  
   Yeh aapka **Cash Counter** hai. Customer jaise hi buy karta hai, uske samne uske order ka dynamic QR code aata hai jisme exact amount (abhi live test ke liye ₹10) aur aapki UPI ID pre-filled hoti hai. Mobile par 1-tap me Google Pay, PhonePe ya Paytm khul jata hai.

3. **Google Sheet ([Code.gs](file:///g:/Vastu_website/Code.gs)):**  
   Yeh aapka **Hisaab-Kitaab ka Register** hai. Jab bhi koi customer order karega ya paise dega, uska naam, mobile number aur paise is sheet me automatic likh jayenge.

4. **Android Mobile App ([MainActivity.kt](file:///g:/Vastu_website/android-admin-app/app/src/main/java/com/sheetstore/admin/ui/MainActivity.kt)):**  
   Yeh aapke haath me **Dukaan ka Remote Control** hai. Is app se aap apne bistar par baith kar 1-click me website ka design, product ke daam (₹10 se ₹499), naam, phone number aur font badal sakte hain.

5. **Auto-Payment Verifier:**  
   Yeh aapka **Digital Munim** hai. Jaise hi customer UPI se paise bhejega aur bank ka SMS aayega, yeh system apne aap check karke customer ko download link de dega. Aapko haath se kuch nahi karna!

---

## 📌 Aapki Zaroori Links (Inhe Kahin Dhoondhna Nahi Padega)

In links ko apne paas bookmark karke rakh lein:

* **Aapki Live Website:** 👉 [https://yuviteltechnologies-art.github.io/freewebsite/](https://yuviteltechnologies-art.github.io/freewebsite/)
* **Aapka Live Demo Store:** 👉 [https://yuviteltechnologies-art.github.io/freewebsite/demo.html](https://yuviteltechnologies-art.github.io/freewebsite/demo.html)
* **Aapka Google Sheet Web App Link:**  
  `https://script.google.com/macros/s/AKfycby78HzLm4c9qH9Yq5WG9-qtqX0WN3huUsLMZwAHATZysy59KWwU5YYDxf3pGMcGbV98/exec`
* **Mobile App Ka Folder:** `g:\Vastu_website\android-admin-app`

---

## 🧭 5 Aasan Steps Me Pura System Samajhein

---

### Step 1: Website Ka Dynamic Setup & Design (App Se 1-Click Me Look Badlo)

Aapki website internet par bilkul free chal rahi hai (GitHub Pages par).
* Jab bhi customer website kholega, usko sundar modern design, dynamic rate aur products dikhenge.
* **Website Ka Theme/Design Kahan Se Badlega?**  
  👉 **Aapke Mobile App Se!**  
  Android App kholkar jab aap **"🎨 Themes"** tab me kisi bhi template par **"Apply This Template ⚡"** dabate hain, toh poori duniya me live website ka theme, colors aur vibe 1-second me badal jata hai!
* **App Me Yeh 5 Ready-made Designs Hain:**
  1. ⚡ **Modern Blue:** Tech aur digital goods ke liye (Sleek slate + electric blue).
  2. 🕉️ **Royal Vedic Gold:** Vastu Shastra, Jyotish, Kundli aur puja ke samaan ke liye (Royal maroon + glowing gold).
  3. 🛍️ **Clean Minimal Light:** Safed saaf-suthra store, kapde ya physical samaan ke liye (Crisp white + emerald).
  4. 👾 **Neon Violet:** Creators, AI tools aur gaming ke liye (Pitch black + neon pink/violet).
  5. 🌿 **Emerald Pro:** Business, finance aur agency ke liye (Corporate pine + mint green).
* *(Note: Website par bottom-right me jo chota button laga hai, wo sirf testing/preview ke liye hai, par permanent control aapke mobile app me hi hai).*

---

### Step 2: QR Code Payment Module & Live ₹10 Real Payment Testing (Practical Guide)

Aapne poocha tha ki **QR code se payment kaise process hoga, iska pricing app se kaise badlega, aur ₹10 ka real test kaise karein?**  
Yeh poora module live implement ho chuka hai! Aaiye samajhte hain:

#### 1. Dynamic Pricing Control (Application Se Daam Badle):
* **Live Test Ke Liye ₹10 Set Hai:** Website par aur QR code me daam abhi **₹10** set kar diya gaya hai taaki aap bina kisi bade risk ke khud 1-2 real transactions test kar sakein.
* **App Se Kabhi Bhi Badlein:**  
  Jab test poora ho jaye, aap Android App ke **🌐 Settings (Tab 3)** me jayein ➔ **"Main Website Product Price in ₹"** me jaakar `499` ya `999` likhein aur **"Update Website Live 🚀"** daba dein. Turant poori website aur QR code naye daam par update ho jayenge!

#### 2. QR Code Kaise Kaam Karta Hai?
* Jab customer website par click karta hai, system uske liye ek unique Order ID (jaise `ORD-98214`) banata hai.
* Us Order ID aur exact amount (₹10) ka **Dynamic UPI QR Code** screen par generate hota hai.
* Jab customer PhonePe, Google Pay ya Paytm se scan karta hai, toh **₹10 aur aapki UPI ID apne aap bhari hui aati hai** — customer ko haath se amount daalne ki zaroorat nahi padti!
* Mobile phone par customer ke liye 1-tap direct buttons bhi hain: `🟢 GPay`, `🟣 PhonePe`, `🔵 Paytm`.

#### 3. Real Live ₹10 Payment Test Karne Ka Step-by-Step Practical Tareeka:

Aap abhi apne mobile ya computer par ye 5 minute ka real test karke dekhein:

1. **Website Kholein:**  
   Apne browser me yeh link kholein:  
   👉 [https://yuviteltechnologies-art.github.io/freewebsite/](https://yuviteltechnologies-art.github.io/freewebsite/)
2. **Buy Button Dabayein:**  
   Landing page par **"Instant Access Paayein - ₹10 (Pay via UPI / QR)"** par click karein.
3. **Apni Details Daalein:**  
   Apna Naam aur WhatsApp Mobile Number likhein aur **"Proceed to Payment"** par click karein.
4. **QR Code Scan Karein (₹10 Pay Karein):**  
   - Screen par high-quality dynamic QR code aayega jiske upar likha hoga: **"Scan Karein • Amount: ₹10"**.
   - Apne PhonePe, Google Pay ya Paytm se scan karein (ya mobile par hain toh GPay/PhonePe button dabayein).
   - Aap dekhenge ki exact ₹10 aur UPI ID pre-filled aayi hai. Payment complete karein!
5. **12-Digit UTR Number Copy Karein:**  
   - Payment safal hone ke baad UPI app me "Transaction Details" me 12-digit ka **UTR / UPI Ref ID** (jaise: `412891029412`) dikhega.
   - Us number ko copy karein aur website par **"Enter 12-digit UPI Ref / UTR No"** wale box me paste karke **"Verify Payment"** dabayein.
6. **Automatic / Manual Verification:**  
   - Website par likha aayega: *"Verifying your payment with bank..."*
   - Ab aap apne phone me **Android Admin App** kholein ➔ **🛒 Orders** tab me aapko ye naya order dikhega.
   - Bas order ke aage **"Approve [✓]"** dabayein (ya agar SMS listener active hai toh bank ka SMS aate hi wo auto-approve ho jata hai).
7. **Instant Delivery / Jadu:**  
   - Jaise hi approve hua, 2 second ke andar website par celebration confetti phootegi aur customer ka **Success Page (`success.html`)** khul jayega!
   - Google Sheet ke `Orders` tab me transaction status `PAID` ho jayega.

---

### Step 3: Google Sheet Setup (Backend Hisaab-Kitaab Register)

Google Sheet aapka register hai. Agar aapko kabhi naye sire se nayi sheet banani ho toh bas ye 4 baatein karein:

1. Browser me **[sheets.new](https://sheets.new)** kholein (Ek nayi blank Google Sheet khul jayegi).
2. Upar menu me **Extensions** par click karein aur **Apps Script** chunein.
3. Hamare project ki file [`Code.gs`](file:///g:/Vastu_website/Code.gs) ko pura copy karke wahan paste kar dein.
4. Upar ek dropdown dikhega jisme `doGet` likha hoga, us dropdown par click karke **`setupAutoTabs`** chunein aur **Run** dabayein.
   * *Jaise hi aap Run dabayenge, Google Sheet me 3 tabs apne aap ban jayenge:*
     - **Settings:** Dukaan ka naam, UPI ID, WhatsApp number, Main Product Price, Theme, Colors, Fonts.
     - **Products:** Saare bikne wale products aur unke daam.
     - **Orders:** Jo bhi customer khareedega uska live hisaab.
5. Upar blue button **Deploy** dabayein ➔ **New deployment** chunein:
   - "Select type" me **Web app** chunein.
   - "Who has access" me **Anyone** chunein.
   - **Deploy** dabayein aur jo **Web app URL** mile, use copy kar lein!

---

### Step 4: Website Ko Google Sheet Se Jodna (1 Minute Ka Kaam)

Ab website aur Google Sheet ko aapas me connect karna hai taaki sheet ka data website par dikhe:

1. Apni website ka Demo Store kholein: [demo.html](https://yuviteltechnologies-art.github.io/freewebsite/demo.html)
2. Sabse niche footer me jayein, wahan ek chota button dikhega: **"⚙️ Sheet Settings"**.
3. Us button par click karein:
   - Apna **WhatsApp Mobile Number** daalein.
   - Apni **UPI ID** daalein (jaise: `yourname@okaxis` ya `paytm`).
   - Apna **Google Sheet Web App URL** paste karein.
4. **Save Settings** button daba dein!
5. *Bas kaam khatam!* Ab website seedha aapki Google Sheet se jud chuki hai.

---

### Step 5: Android Mobile App Se Poori Website Ko Control Karna

Aapko website badalne ke liye computer kholne ki zaroorat nahi hai. Sab kuch aapke mobile app se hoga:

1. **Android Studio** kholein.
2. **Open** dabakar yeh folder chunein: `g:\Vastu_website\android-admin-app`.
3. Upar green color ka **Play (Run)** button dabakar app ko apne phone me install karein.
4. App khulte hi top-right me **⚙️ Sheet URL** par click karke apna Web App URL save kar lein.

#### App Me Yeh 4 Tabs Milte Hain:

* 🛒 **Tab 1: Orders (Hisaab-Kitaab & Approval)**
  - Aaj kitni kamai hui, kitne order aaye.
  - Sabhi orders ki list. Har order ke aage **"Approve [✓]"** button hota hai jisse customer ka product turant unlock ho jata hai.
* 🎨 **Tab 2: Themes (1-Click Me Look Badlo)**
  - Yahan 5 sundar ready-made designs ke card bane hain (Vedic Gold, Modern Blue, etc.).
  - Aap bas kisi bhi design ke niche **"Apply This Template ⚡"** dabayein.
  - 1 second me poori duniya me website ka rang-roop badal jayega!
* 🌐 **Tab 3: Settings (Branding, Pricing, Fonts, Colors & Text Studio)**
  - **Product Price:** Yahan se aap main product price (₹10, ₹499, ₹999) badal sakte hain.
  - Dukaan ka naam, Logo, Fonts, Header/Footer Colors aur website ka sara text yahan se badalta hai.
* 📦 **Tab 4: Products (Rate List)**
  - Kisi bhi specific product ka Selling Price badlein.
  - Agar koi product khatam ho gaya ho, toh use Inactive kar dein taaki customer use na khareed sake.

---

## 🎨 Advance Customization: Font, Logo, Header/Footer Colors & Text App Se Kaise Badlein?

Aapke Android App ke **🌐 Settings (Tab 3)** me ye saare advance controls available hain:

### 1. 🔤 Font (Likhawat Ka Style) 1-Tap Me Chunein
- **Aapko font ka naam type nahi karna padega:** App me 4 ready-made clickable buttons diye gaye hain:
  - `[ 🕉️ Vedic (Cinzel) ]` ➔ Royal traditional serif font (Vastu, Astrology aur luxury ke liye best).
  - `[ ⚡ Modern (Outfit) ]` ➔ Modern clean tech font (Default).
  - `[ 👾 Creative (Space) ]` ➔ Bold futuristic font (Gaming aur digital goods ke liye).
  - `[ 🛍️ Clean (Jakarta) ]` ➔ Ultra-clean corporate & minimal font.
- Bas jo style pasand ho uspe tap karein, website ki saari badi headlines turant us font me badal jayengi!

### 2. 🖼️ Custom Logo Aur Banner Photo Badalna (Phone Gallery Se)
- **Kaise Karein:**
  - App me ab **"🖼️ Phone Se Logo Photo Chunein"** aur **"🖼️ Phone Se Banner Photo Chunein"** ka direct button hai!
  - Button dabate hi aapke mobile ki gallery khul jayegi. Jo photo pasand ho use select karein.
  - App apne aap us photo ko sahi size me compress karke website par laga dega.
  - Ya fir agar aapke paas koi online image link hai (jaise Google Drive ya Imgur ka), toh aap direct link bhi paste kar sakte hain.

---

### 📐 Har Photo Ka Sahi Size (Ready-Made Size Cheatsheet)

Website fast chale aur photo kabhi fati ya stretch hui na dikhe, iske liye har jagah ka exact size note kar lein:

| Photo Kahan Lagegi | Sahi Size (Pixels) | Shape / Ratio | Kaisi File Best Hai? |
|---|---|---|---|
| 🏢 **Brand Logo** | **`512 x 512 px`** | Square (1:1) | PNG (Transparent Background) ya WebP (<100 KB) |
| 🖼️ **Hero / Showcase Banner** | **`1200 x 600 px`** | Landscape (2:1) | WebP ya JPG (<300 KB) |
| 📦 **Product Thumbnail (Samaan)** | **`600 x 600 px`** | Square (1:1) | WebP ya JPG (<150 KB) |
| 🛡️ **Trust / Offer Badges** | **`128 x 128 px`** | Square (1:1) | PNG Icon (<50 KB) |

> 💡 **Tip:** Canva (Free App) me bas **"Custom Size"** par click karein, upar table me se size daalein, apna design banayein aur save kar lein!

---

### 3. 🎨 Header & Footer Ka Color Badalna (1-Tap Color Selection)
- **Aapko koi bhi `#` ya color code type karne ki bilkul zaroorat nahi hai:**  
  App me ready-made **Color Selection Buttons** de diye gaye hain:
  - `[ 🖤 Dark Slate ]` ➔ Deep modern slate color
  - `[ 💜 Royal Vedic ]` ➔ Majestic royal plum/maroon
  - `[ 💙 Tech Navy ]` ➔ Deep indigo navy
  - `[ 💚 Forest Pine ]` ➔ Deep rich green
  - `[ 🖤 Pure Black ]` ➔ Classic pitch black
  - `[ 🤍 Clean Slate ]` ➔ Fresh light white/slate
- **Header ke liye:** Jo color chahiye us button par tap karein.
- **Footer ke liye:** Jo color chahiye us button par tap karein.
- Bas buttons par tap karke "Update Website Live 🚀" daba dein, website ka Header aur Footer naye rang me chamakne lagega!

### 4. ✍️ Website Ka Har Ek Text (Pura Content) Badalna
Aapko kisi coder ke paas jane ki zaroorat nahi hai, website ka ek-ek word aapke mobile app se badal sakta hai:
- **Hero Main Headline:** Jo sabse badi headline screen par aati hai (e.g. *"Vastu Shastra Se Badlein Apni Zindagi"* ya *"Apna E-Commerce Store Shuru Karein"*).
- **Hero Subtitle:** Headline ke niche aane wala 2-line ka vivaran (description).
- **CTA Button Text:** Main button par kya likha ho (e.g. *"Get Store Now"*, *"Abhi Khareedein ₹10"*, ya *"Consultation Book Karein"*).
- **Top Announcement Bar:** Sabse upar chalne wala discount offer text (e.g. *"Diwali Special Offer: 80% OFF Today!"*).
- **Brand Name & Tagline:** Dukaan ka naam aur slogan.

👉 Jaise hi aap App me **"Update Website Live 🚀"** dabayenge:
1. Mobile app ye sara data Google Sheet ki `Settings` tab me bhej deti hai.
2. Website turant Google Sheet se ye naya data fetch karti hai.
3. 2 second ke andar poori live website par naya logo, naya font, naye colors, naya price aur naya text lag jata hai!

---

## ❓ Agar Kabhi Koi Dikkat Aaye Toh Kya Karein? (Simple Solutions)

| Dikkat | Kyo Hoti Hai? | Kaise Theek Karein? |
|---|---|---|
| **Website par ₹10 ka naya price ya content nahi dikh raha** | Browser purani memory (cache) dikha raha hai | Keyboard par `Ctrl + Shift + R` dabayein, ya phone me browser refresh karein. |
| **QR code scan karne par error aaye** | UPI ID galat daali hui hogi | Settings me jakar apni sahi UPI ID (jaise: `name@oksbi` ya `mobile@paytm`) daalein. |
| **Android App me "Failed to connect" aa raha hai** | Sheet URL galat paste ho gaya hai | App ke top-right me `⚙️ Sheet URL` par click karein aur apna Web App link dobara paste karein. |
| **Google Sheet me naye options save nahi ho rahe** | Apps Script purana version chala raha hai | Apps Script me jayein ➔ `Deploy` ➔ `Manage deployments` ➔ Pencil icon ➔ Version ko `New version` karke `Deploy` dabayein. |
| **Customer ko download link nahi mila** | UTR number galat daal diya hoga | Android app ke `Orders` tab me jakar customer ka phone number dekhein aur WhatsApp par link bhej dein. |

---

## 📝 Aap Aage Apne Naye Steps Kaise Add Kar Sakte Hain?

Agar aapko aage koi naya feature sochna ho ya is guide me add karna ho, toh aap yahan simple checklist me likh sakte hain:

- [x] **Step 1:** Website banakar ready-made themes aur app styling connect karna. *(Completed)*
- [x] **Step 2:** QR Code payment module banana aur live ₹10 test payment verify karna. *(Completed)*
- [ ] **Step 3:** Live test ke baad app se product pricing ko ₹499 ya ₹999 set karna.
- [ ] **Step 4:** Apni pasand ka logo aur banner gallery se upload karna.
- [ ] **Step 5:** WhatsApp par customer ko automatic invoice bhejna.

---

> 💡 **Tip:** Is file ko hamesha dekhne ke liye VS Code ya Android Studio me bas [`MASTER_WORKFLOW_GUIDE.md`](file:///g:/Vastu_website/MASTER_WORKFLOW_GUIDE.md) par click karein!
