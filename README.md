# Maya Assistant v2.3 🎙️

**Maya AI Assistant** - Real-time background voice assistant with offline "OK Maya" wake-word detection, natural sweet female Hindi voice, Boss/Owner voice recognition ("हंटर / बॉस प्रोटेक्शन"), hands-free WhatsApp sender, crash-free Instagram DM, Gemini Image generator, and YouTube auto-play.

---

## 📲 Direct APK Download Links (डायरेक्ट डाउनलोड लिंक)

### Option 1: Direct Release Download (सीधा डाउनलोड)
जब आप कोड गिटहब पर पुश करते हैं, तो GitHub Actions द्वारा ऑटोमैटिकली नया APK रिलीज़ हो जाता है:

👉 **Direct Download Link:**
```
https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases/latest/download/app-debug.apk
```
*(बस `<YOUR_GITHUB_USERNAME>` की जगह अपना गिटहब यूजरनेम और `<YOUR_REPO_NAME>` की जगह अपने रिपॉजिटरी का नाम डालें और ब्राउज़र में खोलें — v2.3 APK तुरंत डाउनलोड हो जाएगा!)*

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

## ✨ What's Fixed & New in v2.3 (नया क्या है और क्या ठीक किया गया):

1. 👑 **Boss Voice Protection (मालिक की पहचान):**
   - माया अब अपने बॉस (हंटर / राजन बाबू) को पहचानती है। "क्या तुम अपने बॉस की आवाज पहचानती हो?" या "मेरी आवाज पहचानती हो?" पूछने पर प्यार से पुष्टि करती है कि बॉस की आवाज उसके सिस्टम और दिल में एनरोल है।

2. 💖 **Sweet Natural Hindi Female Voice (स्वीट और प्यारी आवाज):**
   - Google Neural TTS इंजन से कनेक्टेड, फ़ीमेल हिंदी आवाज़, परफेक्ट पिच (1.22x) और नेचुरल स्पीड (1.02x) के साथ, जैसा ओरिजिनल वीडियो में था ("हाँ जान! बैटरी 39% है...").

3. 💬 **WhatsApp Auto-Send & Contact Matching (व्हाट्सएप फिक्स):**
   - "विकास शर्मा को हाय लिखकर सेंड करो" बोलने पर "है लिखकर" टाइप होने वाला बग खत्म। अब यह सीधे स्वच्छ संदेश ("हाय") भेजता है।
   - स्क्रीन पर देवनागरी/हिंदी नाम (`विकास शर्मा`, `रितेश विश्वकर्मा`) मिलने पर बिना किसी देरी के सीधे चैट खोलकर ऑटो-सेंड करता है।

4. 🎨 **Gemini Image Generation Prompt (जेमिनी इमेज जनरेटर):**
   - "जेमनाई पर एक इमेज बनाओ शेर का" बोलने पर जेमिनी ऐप खोलकर प्रॉम्प्ट बॉक्स में इंग्लिश 8K सिनेमैटिक प्रॉम्प्ट ऑटोमैटिकली पेस्ट करके सबमिट करता है।

5. 🛡️ **Instagram Crash Fix (इंस्टाग्राम क्रैश फिक्स):**
   - पहले `ig.me/m/` लिंक की वजह से इंस्टाग्राम एरर पॉपअप देकर तुरंत बंद/क्रैश हो जाता था। अब इंस्टाग्राम सेफ डायरेक्ट इनबॉक्स के जरिए खुलता है बिना किसी क्रैश के।

6. 🎵 **YouTube Song Search & Auto-Play:**
   - "Ok Maya YouTube पर बैरन गाना सर्च करके प्ले करो" बोलने पर सर्च क्वेरी से "ओके माय" पूरी तरह साफ़ करके सीधा वीडियो ऑटो-प्ले करता है।

7. 🛠️ **GitHub Actions KSP Build Error Fix:**
   - GitHub Actions में `kspDebugKotlin` पर आ रहा `NullPointerException (AWT-EventQueue-0)` एरर खत्म कर दिया गया है। अब GitHub Actions में APK बिल्ड 100% सक्सेसफुल होगा।
