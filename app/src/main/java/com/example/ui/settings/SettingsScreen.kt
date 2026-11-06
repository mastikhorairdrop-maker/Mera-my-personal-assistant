package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.ZoyaForegroundService
import com.example.accessibility.MayaAccessibilityService
import com.example.guard.TouchGuardManager
import com.example.memory.MayaMemoryManager
import com.example.settings.PersonalityMode
import com.example.settings.UserSettingsManager

sealed class SettingsSubScreen {
    data object Hub : SettingsSubScreen()
    data object VoiceAndLanguage : SettingsSubScreen()
    data object LookAndFeel : SettingsSubScreen()
    data object PhoneControl : SettingsSubScreen()
    data object Connections : SettingsSubScreen()
    data object PrivacyAndAbout : SettingsSubScreen()
    data object PorcupineWakeWord : SettingsSubScreen()
    // Video Requested Sub-Screens:
    data object Personal : SettingsSubScreen()
    data object MayaAssistant : SettingsSubScreen()
    data object VoiceEngine : SettingsSubScreen()
    data object VoiceGuardian : SettingsSubScreen()
    data object SecretSettings : SettingsSubScreen()
    data object SkillsStore : SettingsSubScreen()
    data object SubAgents : SettingsSubScreen()
    data object EmailSettings : SettingsSubScreen()
    data object SocialMedia : SettingsSubScreen()
    data object BackupSettings : SettingsSubScreen()
    data object MayaRules : SettingsSubScreen()
    data object AdvancedHub : SettingsSubScreen()
    data object ThemeSettings : SettingsSubScreen()
    data object AppearanceSettings : SettingsSubScreen()
    data object BehaviourSettings : SettingsSubScreen()
    data object TypingSettings : SettingsSubScreen()
    data object ChehreFace : SettingsSubScreen()
    data object EmergencySos : SettingsSubScreen()
    data object EventTriggers : SettingsSubScreen()
}

@Composable
fun FullSettingsContainer(
    onDismiss: () -> Unit,
    onOpenMemories: () -> Unit
) {
    val context = LocalContext.current
    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen>(SettingsSubScreen.Hub) }

    BackHandler {
        when (currentSubScreen) {
            is SettingsSubScreen.Hub -> onDismiss()
            is SettingsSubScreen.ThemeSettings,
            is SettingsSubScreen.AppearanceSettings,
            is SettingsSubScreen.BehaviourSettings,
            is SettingsSubScreen.TypingSettings,
            is SettingsSubScreen.ChehreFace,
            is SettingsSubScreen.EmergencySos,
            is SettingsSubScreen.EventTriggers -> currentSubScreen = SettingsSubScreen.AdvancedHub
            else -> currentSubScreen = SettingsSubScreen.Hub
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090714))
    ) {
        when (currentSubScreen) {
            is SettingsSubScreen.Hub -> {
                SettingsHubView(
                    onNavigateBack = onDismiss,
                    onNavigateToVoiceAndLang = { currentSubScreen = SettingsSubScreen.VoiceAndLanguage },
                    onNavigateToMemories = onOpenMemories,
                    onNavigateToLookAndFeel = { currentSubScreen = SettingsSubScreen.LookAndFeel },
                    onNavigateToPhoneControl = { currentSubScreen = SettingsSubScreen.PhoneControl },
                    onNavigateToConnections = { currentSubScreen = SettingsSubScreen.Connections },
                    onNavigateToPrivacy = { currentSubScreen = SettingsSubScreen.PrivacyAndAbout },
                    onNavigateToPorcupine = { currentSubScreen = SettingsSubScreen.PorcupineWakeWord },
                    onNavigateToPersonal = { currentSubScreen = SettingsSubScreen.Personal },
                    onNavigateToMayaAssistant = { currentSubScreen = SettingsSubScreen.MayaAssistant },
                    onNavigateToVoiceEngine = { currentSubScreen = SettingsSubScreen.VoiceEngine },
                    onNavigateToVoiceGuardian = { currentSubScreen = SettingsSubScreen.VoiceGuardian },
                    onNavigateToSecret = { currentSubScreen = SettingsSubScreen.SecretSettings },
                    onNavigateToSkills = { currentSubScreen = SettingsSubScreen.SkillsStore },
                    onNavigateToSubAgents = { currentSubScreen = SettingsSubScreen.SubAgents },
                    onNavigateToEmail = { currentSubScreen = SettingsSubScreen.EmailSettings },
                    onNavigateToSocial = { currentSubScreen = SettingsSubScreen.SocialMedia },
                    onNavigateToBackup = { currentSubScreen = SettingsSubScreen.BackupSettings },
                    onNavigateToRules = { currentSubScreen = SettingsSubScreen.MayaRules },
                    onNavigateToAdvancedHub = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.VoiceAndLanguage -> {
                VoiceAndLanguageScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub },
                    onNavigateToPorcupine = { currentSubScreen = SettingsSubScreen.PorcupineWakeWord }
                )
            }
            is SettingsSubScreen.LookAndFeel -> {
                LookAndFeelScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.PhoneControl -> {
                PhoneControlScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.Connections -> {
                ConnectionsScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.PrivacyAndAbout -> {
                PrivacyAndAboutScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.PorcupineWakeWord -> {
                PorcupineWakeWordScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.Personal -> {
                PersonalSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.MayaAssistant -> {
                MayaAssistantSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.VoiceEngine -> {
                VoiceEngineView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.VoiceGuardian -> {
                VoiceGuardianScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.SecretSettings -> {
                SecretSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.SkillsStore -> {
                SkillsStoreView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.SubAgents -> {
                SubAgentsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.EmailSettings -> {
                EmailSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.SocialMedia -> {
                SocialMediaSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.BackupSettings -> {
                BackupSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.MayaRules -> {
                MayaRulesView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
                )
            }
            is SettingsSubScreen.AdvancedHub -> {
                AdvancedSettingsHubView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub },
                    onNavigateToVoiceGuardian = { currentSubScreen = SettingsSubScreen.VoiceGuardian },
                    onNavigateToTheme = { currentSubScreen = SettingsSubScreen.ThemeSettings },
                    onNavigateToAppearance = { currentSubScreen = SettingsSubScreen.AppearanceSettings },
                    onNavigateToBehaviour = { currentSubScreen = SettingsSubScreen.BehaviourSettings },
                    onNavigateToTyping = { currentSubScreen = SettingsSubScreen.TypingSettings },
                    onNavigateToFaceId = { currentSubScreen = SettingsSubScreen.ChehreFace },
                    onNavigateToSos = { currentSubScreen = SettingsSubScreen.EmergencySos },
                    onNavigateToEventTriggers = { currentSubScreen = SettingsSubScreen.EventTriggers }
                )
            }
            is SettingsSubScreen.ThemeSettings -> {
                ThemeSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.AppearanceSettings -> {
                AppearanceSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.BehaviourSettings -> {
                BehaviourSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.TypingSettings -> {
                TypingSettingsView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.ChehreFace -> {
                ChehreFaceView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.EmergencySos -> {
                EmergencySosView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
            is SettingsSubScreen.EventTriggers -> {
                EventTriggersView(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.AdvancedHub }
                )
            }
        }
    }
}

/**
 * SCREENSHOT 1: The Settings Hub Screen
 */
@Composable
fun SettingsHubView(
    onNavigateBack: () -> Unit,
    onNavigateToVoiceAndLang: () -> Unit,
    onNavigateToMemories: () -> Unit,
    onNavigateToLookAndFeel: () -> Unit,
    onNavigateToPhoneControl: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToPorcupine: () -> Unit = {},
    onNavigateToPersonal: () -> Unit = {},
    onNavigateToMayaAssistant: () -> Unit = {},
    onNavigateToVoiceEngine: () -> Unit = {},
    onNavigateToVoiceGuardian: () -> Unit = {},
    onNavigateToSecret: () -> Unit = {},
    onNavigateToSkills: () -> Unit = {},
    onNavigateToSubAgents: () -> Unit = {},
    onNavigateToEmail: () -> Unit = {},
    onNavigateToSocial: () -> Unit = {},
    onNavigateToBackup: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToAdvancedHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val voiceName = remember { UserSettingsManager.getVoicePersona(context) }
    val language = remember { UserSettingsManager.getLanguage(context) }
    val speechSpeed = remember { UserSettingsManager.getSpeechSpeed(context) }
    val savedMemoriesCount by MayaMemoryManager.memoriesFlow.collectAsState()
    val savedChatsCount by MayaMemoryManager.chatHistoryFlow.collectAsState()
    val isOwnerEnrolled = com.example.guardian.VoiceGuardianManager.isOwnerEnrolled(context)
    val isGuardianActive = com.example.guardian.VoiceGuardianManager.isGuardianEnabled(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Back Button & Title Header with Anime Maya Banner (matching Screenshot 1)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "MAYA AI",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Settings",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Complete Free & Unlocked AI Suite",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Glowing Anime Hero Banner Card (Screenshot 1 top-right art)
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 135.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF8A2BE2), Color(0xFF2A0845), Color(0xFF0D0819))
                        )
                    )
                    .border(1.5.dp, Color(0xFFC084FC).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(16.dp, CircleShape, spotColor = Color(0xFFFF007F))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFF007F), Color(0xFF7928CA), Color(0xFF00E5FF))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🌸",
                            fontSize = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "MAYA",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // SPECIAL FEATURE: VOICE GUARDIAN ("मालिक की आवाज़ पहचानने का फीचर")
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToVoiceGuardian() },
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF14172B),
            border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFFFF007F))))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(Brush.linearGradient(listOf(Color(0xFF00E5FF), Color(0xFF2563EB))), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Voice Guardian", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isGuardianActive) Color(0xFF10B981) else Color(0xFF64748B)
                        ) {
                            Text(
                                if (isGuardianActive) "ON ✓" else "OFF",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        if (isOwnerEnrolled) "मालिक की आवाज एनरोल है (Match test ready)" else "मालिक की आवाज रिकॉर्ड करें ताकि सिर्फ आपकी सुने",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.LightGray)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section: 👤 PERSONAL & ASSISTANT SETTINGS
        SectionHeader(title = "CORE SETTINGS", subtitle = "व्यक्तिगत और माया की मुख्य सेटिंग्स")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFF2563EB),
                    title = "Personal",
                    subtitle = "${UserSettingsManager.getUserName(context)} · ${UserSettingsManager.getUserGender(context)} · ${UserSettingsManager.getPreferredMusicApp(context)}",
                    badge = null,
                    onClick = onNavigateToPersonal
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Face,
                    iconBg = Color(0xFFFF007F),
                    title = "Maya",
                    subtitle = "12 Awaazein · Reels Auto-Scroll · Persona",
                    badge = voiceName,
                    onClick = onNavigateToMayaAssistant
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.GraphicEq,
                    iconBg = Color(0xFF7E22CE),
                    title = "Voice Engine",
                    subtitle = "${UserSettingsManager.getVoiceEngineMode(context)} · ${UserSettingsManager.getVoiceEngineModel(context)}",
                    badge = "Beta 🔥",
                    onClick = onNavigateToVoiceEngine
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Lock,
                    iconBg = Color(0xFFEF4444),
                    title = "Secret Settings",
                    subtitle = "Friendly · Romantic · Intense (18+ Private GF Mode)",
                    badge = UserSettingsManager.getSecretMode(context),
                    onClick = onNavigateToSecret
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: 📦 SKILLS, AGENTS & RULES
        SectionHeader(title = "INTELLIGENCE & SKILLS", subtitle = "टूल्स, सब-एजेंट्स और कस्टम रूल्स")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.Extension,
                    iconBg = Color(0xFF00E5FF),
                    title = "Skills Store",
                    subtitle = "8 Working Tools: WhatsApp, Insta, YouTube, Calls...",
                    badge = "8 Active",
                    onClick = onNavigateToSkills
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.SmartToy,
                    iconBg = Color(0xFF10B981),
                    title = "Sub-Agents",
                    subtitle = "Gemini 1.5, Claude 3.5, DeepSeek V3, Whisper",
                    badge = null,
                    onClick = onNavigateToSubAgents
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Rule,
                    iconBg = Color(0xFFF59E0B),
                    title = "Maya ke Rules",
                    subtitle = "+ नया नियम जोड़ें (Custom instructions for Maya)",
                    badge = null,
                    onClick = onNavigateToRules
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Section: ⚙️ ADVANCED & SYSTEM
        SectionHeader(title = "ADVANCED SYSTEM", subtitle = "थीम्स, फेस आईडी, SOS, 16 इवेंट ट्रिगर्स")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.SettingsSuggest,
                    iconBg = Color(0xFF8B5CF6),
                    title = "Advanced Settings Hub",
                    subtitle = "Themes · 3D Avatar · Face ID · SOS · 16 Event Triggers",
                    badge = "Deep Control",
                    onClick = onNavigateToAdvancedHub
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                val isPorcupineActive = UserSettingsManager.getPorcupineEnabled(context)
                val currentSensPercent = (UserSettingsManager.getPorcupineSensitivity(context) * 100).toInt()
                SettingsListItem(
                    icon = Icons.Default.Hearing,
                    iconBg = Color(0xFF2563EB),
                    title = "Porcupine Wake Word",
                    subtitle = if (isPorcupineActive) "Detection ON • Sensitivity: $currentSensPercent%" else "Detection OFF • Built-in acoustic fallback",
                    badge = if (isPorcupineActive) "$currentSensPercent%" else "OFF",
                    onClick = onNavigateToPorcupine
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Email,
                    iconBg = Color(0xFF3B82F6),
                    title = "Email (SMTP)",
                    subtitle = "आवाज से डायरेक्ट ईमेल भेजने की सेटिंग्स",
                    badge = null,
                    onClick = onNavigateToEmail
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Share,
                    iconBg = Color(0xFFEC4899),
                    title = "Social Media",
                    subtitle = "Instagram @handle, ऑटो-कैप्शन्स और स्टोरीज",
                    badge = null,
                    onClick = onNavigateToSocial
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.CloudSync,
                    iconBg = Color(0xFF06B6D4),
                    title = "Backup & Restore",
                    subtitle = "${savedChatsCount.size} चैट्स · ${savedMemoriesCount.size} यादें एक्सपोर्ट/रीस्टोर करें",
                    badge = null,
                    onClick = onNavigateToBackup
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card: Maya AI Info & About
        SettingsHeroCard(
            icon = Icons.Default.Shield,
            iconTint = Color(0xFFC084FC),
            iconBg = Color(0xFF381A66),
            title = "Privacy & About Maya AI",
            subtitle = "v2.8 Free Open-Source Edition • 100% On-Device & Safe\nNo Paid Licenses • No Trial Locks 🇮🇳",
            onClick = onNavigateToPrivacy
        )

        Spacer(modifier = Modifier.height(26.dp))
    }
}

/**
 * SCREENSHOT 2: Voice & Language Subscreen
 */
@Composable
fun VoiceAndLanguageScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPorcupine: () -> Unit = {}
) {
    val context = LocalContext.current

    var assistantName by remember { mutableStateOf(UserSettingsManager.getAssistantName(context)) }
    var userName by remember { mutableStateOf(UserSettingsManager.getUserName(context)) }
    var personality by remember { mutableStateOf(UserSettingsManager.getPersonality(context)) }
    var language by remember { mutableStateOf(UserSettingsManager.getLanguage(context)) }
    var responseStyle by remember { mutableStateOf(UserSettingsManager.getResponseStyle(context)) }

    var selectedVoice by remember { mutableStateOf(UserSettingsManager.getVoiceName(context)) }
    var voiceModel by remember { mutableStateOf(UserSettingsManager.getVoiceModel(context)) }
    var speechSpeed by remember { mutableFloatStateOf(UserSettingsManager.getSpeechSpeed(context)) }
    var voiceVolume by remember { mutableFloatStateOf(UserSettingsManager.getVoiceVolume(context)) }

    var listenOnOpen by remember { mutableStateOf(UserSettingsManager.getListenOnOpen(context)) }
    var wakeWordEnabled by remember { mutableStateOf(UserSettingsManager.getWakeWordEnabled(context)) }

    // Dialog state
    var showNameEditDialog by remember { mutableStateOf(false) }
    var showAssistantNameDialog by remember { mutableStateOf(false) }
    var showPersonalityDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResponseStyleDialog by remember { mutableStateOf(false) }
    var showPicovoiceDialog by remember { mutableStateOf(false) }
    var picovoiceKey by remember { mutableStateOf(UserSettingsManager.getPicoVoiceAccessKey(context)) }
    var picovoiceKeywordFile by remember { mutableStateOf(UserSettingsManager.getPicoVoiceKeywordFile(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Bar
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Voice & language",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // GROUP 1: Assistant & User Identity + Personality
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.AutoAwesome,
                    iconBg = Color(0xFF6B21A8),
                    title = "Assistant name",
                    subtitle = assistantName,
                    badge = null,
                    onClick = { showAssistantNameDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                // CRITICAL FIX: "Your name" editor with reliable persistence!
                SettingsListItem(
                    icon = Icons.Default.Person,
                    iconBg = Color(0xFF00B0FF),
                    title = "Your name (आपका नाम)",
                    subtitle = userName,
                    badge = "Edit",
                    onClick = { showNameEditDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Mood,
                    iconBg = Color(0xFF7E22CE),
                    title = "Personality",
                    subtitle = "${personality.hindiLabel} (${personality.title})",
                    badge = if (personality == PersonalityMode.TEACHER) "Active 🎓" else null,
                    onClick = { showPersonalityDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Language,
                    iconBg = Color(0xFF8B5CF6),
                    title = "Language",
                    subtitle = language,
                    badge = null,
                    onClick = { showLanguageDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.Tune,
                    iconBg = Color(0xFF7C3AED),
                    title = "Response style",
                    subtitle = responseStyle,
                    badge = null,
                    onClick = { showResponseStyleDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // SECTION: ✨ HOW SHE SOUNDS
        SectionHeader(title = "HOW SHE SOUNDS", subtitle = null)

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.RecordVoiceOver,
                    iconBg = Color(0xFF7E22CE),
                    title = "Voice",
                    subtitle = "$selectedVoice - Firm",
                    badge = null,
                    onClick = { showVoiceDialog = true }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.AutoAwesome,
                    iconBg = Color(0xFF9333EA),
                    title = "Voice model",
                    subtitle = voiceModel,
                    badge = null,
                    onClick = {
                        Toast.makeText(context, "Gemini 2.0 Flash Live is pre-selected for real-time speech", Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                // Speech Speed Slider
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF7E22CE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Speech speed", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(String.format("%.2fx", speechSpeed), color = Color(0xFFC084FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechSpeed,
                        onValueChange = {
                            speechSpeed = it
                            UserSettingsManager.setSpeechSpeed(context, it)
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC084FC),
                            activeTrackColor = Color(0xFF9333EA),
                            inactiveTrackColor = Color(0xFF2D1B4E)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                // Voice Volume Slider
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF6B21A8), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Voice volume", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("${(voiceVolume * 100).toInt()}%", color = Color(0xFFC084FC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = voiceVolume,
                        onValueChange = {
                            voiceVolume = it
                            UserSettingsManager.setVoiceVolume(context, it)
                        },
                        valueRange = 0.1f..1.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFC084FC),
                            activeTrackColor = Color(0xFF9333EA),
                            inactiveTrackColor = Color(0xFF2D1B4E)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // SECTION: ✨ LISTENING
        SectionHeader(title = "LISTENING", subtitle = null)

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                // Listen when the app opens
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF6B21A8), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Hearing, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Listen when the app opens", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Starts listening as soon as you open Maya", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = listenOnOpen,
                        onCheckedChange = {
                            listenOnOpen = it
                            UserSettingsManager.setListenOnOpen(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                // Background Wake Word
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF7E22CE), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Background Wake Word", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("Say 'Ok Maya' to activate even when phone is locked", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = wakeWordEnabled,
                        onCheckedChange = {
                            wakeWordEnabled = it
                            UserSettingsManager.setWakeWordEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))

                // Picovoice Porcupine Offline Engine Setup
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToPorcupine() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF2563EB), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙️", fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Offline 'OK Maya' Engine", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (picovoiceKey.isNotBlank()) Color(0xFF10B981) else Color(0xFF38BDF8),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (picovoiceKey.isNotBlank()) "PICOVOICE READY" else "SILENT ACOUSTIC",
                                        color = Color.Black,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (picovoiceKey.isNotBlank()) "Picovoice Porcupine active • 0% tring sound" else "Built-in silent acoustic detector active • Zero earcons",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
    }

    // DIALOG: PICOVOICE PORCUPINE SETUP (Offline "OK Maya" wake-word engine)
    if (showPicovoiceDialog) {
        var tempKey by remember { mutableStateOf(picovoiceKey) }
        var tempFile by remember { mutableStateOf(picovoiceKeywordFile) }
        AlertDialog(
            onDismissRequest = { showPicovoiceDialog = false },
            containerColor = Color(0xFF131126),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎙️", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Offline 'OK Maya' Setup", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Surface(
                        color = Color(0xFF0F766E).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "✅ 0% ट्रिलिंग आवाज • 0% बैटरी खर्च • 100% ऑफलाइन\nअब 'OK Maya' बैकग्राउंड में बिना किसी बीप या डिस्टर्बेंस के काम करता है।",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Picovoice AccessKey (Optional / Free):",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        placeholder = { Text("Picovoice console AccessKey...", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Keyword Model File:",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = tempFile,
                        onValueChange = { tempFile = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        placeholder = { Text("e.g. ok_maya_android.ppn", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "💡 नोट: यदि आप AccessKey नहीं डालते हैं, तो भी माया का 'साइलेंट एकॉस्टिक डिटेक्टर' बिना ट्रिलिंग बीप के काम करेगा।",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        picovoiceKey = tempKey.trim()
                        picovoiceKeywordFile = tempFile.trim()
                        UserSettingsManager.setPicoVoiceAccessKey(context, picovoiceKey)
                        UserSettingsManager.setPicoVoiceKeywordFile(context, picovoiceKeywordFile)
                        showPicovoiceDialog = false
                        Toast.makeText(context, "सेटिंग्स सेव हो गईं! 'OK Maya' एक्टिव है ❤️", Toast.LENGTH_SHORT).show()
                        ZoyaForegroundService.activeService?.wakeWordManager?.let { manager ->
                            manager.pauseListening()
                            manager.resumeListening()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("सेव करें (Save)", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicovoiceDialog = false }) {
                    Text("रद्द करें", color = Color.Gray)
                }
            }
        )
    }

    // DIALOG: EDIT YOUR NAME (Solves the yadav bug 100%)
    if (showNameEditDialog) {
        var tempName by remember { mutableStateOf(userName) }
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            containerColor = Color(0xFF1A1530),
            title = {
                Text("आपका नाम बदलें (Your Name)", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "माया आपको इसी नाम या 'बाबू' कहकर पुकारेगी। इसे कभी भी बदल सकते हैं:",
                        color = Color(0xFFC084FC),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                        ),
                        placeholder = { Text("अपना नाम लिखें...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Quick suggestion chips
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SuggestionChip(
                            onClick = { tempName = "बाबू" },
                            label = { Text("बाबू ❤️") }
                        )
                        SuggestionChip(
                            onClick = { tempName = "दोस्त" },
                            label = { Text("दोस्त") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = tempName.trim().ifEmpty { "बाबू" }
                        userName = finalName
                        UserSettingsManager.setUserName(context, finalName)
                        showNameEditDialog = false
                        Toast.makeText(context, "नाम सेव हो गया: $finalName ✅", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("सेव करें (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditDialog = false }) {
                    Text("रद्द करें", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // DIALOG: EDIT ASSISTANT NAME
    if (showAssistantNameDialog) {
        var tempAssistantName by remember { mutableStateOf(assistantName) }
        AlertDialog(
            onDismissRequest = { showAssistantNameDialog = false },
            containerColor = Color(0xFF1A1530),
            title = {
                Text("असिस्टेंट का नाम बदलें", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = tempAssistantName,
                    onValueChange = { tempAssistantName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFC084FC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = tempAssistantName.trim().ifEmpty { "Maya" }
                        assistantName = finalName
                        UserSettingsManager.setAssistantName(context, finalName)
                        showAssistantNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAssistantNameDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }

    // DIALOG: PICK PERSONALITY (Includes Teacher Mode requested by user!)
    if (showPersonalityDialog) {
        AlertDialog(
            onDismissRequest = { showPersonalityDialog = false },
            containerColor = Color(0xFF1A1530),
            title = {
                Text("माया का मूड और रोल चुनें (Personality)", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PersonalityMode.entries.forEach { mode ->
                        val isSelected = personality == mode
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF6B21A8) else Color(0xFF261D47)
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFC084FC)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    personality = mode
                                    UserSettingsManager.setPersonality(context, mode)
                                    showPersonalityDialog = false
                                    Toast.makeText(context, "${mode.hindiLabel} चालू हो गया!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        personality = mode
                                        UserSettingsManager.setPersonality(context, mode)
                                        showPersonalityDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(mode.hindiLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(mode.subtitle, color = Color(0xFFB3A8D6), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonalityDialog = false }) {
                    Text("Done", color = Color(0xFFC084FC))
                }
            }
        )
    }

    // DIALOG: VOICE SELECTION
    if (showVoiceDialog) {
        val voiceOptions = listOf(
            "Aoede" to "मीठी वूमेन आवाज़ (Default)",
            "Pari" to "परियों जैसी कोमल आवाज़",
            "Zoya" to "गर्मजोशी और प्यार भरी",
            "Kavya" to "स्पष्ट और मधुर आवाज़",
            "Kore" to "चुलबुली और नटखट",
            "Leda" to "शांत और सौम्य"
        )
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            containerColor = Color(0xFF1A1530),
            title = { Text("Choose Sweet Voice", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    voiceOptions.forEach { (vKey, vLabel) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoice = vKey
                                    UserSettingsManager.setVoiceName(context, vKey)
                                    UserSettingsManager.setVoicePersona(context, vKey)
                                    com.example.live.MayaSpeaker.applyVoiceSettings(context)
                                    com.example.live.MayaSpeaker.speak(context, "नमस्ते बाबू! कैसी लगी आपको यह प्यारी सी $vKey आवाज़?")
                                    showVoiceDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVoice.equals(vKey, ignoreCase = true),
                                onClick = {
                                    selectedVoice = vKey
                                    UserSettingsManager.setVoiceName(context, vKey)
                                    UserSettingsManager.setVoicePersona(context, vKey)
                                    com.example.live.MayaSpeaker.applyVoiceSettings(context)
                                    com.example.live.MayaSpeaker.speak(context, "नमस्ते बाबू! कैसी लगी आपको यह प्यारी सी $vKey आवाज़?")
                                    showVoiceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(vKey, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(vLabel, color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoiceDialog = false }) {
                    Text("Close", color = Color(0xFFC084FC))
                }
            }
        )
    }

    // DIALOG: LANGUAGE SELECTION
    if (showLanguageDialog) {
        val langs = listOf("Hindi", "Hinglish", "English")
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            containerColor = Color(0xFF1A1530),
            title = { Text("Choose Language", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    langs.forEach { l ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    language = l
                                    UserSettingsManager.setLanguage(context, l)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == l,
                                onClick = {
                                    language = l
                                    UserSettingsManager.setLanguage(context, l)
                                    showLanguageDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(l, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = Color(0xFFC084FC))
                }
            }
        )
    }

    // DIALOG: RESPONSE STYLE
    if (showResponseStyleDialog) {
        val styles = listOf("Balanced", "Fast & Crisp", "Detailed & In-Depth")
        AlertDialog(
            onDismissRequest = { showResponseStyleDialog = false },
            containerColor = Color(0xFF1A1530),
            title = { Text("Response Style", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    styles.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    responseStyle = s
                                    UserSettingsManager.setResponseStyle(context, s)
                                    showResponseStyleDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = responseStyle == s,
                                onClick = {
                                    responseStyle = s
                                    UserSettingsManager.setResponseStyle(context, s)
                                    showResponseStyleDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(s, color = Color.White, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResponseStyleDialog = false }) {
                    Text("Close", color = Color(0xFFC084FC))
                }
            }
        )
    }
}

/**
 * LOOK & FEEL SCREEN (With Edge Light Battery Saver Toggle!)
 */
@Composable
fun LookAndFeelScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var edgeLightOn by remember { mutableStateOf(UserSettingsManager.getEdgeLightEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Look & feel", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Battery Saver Edge Light Notice
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Border Neon Light (किनारे वाली लाइट)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            "बैटरी बचाने के लिए इसे बंद रखें। जब ऑन करेंगे तभी स्क्रीन किनारों पर नियॉन लाइट चमकेगी।",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = edgeLightOn,
                        onCheckedChange = {
                            edgeLightOn = it
                            UserSettingsManager.setEdgeLightEnabled(context, it)
                            Toast.makeText(
                                context,
                                if (it) "Edge Light On ✨" else "Edge Light Off (बैटरी सेवर एक्टिव) 🔋",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Assistant Visual Theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Dark Violet Cyberpunk (Default)", color = Color(0xFFC084FC), fontSize = 13.sp)
                Text("• 3D Glowing Energy Orb", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    }
}

/**
 * PHONE CONTROL SCREEN (Touch Guard, Screen Unlock, Accessibility, Announcements & System Permissions)
 */
@Composable
fun PhoneControlScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var isAccessibilityActive by remember { mutableStateOf(MayaAccessibilityService.isAccessibilityPermissionGranted(context)) }
    var isBatteryExempt by remember { mutableStateOf(ZoyaForegroundService.isBatteryOptimizationIgnored(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    var touchGuardActive by remember { mutableStateOf(TouchGuardManager.isTouchGuardActive) }
    var motionAlarmEnabled by remember { mutableStateOf(UserSettingsManager.getMotionAlarmEnabled(context)) }

    var autoUnlockEnabled by remember { mutableStateOf(UserSettingsManager.getAutoUnlockEnabled(context)) }
    var autoUnlockPin by remember { mutableStateOf(UserSettingsManager.getAutoUnlockPin(context)) }
    var isPinEditing by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf(autoUnlockPin) }

    var airplaneAlertsEnabled by remember { mutableStateOf(UserSettingsManager.getAirplaneAnnouncementEnabled(context)) }
    var chargingAlertsEnabled by remember { mutableStateOf(UserSettingsManager.getChargingAnnouncementEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Phone Control & Security", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("टच गार्ड, ऑटो अनलॉक और सिस्टम सेटिंग्स", color = Color(0xFFC084FC), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. TOUCH GUARD SECTION
        SectionHeader(title = "TOUCH GUARD & ANTI-THEFT", subtitle = "टच स्क्रीन लॉकर और मोशन अलार्म")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF381E72)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛡️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Full Touch Guard Mode", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text(
                            if (touchGuardActive) "सुरक्षा कवच सक्रिय है! पूरी स्क्रीन प्रोटेक्टेड है।" else "पूरी स्क्रीन पर टच ब्लॉक करें ताकि कोई फोन न छू सके",
                            color = if (touchGuardActive) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = touchGuardActive,
                        onCheckedChange = { enable ->
                            if (enable) {
                                val res = TouchGuardManager.enableTouchGuard(context)
                                touchGuardActive = TouchGuardManager.isTouchGuardActive
                                hasOverlayPermission = Settings.canDrawOverlays(context)
                                if (res == "OVERLAY_PERMISSION_REQUIRED" || !hasOverlayPermission) {
                                    Toast.makeText(context, "कृपया पहले 'Display over other apps' अनुमति दें!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                TouchGuardManager.disableTouchGuard(context)
                                touchGuardActive = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF22C55E),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Overlay Permission Status check
                if (!hasOverlayPermission) {
                    Surface(
                        color = Color(0xFF451A03),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("⚠️ Overlay Permission आवश्यक है", color = Color(0xFFFDBA74), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("टच गार्ड को काम करने के लिए 'Display over other apps' ऑन करें", color = Color(0xFFFED7AA), fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                    try { context.startActivity(intent) } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Grant", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Anti-Theft Motion Siren Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🚨 Anti-Theft Motion Alarm", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("फोन उठाने या हिलाने पर माया जोर से चेतावनी देगी", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = motionAlarmEnabled,
                        onCheckedChange = {
                            motionAlarmEnabled = it
                            UserSettingsManager.setMotionAlarmEnabled(context, it)
                            Toast.makeText(context, if (it) "मोशन अलार्म ऑन 🚨" else "मोशन अलार्म बंद", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. AUTO SCREEN UNLOCK WITH PIN
        SectionHeader(title = "AUTO SCREEN UNLOCK", subtitle = "आवाज से फोन अनलॉक और पिन सेटअप")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF381E72)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔓", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Voice Screen Unlock", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Text("वेक वर्ड 'Ok Maya Unlock Phone' बोलने पर लॉक स्क्रीन खोलना", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoUnlockEnabled,
                        onCheckedChange = {
                            autoUnlockEnabled = it
                            UserSettingsManager.setAutoUnlockEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                Text("Lock Screen PIN (सुरक्षित पिन कोड)", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "फोन का 4 या 6 डिजिट लॉक पिन दर्ज करें ताकि माया स्क्रीन पर ऑटो-टाइप कर सके।",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pinInputValue,
                        onValueChange = { if (it.length <= 8) pinInputValue = it },
                        placeholder = { Text("उदा. 1234", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFC084FC),
                            unfocusedBorderColor = Color(0xFF381E72)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            autoUnlockPin = pinInputValue
                            UserSettingsManager.setAutoUnlockPin(context, pinInputValue)
                            Toast.makeText(context, "पिन कोड सुरक्षित सेव हो गया! ✅", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                    ) {
                        Text("Save PIN")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test Unlock Button
                OutlinedButton(
                    onClick = {
                        if (!isAccessibilityActive) {
                            Toast.makeText(context, "कृपया पहले Accessibility Service ऑन करें!", Toast.LENGTH_LONG).show()
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } else {
                            val pin = autoUnlockPin.ifBlank { pinInputValue }
                            MayaAccessibilityService.unlockScreenDirectly(context, pin)
                            Toast.makeText(context, "ऑटो-अनलॉक कमांड निष्पादित की जा रही है...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    border = BorderStroke(1.dp, Color(0xFF9333EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Test Screen Unlock Now", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. SMART SYSTEM ANNOUNCEMENTS
        SectionHeader(title = "SMART SYSTEM ALERTS", subtitle = "सिस्टम और चार्जिंग वॉयस अलर्ट्स")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF381E72)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Airplane Mode Spoken Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✈️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Airplane Mode Spoken Alerts", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("फ्लाइट मोड लगने और हटने पर बोलकर सूचित करना", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = airplaneAlertsEnabled,
                        onCheckedChange = {
                            airplaneAlertsEnabled = it
                            UserSettingsManager.setAirplaneAnnouncementEnabled(context, it)
                            Toast.makeText(context, if (it) "फ्लाइट मोड अलर्ट ऑन ✈️" else "अलर्ट बंद", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Smart Charging & Full Battery Spoken Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔋", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Battery & Charging Assistant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("चार्जर लगाने पर बैटरी % बताना और 100% फुल होने पर अलार्म देना", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = chargingAlertsEnabled,
                        onCheckedChange = {
                            chargingAlertsEnabled = it
                            UserSettingsManager.setChargingAnnouncementEnabled(context, it)
                            Toast.makeText(context, if (it) "चार्जिंग असिस्टेंट ऑन 🔋" else "असिस्टेंट बंद", Toast.LENGTH_SHORT).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFA855F7),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. ESSENTIAL PERMISSIONS & SERVICE ENGINES
        SectionHeader(title = "CORE PERMISSIONS & STABILITY", subtitle = "फोन कंट्रोल और बैकग्राउंड परमिशन")
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Accessibility Service
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Accessibility Service", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (isAccessibilityActive) "Accessibility Enabled: WhatsApp auto-send & Screen Unlock ready ✅" else "WhatsApp auto-send और स्क्रीन कंट्रोल के लिए आवश्यक",
                            color = if (isAccessibilityActive) Color(0xFF4ADE80) else Color(0xFFFB923C),
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isAccessibilityActive) Color(0xFF15803D) else Color(0xFF7E22CE))
                    ) {
                        Text(if (isAccessibilityActive) "Active" else "Enable")
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Battery Saver Exemption
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Saver Exemption", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("एंड्रॉइड को बैकग्राउंड में वेक वर्ड बंद करने से रोकता है", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    if (!isBatteryExempt) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                        ) {
                            Text("Exempt")
                        }
                    } else {
                        Text("Done ✅", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Display Over Other Apps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Display Over Other Apps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("टच गार्ड और फ्लोटिंग ऑर्ब के लिए परमिशन", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasOverlayPermission) Color(0xFF15803D) else Color(0xFF7E22CE))
                    ) {
                        Text(if (hasOverlayPermission) "Allowed" else "Manage")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * CONNECTIONS SCREEN (Gemini Keys)
 */
@Composable
fun ConnectionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val legacyPrefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }
    var keyInput by remember {
        mutableStateOf(
            legacyPrefs.getString("api_key", "") ?: BuildConfig.GEMINI_API_KEY.takeIf { it != "MY_GEMINI_API_KEY" } ?: ""
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Connections", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gemini API Key", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Live Voice & Intelligence model connectivity", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFC084FC),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    ),
                    placeholder = { Text("AIzaSy...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        legacyPrefs.edit().putString("api_key", keyInput.trim()).apply()
                        Toast.makeText(context, "API Key Saved Successfully! ✅", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save API Key")
                }
            }
        }
    }
}

/**
 * PRIVACY & ABOUT SCREEN
 */
@Composable
fun PrivacyAndAboutScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF1E1738), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text("Privacy & about", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Maya AI Assistant v2.0 (New Video Build)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Made in India 🇮🇳", color = Color(0xFFC084FC), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Maya is designed with complete privacy. Your conversations, memories, and voice data remain strictly within your session. You can delete or clear your memories at any time from Chats & memory.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }
    }
}

// UI HELPER COMPONENTS

@Composable
fun SectionHeader(title: String, subtitle: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✨", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = Color(0xFF64748B),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SettingsHeroCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
        border = BorderStroke(1.dp, Color(0xFF281E4B)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(iconBg, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(text = title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) {
                        Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun SettingsListItem(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String?,
    badge: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (badge != null) {
                Surface(
                    color = Color(0xFF0F766E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = badge,
                        color = Color(0xFF2DD4BF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * PORCUPINE WAKE WORD SETTINGS SCREEN
 * Dedicated screen for toggling Porcupine wake word detection,
 * configuring sensitivity levels (0.0 to 1.0), setting keyword models,
 * Picovoice AccessKeys, and live audio trigger testing.
 */
@Composable
fun PorcupineWakeWordScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var isPorcupineEnabled by remember { mutableStateOf(UserSettingsManager.getPorcupineEnabled(context)) }
    var sensitivity by remember { mutableFloatStateOf(UserSettingsManager.getPorcupineSensitivity(context)) }
    var accessKey by remember { mutableStateOf(UserSettingsManager.getPicoVoiceAccessKey(context)) }
    var keywordFile by remember { mutableStateOf(UserSettingsManager.getPicoVoiceKeywordFile(context)) }

    var isKeyHidden by remember { mutableStateOf(true) }
    var keyInputValue by remember { mutableStateOf(accessKey) }
    var isTestListening by remember { mutableStateOf(false) }
    var testTriggeredText by remember { mutableStateOf<String?>(null) }

    val currentPercent = (sensitivity * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Navigation Header
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
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Porcupine Wake Word",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "वेक वर्ड डिटेक्शन और सेंसिटिविटी कंट्रोल",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. MASTER WAKE WORD TOGGLE CARD
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13112A)),
            border = BorderStroke(1.5.dp, if (isPorcupineEnabled) Color(0xFF2563EB) else Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (isPorcupineEnabled) Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF7C3AED)))
                                    else Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF1F2937))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎙️", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Porcupine Wake Word",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isPorcupineEnabled) "Active on-device • 0% Battery Drain" else "Engine turned off • Standby",
                                color = if (isPorcupineEnabled) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isPorcupineEnabled,
                        onCheckedChange = { checked ->
                            isPorcupineEnabled = checked
                            UserSettingsManager.setPorcupineEnabled(context, checked)
                            ZoyaForegroundService.reloadWakeWordEngine()
                            Toast.makeText(
                                context,
                                if (checked) "Porcupine Wake Word Enabled ✅" else "Porcupine Disabled (Offline Acoustic Fallback Active)",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Engine Technology Badge
                Surface(
                    color = if (isPorcupineEnabled && accessKey.isNotBlank()) Color(0xFF065F46) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPorcupineEnabled && accessKey.isNotBlank()) "⚡ PICOVOICE NEURAL ENGINE ACTIVE"
                            else if (isPorcupineEnabled) "ℹ️ ACCESS KEY REQUIRED (RUNNING ACOUSTIC DETECTOR)"
                            else "💤 STANDBY (WAKE WORD DETECTION DISABLED)",
                            color = if (isPorcupineEnabled && accessKey.isNotBlank()) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. DETECTION SENSITIVITY SLIDER & PRESETS (Core Requirement)
        SectionHeader(
            title = "DETECTION SENSITIVITY",
            subtitle = "माइक्रोफ़ोन सेंसिटिविटी लेवल एडजस्ट करें"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header with current sensitivity badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Sensitivity Level", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("Default: 0.70 (70%)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    Surface(
                        color = when {
                            sensitivity >= 0.80f -> Color(0xFF7C3AED)
                            sensitivity >= 0.50f -> Color(0xFF2563EB)
                            else -> Color(0xFF0F766E)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = String.format("%.2f (%d%%)", sensitivity, currentPercent),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The Interactive Slider
                Slider(
                    value = sensitivity,
                    onValueChange = { newVal ->
                        sensitivity = newVal
                        UserSettingsManager.setPorcupineSensitivity(context, newVal)
                    },
                    onValueChangeFinished = {
                        UserSettingsManager.setPorcupineSensitivity(context, sensitivity)
                        ZoyaForegroundService.reloadWakeWordEngine()
                    },
                    valueRange = 0.0f..1.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF38BDF8),
                        activeTrackColor = Color(0xFF2563EB),
                        inactiveTrackColor = Color(0xFF231C3D)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Dynamic Sensitivity Explanation based on value
                Surface(
                    color = Color(0xFF1B1638),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when {
                                    sensitivity < 0.40f -> "🔉 कम सेंसिटिविटी (Low Sensitivity)"
                                    sensitivity in 0.40f..0.75f -> "🎯 संतुलित स्तर (Balanced Sensitivity)"
                                    else -> "🔊 उच्च सेंसिटिविटी (High Sensitivity)"
                                },
                                color = Color(0xFFC084FC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                sensitivity < 0.40f ->
                                    "शोरगुल वाले कमरे या गाड़ी में अनचाहे ट्रिगर को रोकता है। पास आकर स्पष्ट आवाज़ में 'Ok Maya' बोलना होगा।"
                                sensitivity in 0.40f..0.75f ->
                                    "दैनिक उपयोग के लिए सबसे अनुशंसित (Recommended)। 1 से 3 मीटर की सामान्य दूरी से आवाज़ आसानी से पहचान लेता है।"
                                else ->
                                    "हल्की फुसफुसाहट (whispers) या दूर कमरे से भी ट्रिगर कर सकता है। शांत वातावरण के लिए सर्वोत्तम है।"
                            },
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1-Tap Preset Sensitivity Chips
                Text("Quick Preset Levels (एक-टैप प्रीसेट):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        Pair("0.35", 0.35f),
                        Pair("0.50", 0.50f),
                        Pair("0.70 ⭐", 0.70f),
                        Pair("0.85", 0.85f),
                        Pair("0.95", 0.95f)
                    )
                    presets.forEach { (label, value) ->
                        val isSelected = Math.abs(sensitivity - value) < 0.03f
                        Surface(
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF1E1738),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    sensitivity = value
                                    UserSettingsManager.setPorcupineSensitivity(context, value)
                                    ZoyaForegroundService.reloadWakeWordEngine()
                                    Toast.makeText(context, "सेंसिटिविटी $label सेट हो गई!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. KEYWORD MODEL & PICOVOICE ACCESS KEY CARD
        SectionHeader(
            title = "PICOVOICE CONFIGURATION",
            subtitle = "कस्टम कीवर्ड मॉडल और एक्सेस की"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Keyword File Selection
                Text("Keyword Model (कीवर्ड मॉडल):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                val keywordOptions = listOf(
                    Pair("ok_maya_android.ppn", "🎯 'OK Maya' Custom Model (Trained for Maya)"),
                    Pair("JARVIS", "🤖 Built-in 'Jarvis' Fallback"),
                    Pair("COMPUTER", "💻 Built-in 'Computer' Fallback")
                )

                keywordOptions.forEach { (fileKey, label) ->
                    val isChosen = keywordFile.equals(fileKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                keywordFile = fileKey
                                UserSettingsManager.setPicoVoiceKeywordFile(context, fileKey)
                                ZoyaForegroundService.reloadWakeWordEngine()
                                Toast.makeText(context, "मॉडल: $fileKey चुना गया", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isChosen,
                            onClick = {
                                keywordFile = fileKey
                                UserSettingsManager.setPicoVoiceKeywordFile(context, fileKey)
                                ZoyaForegroundService.reloadWakeWordEngine()
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = if (isChosen) Color.White else Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // AccessKey Input
                Text("Picovoice Console AccessKey:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Picovoice Console से फ्री लाइफटाइम एक्सेस की दर्ज करें:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = keyInputValue,
                    onValueChange = { keyInputValue = it },
                    placeholder = { Text("Paste your Picovoice AccessKey here...", color = Color.Gray, fontSize = 12.sp) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { isKeyHidden = !isKeyHidden }) {
                            Text(if (isKeyHidden) "👁️" else "🙈", fontSize = 14.sp)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF281E4B)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://console.picovoice.ai/")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        border = BorderStroke(1.dp, Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Get Free Key (console.picovoice.ai)", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            accessKey = keyInputValue.trim()
                            UserSettingsManager.setPicoVoiceAccessKey(context, accessKey)
                            ZoyaForegroundService.reloadWakeWordEngine()
                            Toast.makeText(context, "AccessKey सेव हो गई! Porcupine चालू हो गया ✅", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Save Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. INTERACTIVE LIVE MIC & TRIGGER TEST
        SectionHeader(
            title = "LIVE TRIGGER TEST",
            subtitle = "माइक्रोफ़ोन और ट्रिगर रिस्पॉन्स टेस्ट करें"
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "बोलें: 'Ok Maya' या 'Jarvis'",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "चयनित सेंसिटिविटी (${currentPercent}%) पर माइक्रोफ़ोन से वेक वर्ड ट्रिगर टेस्ट करें।",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Soundwave Feedback Box
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF2563EB).copy(alpha = 0.4f), Color(0xFF1E1738))
                            ),
                            CircleShape
                        )
                        .border(1.5.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 32.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Apply & Reload Button
                Button(
                    onClick = {
                        UserSettingsManager.setPorcupineEnabled(context, isPorcupineEnabled)
                        UserSettingsManager.setPorcupineSensitivity(context, sensitivity)
                        ZoyaForegroundService.reloadWakeWordEngine()
                        Toast.makeText(
                            context,
                            "वेक वर्ड इंजन रीलोड हो गया! सेंसिटिविटी: $currentPercent%",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚡ Apply & Reload Wake Word Engine",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
