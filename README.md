# Maya Assistant v2.5 🎙️

**Maya AI Assistant** - Real-time background voice assistant with offline "OK Maya" wake-word detection, sweet and natural Hindi female voice, 100% working WhatsApp auto-send, crash-free Instagram launch, Touch Guard Mode (टच गार्ड / पॉकेट प्रोटेक्शन), Voice Screen Unlock (ऑटो स्क्रीन अनलॉक), Anti-Theft Motion Siren, Gemini Image generator, and YouTube auto-play.

---

## 📲 Direct APK Download Links (डायरेक्ट डाउनलोड लिंक)

### Option 1: Direct Release Download (सीधा डाउनलोड)
जब आप कोड गिटहब पर पुश करते हैं, तो GitHub Actions द्वारा ऑटोमैटिकली नया APK रिलीज़ हो जाता है:

👉 **Direct Download Link:**
```
https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases/latest/download/app-debug.apk
```
*(बस `<YOUR_GITHUB_USERNAME>` की जगह अपना गिटहब यूजरनेम और `<YOUR_REPO_NAME>` की जगह अपने रिपॉजिटरी का नाम डालें और ब्राउज़र में खोलें — v2.5 APK तुरंत डाउनलोड हो जाएगा!)*

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

## ✨ What's Fixed & New in v2.5 (नया क्या है और क्या ठीक किया गया):

1. 💬 **WhatsApp Auto-Send Fix (मैसेज सेंड न होने की समस्या दूर):**
   - पहले WhatsApp खुल जाता था लेकिन सेंड बटन नहीं दबता था।
   - अब एक्सेसिबिलिटी सर्विस चैट खुलते ही इनपुट बॉक्स में संदेश टाइप/पेस्ट करती है और तुरंत सीधे नोड एक्शन + स्क्रीन जेस्चर दोनों से सेंड बटन पर क्लिक करके संदेश 100% भेज देती है।

2. 📸 **Instagram 100% Crash-Free (इंस्टाग्राम क्रैश फिक्स):**
   - पहले इंस्टाग्राम पर जाते ही डीप-लिंक एरर से ऐप क्रैश होकर तुरंत बंद हो जाता था।
   - अब इंस्टाग्राम सीधे ऑफिशियल लॉन्चर एक्टिविटी से सेफली खुलता है। कोई एरर, पॉपअप या क्रैश नहीं आएगा।

3. 💖 **Sweet & Natural Voice Tuning (आवाज़ में सुधार):**
   - पिच को 1.22x से घटाकर 1.05x और स्पीड को 0.96x कर दिया गया है। 
   - पुरुष या रोबोटिक आवाजों को पूरी तरह हटाकर केवल शुद्ध भारतीय हिंदी फ़ीमेल न्यूरल वॉइस (`hi-in-x-hie`, `hi-in-x-hid`, `hi-in-x-hib`) को प्राथमिकता दी गई है, जिससे आवाज़ अत्यंत मधुर, शांत और इंसानी सुनाई देती है।

4. 🛡️ **Touch Guard Mode (टच गार्ड / पॉकेट प्रोटेक्शन):**
   - जेब में रखने या हाथ में पकड़ने पर गलती से होने वाले टच को पूरी तरह रोकता है।

5. 🔓 **Voice Screen Unlock (ऑटोमैटिक स्क्रीन अनलॉक):**
   - "Ok Maya फोन खोलो" या "स्क्रीन अनलॉक करो" बोलने पर स्क्रीन ऑन करके स्वाइप और अनलॉक करता है।

6. 🚨 **Anti-Theft Motion Alarm ("Don't Touch My Phone"):**
   - फोन को कोई भी हिलाता या उठाता है तो माया अलार्म बजाती है: *"अरे! मेरा फोन किसने उठाया? नीचे रखो इसे!"*
