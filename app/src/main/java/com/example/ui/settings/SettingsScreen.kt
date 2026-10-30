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
}

@Composable
fun FullSettingsContainer(
    onDismiss: () -> Unit,
    onOpenMemories: () -> Unit
) {
    val context = LocalContext.current
    var currentSubScreen by remember { mutableStateOf<SettingsSubScreen>(SettingsSubScreen.Hub) }

    BackHandler {
        if (currentSubScreen != SettingsSubScreen.Hub) {
            currentSubScreen = SettingsSubScreen.Hub
        } else {
            onDismiss()
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
                    onNavigateToPrivacy = { currentSubScreen = SettingsSubScreen.PrivacyAndAbout }
                )
            }
            is SettingsSubScreen.VoiceAndLanguage -> {
                VoiceAndLanguageScreen(
                    onNavigateBack = { currentSubScreen = SettingsSubScreen.Hub }
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
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val voiceName = remember { UserSettingsManager.getVoiceName(context) }
    val language = remember { UserSettingsManager.getLanguage(context) }
    val speechSpeed = remember { UserSettingsManager.getSpeechSpeed(context) }
    val savedMemoriesCount by MayaMemoryManager.memoriesFlow.collectAsState()
    val savedChatsCount by MayaMemoryManager.chatHistoryFlow.collectAsState()

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
                    text = "Customize your AI experience",
                    color = Color(0xFFB3A8D6),
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

        // Card 1: Maya AI Info
        SettingsHeroCard(
            icon = Icons.Default.KeyboardArrowUp,
            iconTint = Color(0xFFC084FC),
            iconBg = Color(0xFF381A66),
            title = "Maya AI",
            subtitle = "Your Personal AI Assistant\nv2.0 (New Video Build)\nMade in India 🇮🇳",
            onClick = onNavigateToPrivacy
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: Join Community
        SettingsHeroCard(
            icon = Icons.AutoMirrored.Filled.Send,
            iconTint = Color(0xFFFF4081),
            iconBg = Color(0xFF52133B),
            title = "Join community",
            subtitle = null,
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/"))
                try { context.startActivity(intent) } catch (_: Exception) {
                    Toast.makeText(context, "Community link: Telegram / AI Studio", Toast.LENGTH_SHORT).show()
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card 3: Watch Setup Video
        SettingsHeroCard(
            icon = Icons.Default.PlayArrow,
            iconTint = Color.White,
            iconBg = Color(0xFFE50914),
            title = "Watch the setup video",
            subtitle = "API keys, permissions and Accessibility, step by step",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
                try { context.startActivity(intent) } catch (_: Exception) {}
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Section: ✨ AI SETTINGS
        SectionHeader(title = "AI SETTINGS", subtitle = "Configure your AI assistant")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                SettingsListItem(
                    icon = Icons.Default.Key,
                    iconBg = Color(0xFF6B21A8),
                    title = "Connections",
                    subtitle = "Gemini and Groq keys",
                    badge = "Ready",
                    onClick = onNavigateToConnections
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Default.RecordVoiceOver,
                    iconBg = Color(0xFF7E22CE),
                    title = "Voice & language",
                    subtitle = "$voiceName · $language · ${speechSpeed}x",
                    badge = null,
                    onClick = onNavigateToVoiceAndLang
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsListItem(
                    icon = Icons.Outlined.ChatBubble,
                    iconBg = Color(0xFF9333EA),
                    title = "Chats & memory",
                    subtitle = "${savedChatsCount.size} saved chats · ${savedMemoriesCount.size} yaadein",
                    badge = null,
                    onClick = onNavigateToMemories
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: ⚙️ APP SETTINGS
        SectionHeader(title = "APP SETTINGS", subtitle = "Appearance and behavior")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                val edgeLightOn = UserSettingsManager.getEdgeLightEnabled(context)
                SettingsListItem(
                    icon = Icons.Default.Palette,
                    iconBg = Color(0xFF6B21A8),
                    title = "Look & feel",
                    subtitle = if (edgeLightOn) "System · Classic orb · Edge light ON" else "System · Classic orb · Battery saver (Edge light OFF)",
                    badge = null,
                    onClick = onNavigateToLookAndFeel
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(horizontal = 16.dp))
                val isAcc = MayaAccessibilityService.isServiceRunning()
                SettingsListItem(
                    icon = Icons.Default.PhoneAndroid,
                    iconBg = Color(0xFF7E22CE),
                    title = "Phone control",
                    subtitle = if (isAcc) "100% Control Active ✅" else "Quick actions and permissions",
                    badge = if (isAcc) "Active" else null,
                    onClick = onNavigateToPhoneControl
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Section: ℹ️ ABOUT
        SectionHeader(title = "ABOUT", subtitle = "App information and support")

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsListItem(
                icon = Icons.Default.Shield,
                iconBg = Color(0xFF581C87),
                title = "Privacy & about",
                subtitle = "Your data, version, support",
                badge = null,
                onClick = onNavigateToPrivacy
            )
        }

        Spacer(modifier = Modifier.height(26.dp))
    }
}

/**
 * SCREENSHOT 2: Voice & Language Subscreen
 */
@Composable
fun VoiceAndLanguageScreen(onNavigateBack: () -> Unit) {
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
            }
        }

        Spacer(modifier = Modifier.height(26.dp))
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
        val voiceOptions = listOf("Kore", "Aoede", "Puck", "Charon", "Fenrir")
        AlertDialog(
            onDismissRequest = { showVoiceDialog = false },
            containerColor = Color(0xFF1A1530),
            title = { Text("Choose Voice", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    voiceOptions.forEach { v ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedVoice = v
                                    UserSettingsManager.setVoiceName(context, v)
                                    showVoiceDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedVoice == v,
                                onClick = {
                                    selectedVoice = v
                                    UserSettingsManager.setVoiceName(context, v)
                                    showVoiceDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFC084FC))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(v, color = Color.White, fontSize = 15.sp)
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
 * PHONE CONTROL SCREEN (Accessibility, permissions & shortcuts)
 */
@Composable
fun PhoneControlScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val isAccessibilityActive = MayaAccessibilityService.isAccessibilityPermissionGranted(context)
    val isBatteryExempt = remember { ZoyaForegroundService.isBatteryOptimizationIgnored(context) }

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
            Text("Phone control", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                        Text("Accessibility Service", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            if (isAccessibilityActive) "Accessibility Enabled: 100% Phone Control Ready ✅" else "Turn on Accessibility for WhatsApp auto-send and clicks",
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7E22CE))
                    ) {
                        Text(if (isAccessibilityActive) "Open" else "Enable")
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Battery Saver Exemption", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Prevents Android from killing background listening", color = Color(0xFF94A3B8), fontSize = 12.sp)
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
            }
        }
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
