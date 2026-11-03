# Maya Assistant v2.6 🎙️

**Maya AI Assistant** - Real-time background voice assistant with offline "OK Maya" wake-word detection, sweet and natural Hindi female voice, 100% working WhatsApp auto-send, crash-free Instagram launch, Full-Screen Touch Guard Shield (टच गार्ड / पॉकेट प्रोटेक्शन), Voice Screen Unlock (ऑटो स्क्रीन अनलॉक), Anti-Theft Motion Siren, Real-time Spoken Announcements (Airplane Mode & Smart Charging), Gemini Image generator, and YouTube auto-play.

---

## 📲 Direct APK Download Links (डायरेक्ट डाउनलोड लिंक)

### Option 1: Direct Release Download (सीधा डाउनलोड)
जब आप कोड गिटहब पर पुश करते हैं, तो GitHub Actions द्वारा ऑटोमैटिकली नया APK रिलीज़ हो जाता है:

👉 **Direct Download Link:**
```
https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases/latest/download/app-debug.apk
```
*(बस `<YOUR_GITHUB_USERNAME>` की जगह अपना गिटहब यूजरनेम और `<YOUR_REPO_NAME>` की जगह अपने रिपॉजिटरी का नाम डालें और ब्राउज़र में खोलें — v2.6 APK तुरंत डाउनलोड हो जाएगा!)*

---

### Option 2: GitHub Actions Artifacts (एक्शन आर्टिफैक्ट से)
1. GitHub Repo में ऊपर **Actions** टैब पर जाएं:
   `https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/actions`
2. सबसे ऊपर वाले रन (**Build and Release Debug APK**) पर क्लिक करें।
3. नीचे **Artifacts** सेक्शन में `app-debug-apk` पर क्लिक करके डाउनलोड करें।

---

### Option 3: GitHub Releases Page (रिलीज़ पेज से)
1. अपने GitHub Repo के होमपेज पर जाएं:
   `https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases`
2. **Latest Release** के अंदर **Assets** सेक्शन में `app-debug.apk` पर क्लिक करके डाउनलोड करें।

---

### Option 4: AI Studio से सीधा डाउनलोड
1. AI Studio के ऊपर दाईं ओर (Top Right) **Settings (⚙️) / Export** मेन्यू से **Download APK** चुनें।
2. या फाइल एक्सप्लोरर में `app/build/outputs/apk/debug/app-debug.apk` पर राइट-क्लिक करके डाउनलोड करें।

---

## ✨ What's Fixed & New in v2.6 (नया क्या है और क्या ठीक किया गया):

1. 🛡️ **Touch Guard Full Screen Protection (टच गार्ड 100% फिक्स):**
   - पहले टच गार्ड केवल स्क्रीन के एक छोटे से हिस्से को छूने देता था और अनुमति न होने पर चुपचाप बंद हो जाता था।
   - **अब:** टच गार्ड एक्टिव होते ही पूरी स्क्रीन पर सुरक्षा कवच (Full-screen overlay blocker) लग जाता है। कोई भी टच स्क्रीन पर काम नहीं करेगा। 
   - अगर कोई स्क्रीन छुएगा तो फोन वाइब्रेट होगा और माया चेतावनी देगी: *"चेतावनी! टच गार्ड एक्टिव है! मालिक की अनुमति के बिना फोन को मत छुओ!"*
   - जब तक नीचे मौजूद "🔓 टच गार्ड अनलॉक करें" बटन पर टैप नहीं किया जाएगा, स्क्रीन सुरक्षित लॉक रहेगी।
   - यदि `Display over other apps` की अनुमति नहीं होगी, तो माया खुद सेटिंग्स स्क्रीन खोलकर अनुमति देने के लिए बोलेगी।

2. ⚙️ **All Phone Control Settings Fully Working (सभी सेटिंग्स फिक्स):**
   - सेटिंग्स में `Phone control` टैब के अंदर सभी जरूरी विकल्प जोड़ दिए गए हैं:
     - **Full Touch Guard Mode** (ऑन/ऑफ और डायरेक्ट टेस्ट)
     - **Display Over Other Apps** अनुमति बटन
     - **Anti-Theft Motion Alarm** (फोन उठाने या हिलाने पर तेज आवाज में चेतावनी)
     - **Voice Screen Unlock** (पिन कोड इनपुट बॉक्स, सेव बटन और टेस्ट अनलॉक बटन)
     - **Airplane Mode Spoken Alerts** (फ्लाइट मोड ऑन/ऑफ होने पर बोलकर सूचित करना)
     - **Battery & Charging Assistant** (चार्जर लगाने पर बैटरी % बताना और 100% फुल होने पर अनप्लग अलार्म)
     - **Accessibility Service** और **Battery Saver Exemption**

3. 💬 **WhatsApp Auto-Send 100% Reliable (मैसेज सेंडिंग फिक्स):**
   - एक्सेसिबिलिटी सर्विस इनपुट बॉक्स में संदेश तुरंत टाइप/पेस्ट करती है और सीधे नोड क्लिक व स्क्रीन जेस्चर दोनों से सेंड बटन दबाकर मैसेज भेज देती है।

4. 📸 **Instagram 100% Crash-Free (इंस्टाग्राम क्रैश फिक्स):**
   - इंस्टाग्राम सीधे ऑफिशियल लॉन्चर एक्टिविटी से सेफली खुलता है। कोई क्रैश नहीं होगा।

5. 💖 **Sweet & Natural Hindi Female Voice:**
   - रोबोटिक या कठोर आवाजों को पूरी तरह हटाकर कोमल, मधुर और प्राकृतिक भारतीय हिंदी आवाज़ सेट की गई है।

6. ⚡ **GitHub Actions AWT NullPointerException Fix:**
   - KSP प्लगइन को डिसेबल करके और `-Djava.awt.headless=true` जोड़कर GitHub Actions में APK बिल्ड को 100% पास करा दिया गया है।
