# KidShield Pro (किड्स लॉक - Fingerprint Edition)

A modern **Kotlin + Jetpack Compose** Android Parental Control & Screen Protection app designed for parents who want fingerprint-only access, persistent monitored app lists (like YouTube), custom lock alert sounds, a **Fake PIN Lock Trap**, configurable **After-Timer Lock Protection**, and **Device Admin + Accessibility Anti-Uninstall Guard**.

---

## Features Included (मुख्य फीचर्स)
- **Fingerprint-Only Access:** Uses Android `BiometricPrompt` strictly with biometrics (no PIN/Pattern fallback).
- **Persistent App List:** YouTube is pre-saved by default; add/remove any installed app without re-adding every time.
- **Custom Timer + Instant ACTIVE Launch:** Set Minutes/Seconds and press **ACTIVE** -> KidShield locks immediately and opens YouTube.
- **Custom Alert Sound:** Pick any `.mp3` / `.wav` / audio file from your phone to play automatically when the timer reaches `00:00`.
- **Fake PIN Lock Overlay:** Full-screen realistic PIN trap where every entered PIN fails with shake feedback; only **Parent Fingerprint** unlocks the phone.
- **After-Timer Protection Limit:** Choose `15 min`, `30 min`, `1 hour`, `2 hours`, or `Until Fingerprint Only` in Settings.
- **Device Administrator + Accessibility Service:** Strengthens lock enforcement and blocks unauthorized uninstallation.

---

## GitHub से APK कैसे बनाएं (Step-by-Step Guide)

1. इस ZIP फाइल (`KidShieldPro-Android-Kotlin.zip`) को अपने फोन या कंप्यूटर में **Unzip / Extract** करें।
2. [GitHub.com](https://github.com) पर लॉगिन करें और एक **New Repository** बनाएं (जैसे `KidShieldPro`)।
3. `KidShieldPro` फोल्डर के अंदर की **सारी फाइलें और फोल्डर** (ध्यान रहे `.github` फोल्डर भी ज़रूर अपलोड हो!) अपनी नई Repository में अपलोड करें और **Commit** करें।
4. अब GitHub पर ऊपर **Actions** टैब में जाएं।
5. वहाँ **"Build KidShield Pro APK"** नाम का Workflow अपने आप चलता दिखेगा (या आप **Run workflow** बटन दबा सकते हैं)।
6. लगभग 2–3 मिनट में जब ग्रीन टिक (✔) आ जाए, उस पर क्लिक करें और नीचे **Artifacts** सेक्शन से **`KidShieldPro-APK`** डाउनलोड करें।
7. डाउनलोड हुई फाइल को Unzip करके **`KidShieldPro-v1.0.apk`** अपने Android फोन में इंस्टॉल करें।

---

## First-Time Setup on Android Phone
1. ऐप खोलें और अपने **Parent Fingerprint** से अंदर जाएं।
2. ऊपर दाईं ओर **Settings (⚙)** आइकन पर टैप करें और ये 4 परमिशन चालू करें:
   - **Display Over Other Apps (Overlay)**
   - **Accessibility Service (`KidShield Pro Lock Guard`)**
   - **Device Administrator (`KidShield Pro Anti-Uninstall`)**
   - **Notifications**
3. अपनी पसंद का **After-Timer Protection Limit** (`15 min`, `30 min`, `1 hr`, `2 hr`, या `Until Fingerprint Only`) चुनें।
4. Dashboard पर वापस आएं, अपना **Alert Sound** चुनें, टाइमर सेट करें और **ACTIVE** दबाएं!
