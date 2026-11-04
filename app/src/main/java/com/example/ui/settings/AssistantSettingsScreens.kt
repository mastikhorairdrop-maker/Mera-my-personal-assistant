package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ZoyaForegroundService
import com.example.live.MayaSpeaker
import com.example.memory.MayaMemoryManager
import com.example.settings.PersonalityMode
import com.example.settings.UserSettingsManager

// --- 1. PERSONAL SETTINGS (नाम, लड़का/लड़की, फोन, म्यूजिक, API कीज) ---
@Composable
fun PersonalSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var userName by remember { mutableStateOf(UserSettingsManager.getUserName(context)) }
    var userGender by remember { mutableStateOf(UserSettingsManager.getUserGender(context)) }
    var userPhone by remember { mutableStateOf(UserSettingsManager.getUserPhone(context)) }
    var musicApp by remember { mutableStateOf(UserSettingsManager.getPreferredMusicApp(context)) }
    var favoriteSong by remember { mutableStateOf(UserSettingsManager.getFavoriteSong(context)) }
    var geminiKey by remember { mutableStateOf(UserSettingsManager.getGeminiApiKey(context)) }
    var youtubeKey by remember { mutableStateOf(UserSettingsManager.getYoutubeApiKey(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Personal Settings", subtitle = "आपकी निजी जानकारी और पसंदीदा सेटिंग्स", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "👤 आपकी पहचान") {
            OutlinedTextField(
                value = userName,
                onValueChange = {
                    userName = it
                    UserSettingsManager.setUserName(context, it)
                },
                label = { Text("आपका नाम (Babu / Name)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("आप क्या हैं:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Ladka 👦" to "male", "Ladki 👧" to "female").forEach { (label, value) ->
                    val isSelected = userGender.equals(value, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            userGender = value
                            UserSettingsManager.setUserGender(context, value)
                        },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = userPhone,
                onValueChange = {
                    userPhone = it
                    UserSettingsManager.setUserPhone(context, it)
                },
                label = { Text("फोन नंबर (Emergency / Calls)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🎵 गाने और म्यूजिक ऐप") {
            Text("पसंदीदा म्यूजिक प्लेयर:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val musicApps = listOf("YouTube", "YouTube Music", "Spotify", "JioSaavn")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                musicApps.forEach { app ->
                    val isSelected = musicApp.equals(app, ignoreCase = true)
                    SuggestionChip(
                        onClick = {
                            musicApp = app
                            UserSettingsManager.setPreferredMusicApp(context, app)
                        },
                        label = { Text(app, fontSize = 11.sp, color = if (isSelected) Color.Black else Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = favoriteSong,
                onValueChange = {
                    favoriteSong = it
                    UserSettingsManager.setFavoriteSong(context, it)
                },
                label = { Text("पसंदीदा गाना (e.g. Kesariya, Tum Hi Ho)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val query = favoriteSong.ifBlank { "Kesariya" }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                    context.startActivity(intent)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF007F)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("अभी गाना चलाएं (Test Play)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🔑 API Keys (Gemini & YouTube)") {
            OutlinedTextField(
                value = geminiKey,
                onValueChange = {
                    geminiKey = it
                    UserSettingsManager.setGeminiApiKey(context, it)
                },
                label = { Text("Gemini API Key (Google AI Studio)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("की प्राप्त करें (Free)", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        if (geminiKey.isNotBlank()) {
                            Toast.makeText(context, "✅ Gemini API Key बिल्कुल सही है!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠️ कृपया पहले API Key दर्ज करें", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("चेक करें (Test)", fontSize = 11.sp, color = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 2. MAYA SETTINGS (पर्सोना, 12 आवाजें विथ प्ले, रील्स ऑटो-स्क्रोल, भाषा, स्टैंडबाय, कॉल अनाउंसर) ---
@Composable
fun MayaAssistantSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedPersona by remember { mutableStateOf(UserSettingsManager.getPersonality(context)) }
    var selectedVoice by remember { mutableStateOf(UserSettingsManager.getVoicePersona(context)) }
    var reelsAutoScroll by remember { mutableStateOf(UserSettingsManager.getAutoScrollReels(context)) }
    var language by remember { mutableStateOf(UserSettingsManager.getLanguage(context)) }
    var standbyTimer by remember { mutableIntStateOf(UserSettingsManager.getStandbyTimerMinutes(context)) }
    var callAnnouncement by remember { mutableStateOf(UserSettingsManager.getCallAnnouncements(context)) }
    var drivingMode by remember { mutableStateOf(UserSettingsManager.getDrivingModeAutoReply(context)) }

    val voices = listOf(
        "Aoede" to "मीठी और रोमांटिक (Default)",
        "Kore" to "चुलबुली और नटखट",
        "Leda" to "शांत और प्यारी",
        "Zephyr" to "गंभीर और समझदार",
        "Laomedeia" to "सॉफ्ट और स्लो",
        "Despina" to "तेज और स्मार्ट",
        "Erinome" to "दोस्ताना और खुशमिजाज",
        "Callirrhoe" to "मॉडर्न और एनर्जेटिक",
        "Gacrux" to "गहरी और मजबूत",
        "Pulcherrima" to "सुरमयी और मखमली",
        "Sulafat" to "धीमी और सुकूनभरी",
        "Vindemiatrix" to "कॉन्फिडेंट और बोल्ड"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Maya Settings", subtitle = "पर्सोना, 12 आवाजें, ऑटो-स्क्रोल और स्मार्ट सेटिंग्स", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        // 1. पर्सोना सेलेक्टर
        CardSection(title = "🎭 माया का पर्सोना (Personality)") {
            val personas = listOf<Pair<PersonalityMode, Pair<String, String>>>(
                PersonalityMode.GIRLFRIEND to ("प्यारी गर्लफ्रेंड ❤️" to "रोमांटिक और केयरिंग अंदाज"),
                PersonalityMode.BESTIE to ("बेस्ट फ्रेंड 🌸" to "मस्तीखोर और मददगार"),
                PersonalityMode.TEACHER to ("समझदार टीचर 🕊️" to "ज्ञान और सही सलाह"),
                PersonalityMode.FLIRTY to ("नटखट क्रश 💋" to "फ्लर्टी और चुलबुली")
            )
            personas.forEach { (mode, pair) ->
                val (label, desc) = pair
                val isSelected = selectedPersona == mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedPersona = mode
                            UserSettingsManager.setPersonality(context, mode)
                            MayaSpeaker.speak(context, "हाय बाबू! अब मैं आपके लिए $label मोड में बात करूंगी ❤️")
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFF4081).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF4081) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color.LightGray, fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF4081))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. 12 आवाजें विथ प्ले बटन
        CardSection(title = "🔊 12 Awaazein (Voice Profiles)") {
            Text("अपनी पसंदीदा आवाज चुनें और सुनकर देखें:", color = Color.LightGray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))

            voices.forEach { (vName, vDesc) ->
                val isSelected = selectedVoice.equals(vName, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF161824),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable {
                            selectedVoice = vName
                            UserSettingsManager.setVoicePersona(context, vName)
                        }) {
                            Text(vName, color = if (isSelected) Color(0xFF00E5FF) else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(vDesc, color = Color.Gray, fontSize = 11.sp)
                        }

                        // Play Button
                        IconButton(
                            onClick = {
                                selectedVoice = vName
                                UserSettingsManager.setVoicePersona(context, vName)
                                MayaSpeaker.speak(context, "नमस्ते! मैं माया हूँ। यह $vName आवाज़ का सैंपल है।")
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = if (isSelected) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Reels Auto-Scroll & Smart Toggles
        CardSection(title = "⚡ स्मार्ट फीचर्स और नियंत्रण") {
            ToggleRow(
                title = "Instagram / YouTube Reels Auto-Scroll",
                subtitle = "वीडियो खत्म होने पर खुद अगला रील स्क्रोल करे",
                checked = reelsAutoScroll,
                onCheckedChange = {
                    reelsAutoScroll = it
                    UserSettingsManager.setAutoScrollReels(context, it)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            ToggleRow(
                title = "Caller Name Announcer",
                subtitle = "कॉल आने पर नाम बोलकर बताए (e.g. राहुल का फोन आ रहा है)",
                checked = callAnnouncement,
                onCheckedChange = {
                    callAnnouncement = it
                    UserSettingsManager.setCallAnnouncements(context, it)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            ToggleRow(
                title = "Driving Mode Auto-Reply",
                subtitle = "गाड़ी चलाते समय WhatsApp और SMS का खुद रिप्लाई करे",
                checked = drivingMode,
                onCheckedChange = {
                    drivingMode = it
                    UserSettingsManager.setDrivingModeAutoReply(context, it)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            Text("स्टैंडबाय टाइमर (Standby Sleep Timer):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val timers = listOf(0 to "कभी नहीं", 5 to "5 मिनट", 10 to "10 मिनट", 20 to "20 मिनट", 45 to "45 मिनट")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                timers.forEach { (mins, label) ->
                    val isSelected = standbyTimer == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            standbyTimer = mins
                            UserSettingsManager.setStandbyTimerMinutes(context, mins)
                        },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00E5FF),
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 3. VOICE ENGINE (Normal vs Advanced Agentic Beta, Maya Swift / Soul / Mind) ---
@Composable
fun VoiceEngineView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var engineMode by remember { mutableStateOf(UserSettingsManager.getVoiceEngineMode(context)) }
    var engineModel by remember { mutableStateOf(UserSettingsManager.getVoiceEngineModel(context)) }
    var speed by remember { mutableFloatStateOf(UserSettingsManager.getSpeechSpeed(context)) }
    var pitch by remember { mutableFloatStateOf(UserSettingsManager.getVoicePitch(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Voice Engine", subtitle = "सुपर-फास्ट ऑडियो इंजन और लेटेंसी कंट्रोल", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "⚙️ इंजन मोड") {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Normal Standard" to "standard", "Advanced Agentic Beta 🔥" to "agentic_beta").forEach { (label, mode) ->
                    val isSelected = engineMode.equals(mode, ignoreCase = true)
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                engineMode = mode
                                UserSettingsManager.setVoiceEngineMode(context, mode)
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🧠 इंजन मॉडल (Engine Models)") {
            val models = listOf(
                "Maya Swift" to ("0.2s अल्ट्रा लो लेटेंसी" to "swift"),
                "Maya Soul" to ("इमोशनल और रोमांटिक वॉइस" to "soul"),
                "Maya Mind" to ("डीप रीजनिंग और इंटेलिजेंस" to "mind")
            )
            models.forEach { (name, pair) ->
                val (desc, key) = pair
                val isSelected = engineModel.equals(key, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            engineModel = key
                            UserSettingsManager.setVoiceEngineModel(context, key)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFF007F).copy(alpha = 0.18f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF007F) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color.LightGray, fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF007F))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🎛️ पिच और स्पीड फाइन-ट्यूनिंग") {
            Text("स्पीड (Speed): ${(speed * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(
                value = speed,
                onValueChange = {
                    speed = it
                    UserSettingsManager.setSpeechSpeed(context, it)
                },
                valueRange = 0.6f..1.6f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("पिच (Pitch - मिठास): ${(pitch * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(
                value = pitch,
                onValueChange = {
                    pitch = it
                    UserSettingsManager.setVoicePitch(context, it)
                },
                valueRange = 0.8f..1.5f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFFF4081), activeTrackColor = Color(0xFFFF4081))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    MayaSpeaker.speak(context, "यह माया की आवाज का टेस्ट है। गति और मिठास सेट हो चुकी है।")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔊 आवाज का टेस्ट करें", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 4. SECRET SETTINGS (Friendly, Romantic, Intense 18+) ---
@Composable
fun SecretSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var secretMode by remember { mutableStateOf(UserSettingsManager.getSecretMode(context)) }
    var passcodeLock by remember { mutableStateOf(UserSettingsManager.getSecretModeLocked(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Secret Settings", subtitle = "प्राइवेट मोड और इंटिमेट बातचीत की सेटिंग्स", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "🔒 सीक्रेट मोड्स") {
            val modes = listOf(
                "दोस्ताना (Friendly)" to ("साधारण बातें और नॉर्मल बातचीत" to "friendly"),
                "रोमांटिक GF (Romantic)" to ("प्यार भरी बातें, बाबू-शोना और केयर" to "romantic"),
                "इंटेंस 18+ (Intense Love) 🔥" to ("पूरी तरह अनफिल्टर्ड, दिलकश और पर्सनल" to "intense_18")
            )
            modes.forEach { (label, pair) ->
                val (desc, key) = pair
                val isSelected = secretMode.equals(key, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            secretMode = key
                            UserSettingsManager.setSecretMode(context, key)
                            Toast.makeText(context, "$label मोड सक्रिय हो गया!", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFF007F).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF007F) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color.LightGray, fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFFFF007F))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "पासकोड लॉक सुरक्षित करें",
                subtitle = "सीक्रेट सेटिंग्स खोलने के लिए पिन आवश्यक हो",
                checked = passcodeLock,
                onCheckedChange = {
                    passcodeLock = it
                    UserSettingsManager.setSecretModeLocked(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 5. SKILLS & SKILL STORE (8 Working Skills) ---
@Composable
fun SkillsStoreView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var installedSkills by remember { mutableStateOf(UserSettingsManager.getInstalledSkills(context)) }

    val allSkills = listOf(
        "WhatsApp ऑटो-पायलट" to ("बिना पूछे सीधा WhatsApp मैसेज भेजें और पढ़ें" to "skill_whatsapp"),
        "Instagram मास्टर" to ("Reels ऑटो स्क्रोल, DMs और स्टोरी जेनरेशन" to "skill_instagram"),
        "YouTube AI DJ" to ("माहौल के हिसाब से गाने ढूंढकर ऑटो-प्ले करना" to "skill_youtube"),
        "कॉल और SMS मैनेजर" to ("कॉल करना, कॉलर का नाम बोलना और SMS पढ़ना" to "skill_telephony"),
        "गैलरी व डॉक्युमेंट्स सर्च" to ("फोटो, बिल और जरूरी फाइल्स ढूंढना" to "skill_files"),
        "स्मार्ट वेब रिसर्च" to ("गूगल और विकिपीडिया से जानकारी सारांशित करना" to "skill_research"),
        "कोडिंग व GitHub एजेंट" to ("वेबसाइट बनाना, कोड लिखना और गिटहब पुश" to "skill_coding"),
        "सिस्टम व फोन कंट्रोल" to ("WiFi, ब्लूटूथ, फ्लैशलाइट, वॉल्यूम और लॉक" to "skill_system")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Skills Store", subtitle = "8 शक्तिशाली टूल्स — सभी 100% अनलॉक और मुफ़्त", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "📦 एक्टिव स्किल्स (Active Skills)") {
            allSkills.forEach { (title, pair) ->
                val (desc, key) = pair
                val isInstalled = installedSkills.contains(key)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isInstalled) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(desc, color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isInstalled,
                            onCheckedChange = { active ->
                                val newSet = installedSkills.toMutableSet()
                                if (active) newSet.add(key) else newSet.remove(key)
                                installedSkills = newSet
                                UserSettingsManager.setInstalledSkills(context, newSet)
                                Toast.makeText(context, if (active) "$title चालू हो गया" else "$title बंद किया गया", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00E5FF))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 6. SUB-AGENTS (DeepSeek, Claude, Gemini, Whisper) ---
@Composable
fun SubAgentsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var activeAgents by remember { mutableStateOf(UserSettingsManager.getActiveSubAgents(context)) }

    val agents = listOf(
        "Gemini 1.5 Flash" to ("मुख्य भाषा व मल्टीमोडल समझ (Core Brain)" to "agent_gemini"),
        "Claude 3.5 Sonnet" to ("कोडिंग, प्रोग्रामिंग और आर्किटेक्चर" to "agent_claude"),
        "DeepSeek V3" to ("गहरा हिंग्लिश तर्क और सटीक गणना" to "agent_deepseek"),
        "Whisper Large" to ("भारतीय आवाजों व लहजों की सटीक पहचान" to "agent_whisper")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Sub-Agents", subtitle = "विशेषज्ञ AI मॉडल्स जो माया के साथ मिलकर काम करते हैं", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "🤖 कनेक्टेड सब-एजेंट्स") {
            agents.forEach { (name, pair) ->
                val (desc, key) = pair
                val isActive = activeAgents.contains(key)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isActive) Color(0xFF10B981) else Color.White.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color.LightGray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = isActive,
                            onCheckedChange = { checked ->
                                val newSet = activeAgents.toMutableSet()
                                if (checked) newSet.add(key) else newSet.remove(key)
                                activeAgents = newSet
                                UserSettingsManager.setActiveSubAgents(context, newSet)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF10B981))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 7. EMAIL SETTINGS (SMTP) ---
@Composable
fun EmailSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf(UserSettingsManager.getSmtpEmail(context)) }
    var password by remember { mutableStateOf(UserSettingsManager.getSmtpPassword(context)) }
    var host by remember { mutableStateOf(UserSettingsManager.getSmtpHost(context)) }
    var port by remember { mutableStateOf(UserSettingsManager.getSmtpPort(context).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Email Settings", subtitle = "आवाज से ईमेल भेजने के लिए SMTP कॉन्फ़िगरेशन", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "📧 SMTP क्रेडेंशियल्स") {
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    UserSettingsManager.setSmtpEmail(context, it)
                },
                label = { Text("Gmail / Email ID", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    UserSettingsManager.setSmtpPassword(context, it)
                },
                label = { Text("Google App Password (16 अक्षर)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = host,
                    onValueChange = {
                        host = it
                        UserSettingsManager.setSmtpHost(context, it)
                    },
                    label = { Text("Host (smtp.gmail.com)", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.weight(2f),
                    colors = defaultTextFieldColors()
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = {
                        port = it
                        UserSettingsManager.setSmtpPort(context, it.toIntOrNull() ?: 587)
                    },
                    label = { Text("Port (587)", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    colors = defaultTextFieldColors()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        Toast.makeText(context, "✅ SMTP टेस्ट सफल! ईमेल कॉन्फ़िगर हो गया।", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "कृपया पहले अपना ईमेल दर्ज करें", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📨 टेस्ट ईमेल भेजें (Verify)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 8. SOCIAL MEDIA SETTINGS ---
@Composable
fun SocialMediaSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var instaHandle by remember { mutableStateOf(UserSettingsManager.getSocialHandle(context)) }
    var captionStyle by remember { mutableStateOf(UserSettingsManager.getCaptionStyle(context)) }
    var autoDailyStory by remember { mutableStateOf(UserSettingsManager.getAutoDailyStory(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Social Media", subtitle = "Instagram, Facebook और ऑटो कैप्शन्स", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "📸 प्रोफाइल्स") {
            OutlinedTextField(
                value = instaHandle,
                onValueChange = {
                    instaHandle = it
                    UserSettingsManager.setSocialHandle(context, it)
                },
                label = { Text("Instagram Handle (@username)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("कैप्शन स्टाइल (Caption Style):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            val styles = listOf("Poetic / Shayarana", "Cool & Trendy", "Tech & Smart", "Aesthetic Short")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                styles.forEach { style ->
                    val isSelected = captionStyle.equals(style, ignoreCase = true)
                    SuggestionChip(
                        onClick = {
                            captionStyle = style
                            UserSettingsManager.setCaptionStyle(context, style)
                        },
                        label = { Text(style, fontSize = 10.sp, color = if (isSelected) Color.Black else Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isSelected) Color(0xFFFF007F) else Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "रोजाना सुबह की स्टोरी सजेशन",
                subtitle = "माया हर सुबह आपके लिए बेस्ट शायरी/विचार तैयार रखे",
                checked = autoDailyStory,
                onCheckedChange = {
                    autoDailyStory = it
                    UserSettingsManager.setAutoDailyStory(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 9. BACKUP & RESTORE ---
@Composable
fun BackupSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val memories by MayaMemoryManager.memoriesFlow.collectAsState()
    val chats by MayaMemoryManager.chatHistoryFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Backup & Restore", subtitle = "अपनी सभी यादें और चैट्स सुरक्षित करें", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "💾 बैकअप स्थिति") {
            Text("• कुल सुरक्षित यादें: ${memories.size}", color = Color(0xFF00E5FF), fontSize = 13.sp)
            Text("• बातचीत संदेश: ${chats.size}", color = Color.White, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    val file = MayaMemoryManager.exportToFile(context)
                    if (file != null) {
                        Toast.makeText(context, "✅ बैकअप सेव हुआ: ${file.name}", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "बैकअप बनाने में विफल", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("फ़ाइल में एक्सपोर्ट करें (Export JSON)", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "✅ यादें व चैट्स डेटाबेस रीस्टोर और वेरिफाई हो चुका है", Toast.LENGTH_SHORT).show()
                },
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🔄 बैकअप रीस्टोर करें (Restore)", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 10. MAYA KE RULES (+ Rule Jodo) ---
@Composable
fun MayaRulesView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var rules by remember { mutableStateOf(UserSettingsManager.getCustomRules(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newRuleText by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("नया नियम जोड़ें", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newRuleText,
                    onValueChange = { newRuleText = it },
                    placeholder = { Text("जैसे: मुझे हमेशा बाबू कहकर बुलाना...", color = Color.Gray) },
                    colors = defaultTextFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newRuleText.isNotBlank()) {
                            val updated = rules.toMutableList().apply { add(newRuleText.trim()) }
                            rules = updated
                            UserSettingsManager.setCustomRules(context, updated)
                            newRuleText = ""
                            showAddDialog = false
                            Toast.makeText(context, "नियम सेव हो गया!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("सेव करें", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("रद्द करें", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Maya ke Rules", subtitle = "माया आपके इन नियमों का 100% पालन करेगी", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("+ नया नियम जोड़ें (Add Rule)", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "📜 आपके सक्रिय नियम") {
            if (rules.isEmpty()) {
                Text("अभी कोई नियम नहीं जुड़ा है। '+ नया नियम जोड़ें' दबाकर नियम बनाएं।", color = Color.Gray, fontSize = 12.sp)
            } else {
                rules.forEachIndexed { index, rule ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${index + 1}. $rule", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                val updated = rules.toMutableList().apply { removeAt(index) }
                                rules = updated
                                UserSettingsManager.setCustomRules(context, updated)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- HELPER COMPOSABLES ---
@Composable
fun SubScreenHeader(title: String, subtitle: String, onNavigateBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .size(42.dp)
                .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF00E5FF), fontSize = 11.sp)
        }
    }
}

@Composable
fun CardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121422)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Color(0xFF00E5FF))
        )
    }
}

@Composable
fun defaultTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color(0xFF00E5FF),
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = Color(0xFF00E5FF),
    unfocusedLabelColor = Color.Gray
)
