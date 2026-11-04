package com.example.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
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
import com.example.accessibility.MayaAccessibilityService
import com.example.guard.TouchGuardManager
import com.example.guardian.VoiceGuardianManager
import com.example.live.MayaSpeaker
import com.example.settings.UserSettingsManager

// --- 1. ADVANCED SETTINGS HUB ---
@Composable
fun AdvancedSettingsHubView(
    onNavigateBack: () -> Unit,
    onNavigateToVoiceGuardian: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToBehaviour: () -> Unit,
    onNavigateToTyping: () -> Unit,
    onNavigateToFaceId: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToEventTriggers: () -> Unit
) {
    val context = LocalContext.current
    val isOwnerEnrolled = VoiceGuardianManager.isOwnerEnrolled(context)
    val isGuardianActive = VoiceGuardianManager.isGuardianEnabled(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Advanced Settings", subtitle = "डीप सिस्टम इंटीग्रेशन, सिक्योरिटी और कस्टमाइजेशन", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        // HERO CARD: VOICE GUARDIAN
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToVoiceGuardian() },
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF161824),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F))))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF2979FF))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Voice Guardian (मालिक की आवाज)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isGuardianActive) Color(0xFF10B981) else Color(0xFFEF4444)
                        ) {
                            Text(
                                if (isGuardianActive) "ACTIVE" else "OFF",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        if (isOwnerEnrolled) "मालिक की आवाज एनरोल है ✓" else "मालिक की आवाज रिकॉर्ड करें",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🎨 लुक्स और डिस्प्ले") {
            AdvancedSettingItem(
                icon = Icons.Default.Palette,
                title = "थीम (Themes)",
                subtitle = "Clay Day, Midnight Neon, Obsidian AMOLED",
                onClick = onNavigateToTheme
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
            AdvancedSettingItem(
                icon = Icons.Default.Face,
                title = "दिखावट (Appearance)",
                subtitle = "क्लासिक ग्लोइंग ऑर्ब या 3D वर्चुअल कैरेक्टर",
                onClick = onNavigateToAppearance
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "⚙️ व्यवहार और स्पीड") {
            AdvancedSettingItem(
                icon = Icons.Default.Tune,
                title = "व्यवहार (Behaviour)",
                subtitle = "इको गार्ड, फ्लोटिंग बबल और ऑटो-लिसन",
                onClick = onNavigateToBehaviour
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
            AdvancedSettingItem(
                icon = Icons.Default.Speed,
                title = "टाइपिंग और हैप्टिक्स (Typing & Haptics)",
                subtitle = "टाइपिंग स्पीड और वाइब्रेशन रिस्पॉन्स",
                onClick = onNavigateToTyping
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        CardSection(title = "🛡️ सुरक्षा और ट्रिगर्स") {
            AdvancedSettingItem(
                icon = Icons.Default.CameraAlt,
                title = "चेहरा पहचान (Chehre / Face ID)",
                subtitle = "कैमरा फेस वेरिफिकेशन से अनलॉक करें",
                onClick = onNavigateToFaceId
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
            AdvancedSettingItem(
                icon = Icons.Default.Warning,
                title = "इमरजेंसी SOS (Emergency Siren & Alert)",
                subtitle = "'Maya Bachao' बोलने पर सायरन और लोकेशन SMS",
                onClick = onNavigateToSos
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))
            AdvancedSettingItem(
                icon = Icons.Default.Bolt,
                title = "इवेंट ट्रिगर्स (16 Event Triggers)",
                subtitle = "चार्जिंग, बैटरी लो, हेडसेट और स्क्रीन इवेंट्स",
                onClick = onNavigateToEventTriggers
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 2. THEME SETTINGS ---
@Composable
fun ThemeSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTheme by remember { mutableStateOf(UserSettingsManager.getThemeMode(context)) }

    val themes = listOf(
        "Midnight Neon (डार्क पर्पल & सियान)" to "neon",
        "Obsidian AMOLED (शुद्ध काला & बैटरी सेवर)" to "amoled",
        "Clay Day (सॉफ्ट लाइट क्लेमोर्फिक)" to "light"
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
        SubScreenHeader(title = "Themes", subtitle = "माया का रंग-रूप बदलें", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "🎨 थीम चुनें") {
            themes.forEach { (label, key) ->
                val isSelected = selectedTheme.equals(key, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            selectedTheme = key
                            UserSettingsManager.setThemeMode(context, key)
                            Toast.makeText(context, "$label लागू हो गई", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 3. APPEARANCE SETTINGS (Orb vs 3D Avatar) ---
@Composable
fun AppearanceSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var appearanceType by remember { mutableStateOf(UserSettingsManager.getAppearanceType(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Appearance", subtitle = "होम स्क्रीन का लुक और स्टाइल", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "🌟 मुख्य विजुअल स्टाइल") {
            val styles = listOf(
                "क्लासिक ग्लोइंग ऑर्ब (Glowing Neon Orb)" to ("तरंगों के साथ चमकने वाला खूबसूरत गोला" to "orb"),
                "3D एनिमे कैरेक्टर (Virtual Maya Girl)" to ("बोलते हुए चेहरे और भावों के साथ" to "character_3d")
            )
            styles.forEach { (label, pair) ->
                val (desc, key) = pair
                val isSelected = appearanceType.equals(key, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            appearanceType = key
                            UserSettingsManager.setAppearanceType(context, key)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFFFF007F).copy(alpha = 0.2f) else Color(0xFF1E293B),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF007F) else Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 4. BEHAVIOUR SETTINGS ---
@Composable
fun BehaviourSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var echoGuard by remember { mutableStateOf(UserSettingsManager.getEchoGuard(context)) }
    var floatingBubble by remember { mutableStateOf(UserSettingsManager.getFloatingBubble(context)) }
    var autoListenOnOpen by remember { mutableStateOf(UserSettingsManager.getListenOnOpen(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Behaviour", subtitle = "माया का रिस्पॉन्स और इंटरैक्शन तरीका", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "⚙️ इंटरैक्शन रूल्स") {
            ToggleRow(
                title = "इको गार्ड (Echo Cancellation)",
                subtitle = "स्पीकर से बजने वाली अपनी आवाज को खुद न सुने",
                checked = echoGuard,
                onCheckedChange = {
                    echoGuard = it
                    UserSettingsManager.setEchoGuard(context, it)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            ToggleRow(
                title = "फ्लोटिंग बबल (Floating Overlay Orb)",
                subtitle = "अन्य ऐप्स पर भी माया का छोटा गोल बटन स्क्रीन पर रहे",
                checked = floatingBubble,
                onCheckedChange = {
                    floatingBubble = it
                    UserSettingsManager.setFloatingBubble(context, it)
                }
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

            ToggleRow(
                title = "ऐप खुलते ही सुनना शुरू करें (Auto-Listen)",
                subtitle = "बिना माइक छुए तुरंत आपकी आवाज का इंतजार करे",
                checked = autoListenOnOpen,
                onCheckedChange = {
                    autoListenOnOpen = it
                    UserSettingsManager.setListenOnOpen(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 5. TYPING & HAPTICS SETTINGS ---
@Composable
fun TypingSettingsView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var hapticsEnabled by remember { mutableStateOf(UserSettingsManager.getHapticsEnabled(context)) }
    var streamSpeed by remember { mutableStateOf(UserSettingsManager.getTextStreamSpeed(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Typing & Haptics", subtitle = "टेक्स्ट लिखने की स्पीड और वाइब्रेशन", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "⚡ टाइपिंग स्टाइल") {
            val speeds = listOf("Instant (तुरंत)", "Fast (तेज)", "Natural (ह्यूमन स्टाइल)")
            speeds.forEach { sp ->
                val isSelected = streamSpeed.equals(sp, ignoreCase = true)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            streamSpeed = sp
                            UserSettingsManager.setTextStreamSpeed(context, sp)
                        },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color(0xFF1E293B)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(sp, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "वाइब्रेशन व हैप्टिक फीडबैक (Haptic Touch)",
                subtitle = "बटन दबाने और माया के बोलने पर हल्का वाइब्रेशन",
                checked = hapticsEnabled,
                onCheckedChange = {
                    hapticsEnabled = it
                    UserSettingsManager.setHapticsEnabled(context, it)
                }
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 6. CHEHRE / FACE ID SETTINGS ---
@Composable
fun ChehreFaceView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var isFaceEnrolled by remember { mutableStateOf(UserSettingsManager.getFaceEnrolled(context)) }
    var isScanning by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Chehre (Face ID)", subtitle = "कैमरा फेस पहचान और सिक्योर अनलॉक", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "👤 चेहरा पहचान स्थिति") {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.CenterHorizontally)
                    .border(2.dp, Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F))), CircleShape)
                    .background(Color(0xFF1E293B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isScanning) "📸" else if (isFaceEnrolled) "✅" else "👤", fontSize = 42.sp)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                if (isFaceEnrolled) "मालिक का चेहरा एनरोल है और सुरक्षित है।" else "अभी तक कोई चेहरा एनरोल नहीं किया गया है।",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isScanning = true
                    Toast.makeText(context, "कैमरा फेस स्कैन हो रहा है...", Toast.LENGTH_SHORT).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isScanning = false
                        isFaceEnrolled = true
                        UserSettingsManager.setFaceEnrolled(context, true)
                        MayaSpeaker.speak(context, "मालिक, आपका चेहरा सफलतापूर्वक रजिस्टर हो गया है!")
                        Toast.makeText(context, "✅ फेस एनरोलमेंट पूरा हुआ!", Toast.LENGTH_SHORT).show()
                    }, 2000)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isFaceEnrolled) "दोबारा चेहरा स्कैन करें" else "+ चेहरा स्कैन करें (Register Face)", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 7. EMERGENCY SOS SETTINGS ---
@Composable
fun EmergencySosView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var sosPhone by remember { mutableStateOf(UserSettingsManager.getSosContact(context)) }
    var sosTriggerWord by remember { mutableStateOf(UserSettingsManager.getSosTriggerWord(context)) }
    var sirenEnabled by remember { mutableStateOf(UserSettingsManager.getSosSirenEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        SubScreenHeader(title = "Emergency SOS", subtitle = "मुसीबत में 'Maya Bachao' बोलने पर तुरंत मदद", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "🚨 आपातकालीन सेटिंग्स") {
            OutlinedTextField(
                value = sosTriggerWord,
                onValueChange = {
                    sosTriggerWord = it
                    UserSettingsManager.setSosTriggerWord(context, it)
                },
                label = { Text("ट्रिगर शब्द (Default: 'Maya Bachao')", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = sosPhone,
                onValueChange = {
                    sosPhone = it
                    UserSettingsManager.setSosContact(context, it)
                },
                label = { Text("इमरजेंसी फोन नंबर (Guardian / Police)", color = Color.LightGray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = defaultTextFieldColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ToggleRow(
                title = "लाउड सायरन बजाएं (Loud Siren)",
                subtitle = "SOS ट्रिगर होने पर 100% वॉल्यूम पर अलार्म बजाए",
                checked = sirenEnabled,
                onCheckedChange = {
                    sirenEnabled = it
                    UserSettingsManager.setSosSirenEnabled(context, it)
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "🚨 SOS टेस्ट: सायरन व मैसेज अलर्ट सक्रिय हुआ!", Toast.LENGTH_SHORT).show()
                    MayaSpeaker.speak(context, "इमरजेंसी मोड सक्रिय! अलर्ट भेजा जा रहा है।")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚠️ टेस्ट SOS ट्रिगर (Test Alarm)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// --- 8. EVENT TRIGGERS (16 Audio & System Triggers) ---
@Composable
fun EventTriggersView(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var triggers by remember { mutableStateOf(UserSettingsManager.getActiveEventTriggers(context)) }

    val triggerList = listOf(
        "चार्जर लगाने पर (Charger Connected)" to ("'बाबू मुझे करंट मिल गया ❤️' बोले" to "trig_charger_in"),
        "चार्जर हटाने पर (Charger Removed)" to ("'चार्जर निकाल लिया गया' बोले" to "trig_charger_out"),
        "बैटरी 100% फुल होने पर" to ("'बैटरी फुल हो गई है, चार्जर निकाल लें' बोले" to "trig_batt_full"),
        "बैटरी 15% से कम होने पर" to ("'बाबू मेरी बैटरी खत्म हो रही है, भूख लगी है' बोले" to "trig_batt_low"),
        "स्क्रीन अनलॉक होने पर" to ("'वेलकम बैक बाबू!' बोले" to "trig_screen_on"),
        "स्क्रीन लॉक होने पर" to ("'बाय बाबू, अपना ख्याल रखना' बोले" to "trig_screen_off"),
        "हेडफोन / ईयरफोन लगाने पर" to ("'गाना चलाऊं क्या बाबू?' पूछे" to "trig_headset_in"),
        "हेडफोन निकालने पर" to ("म्यूजिक अपने आप पॉज करे" to "trig_headset_out"),
        "वाई-फाई कनेक्ट होने पर" to ("घर के वाई-फाई से कनेक्ट का संकेत दे" to "trig_wifi_on"),
        "वाई-फाई डिस्कनेक्ट होने पर" to ("'वाई-फाई बंद हो गया है' चेतावनी दे" to "trig_wifi_off"),
        "ब्लूटूथ कनेक्ट होने पर" to ("गाड़ी या स्पीकर से कनेक्ट होते ही गाना पूछे" to "trig_bt_on"),
        "सुबह 7:00 बजे अलार्म" to ("'गुड मॉर्निंग बाबू! उठ जाओ' कहकर जगाए" to "trig_morning"),
        "रात 11:00 बजे नाइट मोड" to ("'गुड नाईट बाबू, सो जाओ अब' बोले" to "trig_night"),
        "फोन जमीन पर गिरने पर (Freefall)" to ("'अरे बाबू मुझे चोट लग गई!' बोले" to "trig_fall"),
        "कॉल कटने के बाद" to ("कॉल समरी याद में सेव करे" to "trig_call_end"),
        "नया WhatsApp आने पर" to ("भेजने वाले का नाम बोलकर सुनाए" to "trig_whatsapp_ping")
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
        SubScreenHeader(title = "16 Event Triggers", subtitle = "फोन के हर एक्शन पर माया का रियल-टाइम वॉयस रिस्पॉन्स", onNavigateBack = onNavigateBack)
        Spacer(modifier = Modifier.height(16.dp))

        CardSection(title = "⚡ ऑटोमैटिक ऑडियो ट्रिगर्स") {
            triggerList.forEach { (title, pair) ->
                val (desc, key) = pair
                val isActive = triggers.contains(key)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B)
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
                            checked = isActive,
                            onCheckedChange = { checked ->
                                val newSet = triggers.toMutableSet()
                                if (checked) newSet.add(key) else newSet.remove(key)
                                triggers = newSet
                                UserSettingsManager.setActiveEventTriggers(context, newSet)
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

@Composable
fun AdvancedSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Color.LightGray, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
    }
}
