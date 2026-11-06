# 🛒 Zero-Knowledge Guide: Apni Website, Google Sheet Aur Mobile App Chalane Ka Asaan Tareeka

> **Namaste!** Agar aapko coding ya technical baaton ka bilkul bhi idea nahi hai (0% knowledge), toh bhi koi chinta nahi!  
> Yeh guide bilkul aam bolchaal ki **Hinglish** me likhi gayi hai. Bas jaise-jaise yahan likha hai, waisa-waisa click karte jayein.

---

## 💡 Sabse Pehle Samajhte Hain: Yeh Pura System Hai Kya?

Aap is poore system ko ek aam dukaan ki tarah samjhein:

1. **Website ([index.html](file:///g:/Vastu_website/index.html)):**  
   Yeh aapki **Dukaan ka Counter** hai jo internet par sabhi logon ko dikhta hai. Customer yahan aakar product dekhta hai aur khareedta hai. Iska koi monthly kharcha nahi hai (0% hosting, 0% domain fee).

2. **Google Sheet ([Code.gs](file:///g:/Vastu_website/Code.gs)):**  
   Yeh aapka **Hisaab-Kitaab ka Register** hai. Jab bhi koi customer order karega ya paise dega, uska naam, mobile number aur paise is sheet me automatic likh jayenge.

3. **Android Mobile App ([MainActivity.kt](file:///g:/Vastu_website/android-admin-app/app/src/main/java/com/sheetstore/admin/ui/MainActivity.kt)):**  
   Yeh aapke haath me **Dukaan ka Remote Control** hai. Is app se aap apne bistar par baith kar 1-click me website ka design, naam, phone number aur product ke daam badal sakte hain.

4. **Auto-Payment Verifier:**  
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

## 🧭 Sirf 5 Aasan Steps Me Pura System Samajhein

---

### Step 1: Website Kahan Hai Aur Look/Theme Kaise Badalti Hai?

Aapki website internet par bilkul free chal rahi hai (GitHub Pages par).
* Jab bhi customer website kholega, usko sundar design, rate aur products dikhenge.
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

### Step 2: Google Sheet Setup (Sirf Copy & Paste)

Google Sheet aapka register hai. Agar aapko kabhi naye sire se nayi sheet banani ho toh bas ye 4 baatein karein:

1. Browser me **[sheets.new](https://sheets.new)** kholein (Ek nayi blank Google Sheet khul jayegi).
2. Upar menu me **Extensions** par click karein aur **Apps Script** chunein.
3. Hamare project ki file [`Code.gs`](file:///g:/Vastu_website/Code.gs) ko pura copy karke wahan paste kar dein.
4. Upar ek dropdown dikhega jisme `doGet` likha hoga, us dropdown par click karke **`setupAutoTabs`** chunein aur **Run** dabayein.
   * *Jaise hi aap Run dabayenge, Google Sheet me 3 tabs apne aap ban jayenge:*
     - **Settings:** Dukaan ka naam, UPI ID, WhatsApp number, Theme, Colors, Fonts.
     - **Products:** Saare bikne wale products aur unke daam.
     - **Orders:** Jo bhi customer khareedega uska live hisaab.
5. Upar blue button **Deploy** dabayein ➔ **New deployment** chunein:
   - "Select type" me **Web app** chunein.
   - "Who has access" me **Anyone** chunein.
   - **Deploy** dabayein aur jo **Web app URL** mile, use copy kar lein!

---

### Step 3: Website Ko Google Sheet Se Jodna (1 Minute Ka Kaam)

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

### Step 4: Mobile App Se Poori Website Ko Control Karna

Aapko website badalne ke liye computer kholne ki zaroorat nahi hai. Sab kuch aapke mobile app se hoga:

1. **Android Studio** kholein.
2. **Open** dabakar yeh folder chunein: `g:\Vastu_website\android-admin-app`.
3. Upar green color ka **Play (Run)** button dabakar app ko apne phone me install karein.
4. App khulte hi top-right me **⚙️ Sheet URL** par click karke apna Web App URL save kar lein.

#### App Me Yeh 4 Tabs Milte Hain:

* 🛒 **Tab 1: Orders (Hisaab-Kitaab)**
  - Aaj kitni kamai hui, kitne order aaye.
  - Sabhi orders ki list. Har order ke aage **"Approve [✓]"** button hota hai.
* 🎨 **Tab 2: Themes (1-Click Me Look Badlo)**
  - Yahan 5 sundar ready-made designs ke card bane hain (Vedic Gold, Modern Blue, etc.).
  - Aap bas kisi bhi design ke niche **"Apply This Template ⚡"** dabayein.
  - 1 second me poori duniya me website ka rang-roop badal jayega!
* 🌐 **Tab 3: Settings (Branding, Fonts, Colors & Text Studio)**
  - Dukaan ka naam, Logo, Fonts, Header/Footer Colors aur website ka sara text yahan se badalta hai.
* 📦 **Tab 4: Products (Rate List)**
  - Kisi bhi product ka Selling Price badlein.
  - Agar koi product khatam ho gaya ho, toh use Inactive kar dein taaki customer use na khareed sake.

---

## 🎨 Advance Customization: Font, Logo, Header/Footer Colors & Text App Se Kaise Badlein?

Aapne poocha tha ki *theme ke alawa agar font, logo, header-footer colors aur likha hua content badalna ho toh kaise hoga?*  
Yeh feature aapke Android App ke **🌐 Settings (Tab 3)** me fully add kar diya gaya hai! Dekhiye kya-kya badal sakte hain:

### 1. 🔤 Font (Likhawat Ka Style) Badalna
- **Kaise Karein:** App ke Settings tab me **"Heading Font"** ka box hai.
- **Kya Likh Sakte Hain:**
  - `Cinzel` ➔ Royal traditional serif font (Vastu, Astrology aur luxury ke liye best).
  - `Outfit` ➔ Modern clean tech font (Default).
  - `Space Grotesk` ➔ Bold futuristic font (Gaming aur digital goods ke liye).
  - `Plus Jakarta Sans` ➔ Ultra-clean corporate font.
- App me naam likh kar save karein, website ki saari badi headlines turant us font me convert ho jayengi!

### 2. 🖼️ Custom Logo Badalna
- **Kaise Karein:** App me **"Custom Logo Image URL"** ka box hai.
- Wahan apne logo image ka direct link paste karein (jaise: `https://i.imgur.com/my-logo.png` ya koi bhi online photo link).
- Save dabate hi website ke top navigation bar me default icon hatkar aapka **asli logo** lag jayega!

### 3. 🎨 Header & Footer Ka Background Color Badalna
- **Kaise Karein:** App me do color boxes hain:
  - **Header Background Color:** e.g. `#080C1A` (Dark Slate), `#1E1B4B` (Royal Indigo), `#1A0B2E` (Plum Maroon), ya `#000000` (Pure Black).
  - **Footer Background Color:** e.g. `#050811` ya `#0F172A`.
- Apna pasandida color code daalein aur save karein — website ka top bar aur bottom footer turant naye color me badal jayega!

### 4. ✍️ Website Ka Har Ek Text (Pura Content) Badalna
Aapko kisi coder ke paas jane ki zaroorat nahi hai, website ka ek-ek word aapke mobile app se badal sakta hai:
- **Hero Main Headline:** Jo sabse badi headline screen par aati hai (e.g. *"Vastu Shastra Se Badlein Apni Zindagi"* ya *"Apna E-Commerce Store Shuru Karein"*).
- **Hero Subtitle:** Headline ke niche aane wala 2-line ka vivaran (description).
- **CTA Button Text:** Main button par kya likha ho (e.g. *"Get Store Now"*, *"Abhi Khareedein ₹499"*, ya *"Consultation Book Karein"*).
- **Top Announcement Bar:** Sabse upar chalne wala discount offer text (e.g. *"Diwali Special Offer: 80% OFF Today!"*).
- **Brand Name & Tagline:** Dukaan ka naam aur slogan.

👉 Jaise hi aap App me **"Update Website Live 🚀"** dabayenge:
1. Mobile app ye sara data Google Sheet ki `Settings` tab me bhej deti hai.
2. Website turant Google Sheet se ye naya data fetch karti hai.
3. 2 second ke andar poori live website par naya logo, naya font, naye colors aur naya text lag jata hai!

---

### Step 5: Customer Khareedta Kaise Hai? (Pura Payment Flow)

Dekhiye customer ke order se lekar paise aane tak kya-kya hota hai:

1. **Customer website par aata hai:**
   - Wo product pasand karta hai aur "Buy Now" dabata hai.
2. **Customer details daalta hai:**
   - Apna naam aur WhatsApp number likhta hai.
3. **UPI QR Code aata hai:**
   - Screen par Google Pay / PhonePe / Paytm se scan karne ke liye QR code dikhta hai.
   - Customer ₹499 pay kar deta hai.
4. **Customer UTR Number daalta hai:**
   - Payment hote hi har UPI app ek 12-digit ka UTR / Ref Number deti hai (jaise: `412984920194`).
   - Customer ye number website par daal kar "Verify Payment" dabata hai.
5. **Jadu (Automatic Verification):**
   - Website par likha aata hai: *"Verifying your payment with bank..."*
   - Jaise hi aapke phone par Bank ka SMS aata hai ("Rs 499 credited..."):
     - **Option A (Automatic):** Android app background me SMS padh kar sheet me status `PAID` kar deti hai.
     - **Option B (Manual):** Aap Android app me bas **"Approve [✓]"** dabate hain ya Google Sheet me tick kar dete hain.
   - 3 second ke andar website par confetti phoot-ti hai aur customer ka **success page** khul jata hai jahan se wo product download kar leta hai!

---

## ❓ Agar Kabhi Koi Dikkat Aaye Toh Kya Karein? (Simple Solutions)

| Dikkat | Kyo Hoti Hai? | Kaise Theek Karein? |
|---|---|---|
| **Website par content update nahi hua** | Browser purani memory (cache) dikha raha hai | Keyboard par `Ctrl + Shift + R` dabayein, ya phone me page refresh karein. |
| **Android App me "Failed to connect" aa raha hai** | Sheet URL galat paste ho gaya hai | App ke top-right me `⚙️ Sheet URL` par click karein aur apna Web App link dobara paste karein. |
| **Google Sheet me naye options save nahi ho rahe** | Apps Script purana version chala raha hai | Apps Script me jayein ➔ `Deploy` ➔ `Manage deployments` ➔ Pencil icon ➔ Version ko `New version` karke `Deploy` dabayein. |
| **Customer ko download link nahi mila** | UTR number galat daal diya hoga | Android app ke `Orders` tab me jakar customer ka phone number dekhein aur WhatsApp par link bhej dein. |

---

## 📝 Aap Aage Apne Naye Steps Kaise Add Kar Sakte Hain?

Agar aapko aage koi naya feature sochna ho ya is guide me add karna ho, toh aap yahan simple checklist me likh sakte hain:

- [ ] **Mera Step 1:** Naya logo design banakar URL app me daalna.
- [ ] **Mera Step 2:** Header aur footer ke colors apni pasand ke set karna.
- [ ] **Mera Step 3:** Website ki main headline ko apne business ke hisaab se customize karna.
- [ ] **Mera Step 4:** WhatsApp par customer ko automatic invoice bhejna.

---

> 💡 **Tip:** Is file ko hamesha kholne ke liye VS Code ya Android Studio me bas [`MASTER_WORKFLOW_GUIDE.md`](file:///g:/Vastu_website/MASTER_WORKFLOW_GUIDE.md) par click karein!
