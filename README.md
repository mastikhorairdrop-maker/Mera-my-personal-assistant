# Maya Assistant v2.2 🎙️

**Maya AI Assistant** - Real-time background voice assistant with offline "OK Maya" wake-word detection, natural sweet female Hindi voice, hands-free automation, WhatsApp smart sender, Instagram DM integration, and Google Gemini integration.

---

## 📲 Direct APK Download Links (डायरेक्ट डाउनलोड लिंक)

### Option 1: Direct Release Download (सीधा डाउनलोड)
अगर आपने इस प्रोजेक्ट को अपने GitHub पर पुश किया है, तो GitHub Actions द्वारा ऑटोमैटिकली रिलीज़ बन जाता है:

👉 **Direct Download Link:**
```
https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases/latest/download/app-debug.apk
```
*(बस `<YOUR_GITHUB_USERNAME>` की जगह अपना गिटहब यूजरनेम और `<YOUR_REPO_NAME>` की जगह अपने रिपॉजिटरी का नाम डालें और ब्राउज़र में खोलें — APK तुरंत डाउनलोड हो जाएगा!)*

---

### Option 2: GitHub Releases Page (रिलीज़ पेज से)
1. अपने GitHub Repo के होमपेज पर जाएं:
   `https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/releases`
2. **Latest Release** के अंदर **Assets** सेक्शन में `app-debug.apk` पर क्लिक करके डाउनलोड करें।

---

### Option 3: GitHub Actions Artifacts (एक्शन आर्टिफैक्ट से)
1. GitHub Repo में ऊपर **Actions** टैब पर जाएं:
   `https://github.com/<YOUR_GITHUB_USERNAME>/<YOUR_REPO_NAME>/actions`
2. सबसे ऊपर वाले रन (**Build and Release Debug APK**) पर क्लिक करें।
3. नीचे **Artifacts** सेक्शन में `app-debug-apk` पर क्लिक करके डाउनलोड करें।

---

### Option 4: AI Studio से सीधा डाउनलोड (Instant)
1. AI Studio के फाइल एक्सप्लोरर में जाएं:
   `app/build/outputs/apk/debug/app-debug.apk` या `.build-outputs/app-debug.apk`
2. फाइल पर **Right-Click** करें और **Download** दबाएं।
3. या AI Studio के ऊपर दाईं ओर (Top Right) **Settings (⚙️) / Export** मेन्यू से **Download APK** चुनें।

---

## ✨ What's Fixed & New in v2.2 (नया क्या है और क्या ठीक किया गया):
- 💖 **Sweet Natural Voice Fixed:** `MayaSpeaker` को एप्लीकेशन-लेवल पर ऑटो-इनिशियलाइज़ कर दिया गया है। माया अब बिल्कुल चुप या रोबोटिक नहीं रहेगी बल्कि स्वीट, प्यारी आवाज़ में बात करेगी।
- ⚡ **Zero-Freeze Watchdog Timer:** पहले आवाज़ न आने की वजह से ऐप "Speaking with you" पर अटक जाता था। अब वॉचडॉग टाइमर की मदद से माया कभी भी अटकेगी नहीं और तुरंत कमांड सुनेगी।
- 🎙️ **Hindi & Indian English Command Recognizer:** "विकास को WhatsApp पर हाय लिखकर सेंड करो", "फोन लॉक करो", "टॉर्च चालू करो" जैसे सभी कमांड्स को सही से सुनने के लिए स्पीच रिकॉग्नाइज़र को ऑप्टिमाइज़ किया गया है।
- 💬 **WhatsApp Smart Auto-Send:** नाम और मैसेज को साफ़ अलग करके सीधे चैट खोलकर मैसेज टाइप व सेंड करता है।
- 🔒 **Screen Lock:** एक्सेसिबिलिटी से तुरंत फोन लॉक करता है।
- 🛠️ **GitHub Actions CI Headless Fix:** `-Djava.awt.headless=true` जोड़कर GitHub Actions में KSP एरर को पूरी तरह ठीक कर दिया गया है।
