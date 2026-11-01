# Maya Assistant v2.4 🎙️

**Maya AI Assistant** - Real-time background voice assistant with offline "OK Maya" wake-word detection, natural sweet female Hindi voice, Boss/Owner voice recognition ("हंटर / बॉस प्रोटेक्शन"), Touch Guard Mode (टच गार्ड / पॉकेट प्रोटेक्शन), Voice Screen Unlock (ऑटो स्क्रीन अनलॉक), Anti-Theft Motion Siren, hands-free WhatsApp sender, crash-free Instagram DM, Gemini Image generator, and YouTube auto-play.

---

## 📲 Direct APK Download Links (डायरेक्ट डाउनलोड लिंक)

### Option 1: Direct Release Download (सीधा डाउनलोड)
जब आप कोड गिटहब पर पुश करते हैं, तो GitHub Actions द्वारा ऑटोमैटिकली नया APK रिलीज़ हो जाता है:

👉 **Direct Download Link:**
```
https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases/latest/download/app-debug.apk
```
*(बस `<YOUR_GITHUB_USERNAME>` की जगह अपना गिटहब यूजरनेम और `<YOUR_REPO_NAME>` की जगह अपने रिपॉजिटरी का नाम डालें और ब्राउज़र में खोलें — v2.4 APK तुरंत डाउनलोड हो जाएगा!)*

---

### Option 2: GitHub Actions Artifacts (एक्शन आर्टिफैक्ट से)
1. GitHub Repo में ऊपर **Actions** टैब पर जाएं:
   `https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/actions`
2. सबसे ऊपर वाले रन (**Build and Release Debug APK**) पर क्लिक करें (अब यह KSP फिक्स के बाद 100% हरा टिक और सफल होगा!)।
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

## ✨ What's Fixed & New in v2.4 (नया क्या है और क्या ठीक किया गया):

1. 🛡️ **Touch Guard Mode (टच गार्ड / पॉकेट प्रोटेक्शन):**
   - जेब में रखने या हाथ में पकड़ने पर गलती से होने वाले टच को पूरी तरह रोकता है।
   - स्क्रीन पर अगर कोई अनधिकृत टच करता है, तो माया तुरंत चेतावनी देती है: *"चेतावनी! टच गार्ड एक्टिव है! मालिक की अनुमति के बिना फोन को मत छुओ!"*
   - वॉयस कमांड: **"माया टच गार्ड ऑन करो"** या **"टच गार्ड बंद करो"**।

2. 🔓 **Voice Screen Unlock (ऑटोमैटिक स्क्रीन अनलॉक):**
   - आवाज से फोन का लॉक खोलने की सुविधा।
   - जैसे ही आप बोलते हैं: **"Ok Maya फोन खोलो"** या **"स्क्रीन अनलॉक करो"**, माया स्क्रीन जगाती है, लॉकस्क्रीन पर ऊपर स्वाइप करती है और अगर पिन सेट है तो पिन एंटर करके फोन खोल देती है।
   - वॉयस उत्तर: *"जय श्री राम बॉस! स्क्रीन अनलॉक कर दी है। बताइए, अब क्या हुक्म है?"*

3. 🚨 **Anti-Theft Motion Alarm ("Don't Touch My Phone"):**
   - एक्सेलेरोमीटर सेंसर से लैस। अगर फोन मेज पर रखा है और कोई भी इसे उठाता या हिलाता है, तो माया जोर से बोलती है: *"अरे! मेरा फोन किसने उठाया? नीचे रखो इसे! मालिक का फोन है!"* और फोन वाइब्रेट करने लगता है।

4. 👑 **Boss Voice Protection (मालिक की पहचान):**
   - माया अपने बॉस (हंटर / राजन बाबू) की पहचान रखती है। *"क्या तुम अपने बॉस की आवाज पहचानती हो?"* पूछने पर प्यार से पुष्टि करती है।

5. 💖 **Sweet Natural Hindi Female Voice:**
   - Google Neural TTS इंजन से कनेक्टेड, फ़ीमेल हिंदी आवाज़, परफेक्ट पिच (1.22x) और नेचुरल स्पीड (1.02x) के साथ।

6. 💬 **WhatsApp Auto-Send & Contact Matching:**
   - "विकास शर्मा को हाय लिखकर सेंड करो" बोलने पर "है लिखकर" टाइप होने वाला बग खत्म। अब यह सीधे स्वच्छ संदेश ("हाय") भेजता है।

7. 🎨 **Gemini Image Generation Prompt:**
   - "जेमनाई पर एक इमेज बनाओ शेर का" बोलने पर जेमिनी ऐप खोलकर 8K सिनेमैटिक प्रॉम्प्ट ऑटो-पेस्ट और सबमिट करता है।

8. 🛡️ **Crash-Free Instagram DM & YouTube Auto-Play:**
   - Instagram Direct बिना क्रैश हुए सेफ तरीके से खुलता है, और YouTube पर गाने ऑटोमैटिकली प्ले होते हैं।
