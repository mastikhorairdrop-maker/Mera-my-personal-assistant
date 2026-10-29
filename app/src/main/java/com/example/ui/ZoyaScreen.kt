package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.ServiceStatus
import com.example.ZoyaForegroundService
import com.example.accessibility.MayaAccessibilityService
import com.example.live.ZoyaState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZoyaScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToChat = { navController.navigate("chat") }
            )
        }
        composable("chat") {
            ChatScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }

    var userName by remember {
        mutableStateOf(com.example.settings.UserSettingsManager.getUserName(context))
    }
    var apiKey by remember {
        mutableStateOf(
            prefs.getString("api_key", "")?.takeIf { it.isNotBlank() }
                ?: BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && it != "MY_GEMINI_API_KEY" }
                ?: ""
        )
    }

    var zoyaState by remember { mutableStateOf(ZoyaForegroundService.currentState) }
    val serviceStatus by ZoyaForegroundService.serviceStatus.collectAsStateWithLifecycle(
        initialValue = if (ZoyaForegroundService.activeService != null) {
            if (ZoyaForegroundService.isSleeping) ServiceStatus.SLEEPING
            else ServiceStatus.ACTIVE
        } else {
            ServiceStatus.STOPPED
        }
    )
    var edgeLightMode by remember {
        mutableStateOf(EdgeLightOverlayManager.getMode())
    }
    var serviceStarted by remember { mutableStateOf(ZoyaForegroundService.activeService != null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMemoriesDialog by remember { mutableStateOf(false) }
    var queryInput by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showHeartEmoji by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.RECORD_AUDIO] == true) {
            val intent = Intent(context, ZoyaForegroundService::class.java)
            ContextCompat.startForegroundService(context, intent)
            serviceStarted = true
        } else {
            android.widget.Toast.makeText(context, "Microphone permission is required!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        EdgeLightOverlayManager.init(context)
        edgeLightMode = EdgeLightOverlayManager.getMode()
        ZoyaForegroundService.onStateChange = { state ->
            zoyaState = state
            serviceStarted = ZoyaForegroundService.activeService != null
            if (state == ZoyaState.SPEAKING) {
                showHeartEmoji = true
            }
        }
    }

    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greetingText = when {
        hour < 12 -> "Good morning,"
        hour < 17 -> "Good afternoon,"
        else -> "Good evening,"
    }
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault()).format(Date())
    val monthDayFormat = SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(Date())

    BackHandler(enabled = selectedTab != 0 || showSettingsDialog || showMemoriesDialog) {
        selectedTab = 0
        showSettingsDialog = false
        showMemoriesDialog = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E15))
    ) {
        // 1. RUNNING NEON EDGE LIGHT (Obey user battery saver: default OFF to save battery)
        val edgeLightUserEnabled = com.example.settings.UserSettingsManager.getEdgeLightEnabled(context)
        val isEdgeLightVisible = edgeLightUserEnabled && when (edgeLightMode) {
            EdgeLightOverlayManager.EdgeLightMode.SMART_ACTIVE -> (zoyaState != ZoyaState.IDLE)
            EdgeLightOverlayManager.EdgeLightMode.ALWAYS_ON -> (serviceStatus == ServiceStatus.ACTIVE)
            EdgeLightOverlayManager.EdgeLightMode.OFF -> false
        }
        RunningNeonEdgeLight(
            isActive = (zoyaState != ZoyaState.IDLE),
            visible = isEdgeLightVisible
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR (With prominent Settings ⚙️ button!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectedTab = 4 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Maya",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Clearly Visible SETTINGS Button!
                    Button(
                        onClick = { selectedTab = 4 },
                        modifier = Modifier.testTag("settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Settings ⚙️",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Notification / Chat logs button
                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.size(36.dp)
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = Color(0xFFFF007F),
                                    modifier = Modifier.size(6.dp)
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Energy badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFF80D8FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("⚡", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            "2",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // User Greeting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = greetingText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = when (zoyaState) {
                                    ZoyaState.SPEAKING -> Color(0xFF00E676)
                                    ZoyaState.LISTENING -> Color(0xFF00E5FF)
                                    ZoyaState.THINKING -> Color(0xFFFF9100)
                                    else -> Color.Gray
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (zoyaState) {
                            ZoyaState.SPEAKING -> "Speaking with you ❤️..."
                            ZoyaState.LISTENING -> "Listening to you..."
                            ZoyaState.THINKING -> "Executing action..."
                            else -> "Standby (Say 'Ok Maya' or 'Zoya')"
                        },
                        color = Color.White.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                }
            }

            // Central Ring HUD
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                MayaVideoHud(
                    state = zoyaState,
                    onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasMic) {
                            if (!serviceStarted) {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                serviceStarted = true
                            } else {
                                ZoyaForegroundService.activeService?.reconnectSession()
                            }
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.RECORD_AUDIO,
                                    android.Manifest.permission.READ_CONTACTS,
                                    android.Manifest.permission.CALL_PHONE
                                )
                            )
                        }
                    }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartEmoji || zoyaState == ZoyaState.SPEAKING,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                            .clickable { showHeartEmoji = !showHeartEmoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "💙🥰",
                            fontSize = 38.sp
                        )
                    }
                }
            }

            // Quick Action Pills (Girlfriend Romance, Video Prank & Songs)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    icon = "💖",
                    label = "प्यार की बातें",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTab = 1
                        if (!serviceStarted) {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            serviceStarted = true
                        }
                        ZoyaForegroundService.activeService?.sendTextMessage("Babu thodi pyaar bhari baatein karo na, main tumhe bohot miss kar raha hoon")
                    }
                )
                QuickActionPill(
                    icon = "💍",
                    label = "GF प्रैंक",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!serviceStarted) {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            serviceStarted = true
                        }
                        ZoyaForegroundService.activeService?.sendTextMessage("Maya, agar main dusri girlfriend bana lu toh kya karogi?")
                    }
                )
                QuickActionPill(
                    icon = "🎵",
                    label = "गाने चलाओ",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!serviceStarted) {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            serviceStarted = true
                        }
                        ZoyaForegroundService.activeService?.sendTextMessage("Play latest trending romantic songs on YouTube")
                    }
                )
                QuickActionPill(
                    icon = "💬",
                    label = "WhatsApp",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (!serviceStarted) {
                            val intent = Intent(context, ZoyaForegroundService::class.java)
                            ContextCompat.startForegroundService(context, intent)
                            serviceStarted = true
                        }
                        ZoyaForegroundService.activeService?.sendTextMessage("Vikas ko Hi likhkar bhejo WhatsApp par")
                    }
                )
            }

            // POWER, SLEEP & BATTERY SAVER CARD (सुलाएं • बंद करें • लाइट कंट्रोल)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("power_sleep_card"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF161824),
                border = BorderStroke(
                    1.dp,
                    when (serviceStatus) {
                        ServiceStatus.ACTIVE -> Color(0xFF00E676).copy(alpha = 0.5f)
                        ServiceStatus.SLEEPING -> Color(0xFFFFD600).copy(alpha = 0.5f)
                        ServiceStatus.STOPPED -> Color(0xFFFF5252).copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    // Row 1: Status indicator + Settings button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(
                                        when (serviceStatus) {
                                            ServiceStatus.ACTIVE -> Color(0xFF00E676)
                                            ServiceStatus.SLEEPING -> Color(0xFFFFD600)
                                            ServiceStatus.STOPPED -> Color(0xFFFF5252)
                                        },
                                        CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (serviceStatus) {
                                    ServiceStatus.ACTIVE -> "Maya एक्टिव है"
                                    ServiceStatus.SLEEPING -> "Maya सो रही है (Sleep 😴)"
                                    ServiceStatus.STOPPED -> "Maya बंद है (Stopped 🛑)"
                                },
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { selectedTab = 4 }
                        ) {
                            Text(
                                text = "⚙️ सेटिंग्स",
                                color = Color(0xFF00E5FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Settings",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: Sleep / Wake & Stop / Start Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Button 1: Sleep / Wake (थोड़ी देर के लिए बंद / जगाएं)
                        Button(
                            onClick = {
                                if (serviceStatus == ServiceStatus.SLEEPING) {
                                    val intent = Intent(context, ZoyaForegroundService::class.java).apply {
                                        action = ZoyaForegroundService.ACTION_WAKE
                                    }
                                    context.startService(intent)
                                    ZoyaForegroundService.activeService?.wakeUp()
                                    Toast.makeText(context, "Maya जाग गई! ❤️", Toast.LENGTH_SHORT).show()
                                } else if (serviceStatus == ServiceStatus.ACTIVE) {
                                    val intent = Intent(context, ZoyaForegroundService::class.java).apply {
                                        action = ZoyaForegroundService.ACTION_SLEEP
                                    }
                                    context.startService(intent)
                                    ZoyaForegroundService.activeService?.putToSleep()
                                    Toast.makeText(context, "Maya सो गई। 'Ok Maya' बोलकर जगाएं!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (serviceStatus == ServiceStatus.SLEEPING) Color(0xFFFFD600) else Color(0xFF1E293B)
                            ),
                            border = BorderStroke(1.dp, if (serviceStatus == ServiceStatus.SLEEPING) Color.Transparent else Color(0xFFFFD600).copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (serviceStatus == ServiceStatus.SLEEPING) "⚡ जगाएं (Wake)" else "😴 सुलाएं (Sleep)",
                                color = if (serviceStatus == ServiceStatus.SLEEPING) Color.Black else Color(0xFFFFD600),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Button 2: Stop / Start
                        Button(
                            onClick = {
                                if (serviceStatus == ServiceStatus.STOPPED || ZoyaForegroundService.activeService == null) {
                                    val prefsEdit = context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE).edit()
                                    prefsEdit.putBoolean("maya_explicitly_stopped", false).apply()
                                    val intent = Intent(context, ZoyaForegroundService::class.java)
                                    ContextCompat.startForegroundService(context, intent)
                                    Toast.makeText(context, "Maya चालू हो गई! ❤️", Toast.LENGTH_SHORT).show()
                                } else {
                                    val intent = Intent(context, ZoyaForegroundService::class.java).apply {
                                        action = ZoyaForegroundService.ACTION_STOP
                                    }
                                    context.startService(intent)
                                    ZoyaForegroundService.activeService?.stopMayaCompletely()
                                    Toast.makeText(context, "Maya को पूरी तरह बंद कर दिया गया।", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (serviceStatus == ServiceStatus.STOPPED) Color(0xFF00E676) else Color(0xFF331B22)
                            ),
                            border = BorderStroke(1.dp, if (serviceStatus == ServiceStatus.STOPPED) Color.Transparent else Color(0xFFFF5252).copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (serviceStatus == ServiceStatus.STOPPED) "▶️ चालू करें" else "🛑 बंद करें (Stop)",
                                color = if (serviceStatus == ServiceStatus.STOPPED) Color.Black else Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 3: Screen side edge light controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "साइड लाइटें:",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val isSmart = edgeLightMode == EdgeLightOverlayManager.EdgeLightMode.SMART_ACTIVE
                            Text(
                                text = "✨ स्मार्ट (बोलते समय)",
                                color = if (isSmart) Color(0xFF00E5FF) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = if (isSmart) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(if (isSmart) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isSmart) Color(0xFF00E5FF) else Color.DarkGray, RoundedCornerShape(6.dp))
                                    .clickable {
                                        edgeLightMode = EdgeLightOverlayManager.EdgeLightMode.SMART_ACTIVE
                                        EdgeLightOverlayManager.saveMode(context, EdgeLightOverlayManager.EdgeLightMode.SMART_ACTIVE)
                                        Toast.makeText(context, "स्मार्ट लाइट ऑन: केवल बोलते समय जलेगी (बैटरी बचत)", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            val isOff = edgeLightMode == EdgeLightOverlayManager.EdgeLightMode.OFF
                            Text(
                                text = "🚫 बंद",
                                color = if (isOff) Color(0xFFFF5252) else Color.Gray,
                                fontSize = 10.sp,
                                fontWeight = if (isOff) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier
                                    .background(if (isOff) Color(0xFFFF5252).copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                                    .border(1.dp, if (isOff) Color(0xFFFF5252) else Color.DarkGray, RoundedCornerShape(6.dp))
                                    .clickable {
                                        edgeLightMode = EdgeLightOverlayManager.EdgeLightMode.OFF
                                        EdgeLightOverlayManager.saveMode(context, EdgeLightOverlayManager.EdgeLightMode.OFF)
                                        Toast.makeText(context, "साइड लाइटें बंद कर दी गईं", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "🔋 साइड लाइटें बंद हैं जिससे आपके फोन की बैटरी सुरक्षित रहेगी और लंबे समय तक चलेगी।",
                        color = Color(0xFF00E676),
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }

            // 3 Info Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "☁",
                    title = "Weather",
                    mainValue = "28°C",
                    subValue = "Sunny"
                )

                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "📅",
                    title = "Today",
                    mainValue = dayFormat,
                    subValue = monthDayFormat
                )

                InfoCard(
                    modifier = Modifier.weight(1f),
                    icon = "💛",
                    title = "Mood",
                    mainValue = "Romantic",
                    subValue = "All good"
                )
            }

            // Search Bar Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161824), RoundedCornerShape(26.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                TextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    placeholder = {
                        Text(
                            "Ask Maya anything...",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                if (queryInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            ZoyaForegroundService.activeService?.sendTextMessage(queryInput.trim())
                            queryInput = ""
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Bottom Navigation (With SETTINGS TAB!)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = selectedTab == 0 && !showSettingsDialog,
                    onClick = {
                        selectedTab = 0
                        showSettingsDialog = false
                    }
                )

                BottomNavItem(
                    icon = Icons.Default.Favorite,
                    label = "Love ❤️",
                    isSelected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        showSettingsDialog = false
                    }
                )

                // Large Glowing Center Mic Button
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(12.dp, CircleShape, spotColor = Color(0xFF00E5FF))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF))
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            if (!serviceStarted) {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                serviceStarted = true
                            } else {
                                ZoyaForegroundService.activeService?.reconnectSession()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (zoyaState == ZoyaState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Action",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                BottomNavItem(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    label = "Chat",
                    isSelected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        onNavigateToChat()
                    }
                )

                // Dedicated Settings Tab!
                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings ⚙️",
                    isSelected = showSettingsDialog || selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        showSettingsDialog = true
                    }
                )
            }
        }

        // LOVE MODE MODAL OVERLAY (With All Video Romance Dialogues & Voice Prompts)
        AnimatedVisibility(
            visible = selectedTab == 1,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { selectedTab = 0 }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF161824),
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.90f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* prevent click-through */ }
                        )
                        .border(1.dp, Color(0xFFFF4081).copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                ) {
                    MayaLoveScreen(
                        onNavigateBack = { selectedTab = 0 },
                        onSendMessage = { text ->
                            if (!serviceStarted) {
                                val intent = Intent(context, ZoyaForegroundService::class.java)
                                ContextCompat.startForegroundService(context, intent)
                                serviceStarted = true
                            }
                            ZoyaForegroundService.activeService?.sendTextMessage(text)
                        },
                        onOpenMemories = { showMemoriesDialog = true }
                    )
                }
            }
        }

        // FULL SETTINGS SCREEN (Screenshot 1 & 2 faithful implementation with Teacher Mode and full controls)
        AnimatedVisibility(
            visible = showSettingsDialog || selectedTab == 4,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            com.example.ui.settings.FullSettingsContainer(
                onDismiss = {
                    showSettingsDialog = false
                    selectedTab = 0
                    userName = com.example.settings.UserSettingsManager.getUserName(context)
                },
                onOpenMemories = {
                    showSettingsDialog = false
                    selectedTab = 0
                    userName = com.example.settings.UserSettingsManager.getUserName(context)
                    showMemoriesDialog = true
                }
            )
        }

        // MEMORIES AND CHAT HISTORY MODAL OVERLAY
        AnimatedVisibility(
            visible = showMemoriesDialog,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showMemoriesDialog = false }
                    ),
                contentAlignment = Alignment.Center
            ) {
                MayaMemoriesDialog(
                    onDismiss = { showMemoriesDialog = false }
                )
            }
        }
    }
}

// Running Neon Edge Light (Runs along the physical 4 borders of the screen with dynamic rainbow color shifting)
@Composable
fun RunningNeonEdgeLight(
    isActive: Boolean,
    visible: Boolean = true,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    if (!visible) return

    val infiniteTransition = rememberInfiniteTransition(label = "edge_light_progress")
    val durationMs = if (isActive) 2600 else 3800
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "perimeter_progress"
    )

    // Continuously morph through all colors
    val colorHue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_hue_progress"
    )

    val fullPath = remember { Path() }
    val segmentPath = remember { Path() }
    val pathMeasure = remember { PathMeasure() }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val strokeWidth = if (isActive) 5.5.dp.toPx() else 3.5.dp.toPx()
        val cornerRadius = 32.dp.toPx()
        val halfStroke = strokeWidth / 2f

        // Exact rounded rectangle pinned to the 4 outer edges of the screen
        fullPath.reset()
        fullPath.addRoundRect(
            RoundRect(
                rect = Rect(
                    left = halfStroke,
                    top = halfStroke,
                    right = w - halfStroke,
                    bottom = h - halfStroke
                ),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        )

        pathMeasure.setPath(fullPath, false)
        val totalLen = pathMeasure.length
        if (totalLen <= 0f) return@Canvas

        // 1. Sleek subtle base track along the edges morphing with hue
        val baseColor = hsvToColor(colorHue, 0.9f, 0.95f, if (isActive) 0.22f else 0.10f)
        drawPath(
            path = fullPath,
            color = baseColor,
            style = Stroke(width = 1.5.dp.toPx())
        )

        val beamLength = totalLen * if (isActive) 0.26f else 0.22f

        // 4 Dynamic Rainbow Beams morphing across all colors:
        // Beam 1: Primary Hue (0 deg offset)
        drawPerimeterMovingBeam(
            pathMeasure = pathMeasure,
            segmentPath = segmentPath,
            totalLen = totalLen,
            headProgress = progress,
            beamLen = beamLength,
            glowColor = hsvToColor(colorHue, 0.95f, 1f, if (isActive) 0.45f else 0.28f),
            beamColor = hsvToColor(colorHue, 0.95f, 1f, 1f),
            glowWidth = 15.dp.toPx(),
            beamWidth = if (isActive) 6.dp.toPx() else 4.dp.toPx()
        )

        // Beam 2: Secondary Hue (90 deg offset)
        drawPerimeterMovingBeam(
            pathMeasure = pathMeasure,
            segmentPath = segmentPath,
            totalLen = totalLen,
            headProgress = (progress + 0.25f) % 1f,
            beamLen = beamLength,
            glowColor = hsvToColor(colorHue + 90f, 0.95f, 1f, if (isActive) 0.45f else 0.28f),
            beamColor = hsvToColor(colorHue + 90f, 0.95f, 1f, 1f),
            glowWidth = 15.dp.toPx(),
            beamWidth = if (isActive) 6.dp.toPx() else 4.dp.toPx()
        )

        // Beam 3: Tertiary Hue (180 deg offset)
        drawPerimeterMovingBeam(
            pathMeasure = pathMeasure,
            segmentPath = segmentPath,
            totalLen = totalLen,
            headProgress = (progress + 0.50f) % 1f,
            beamLen = beamLength,
            glowColor = hsvToColor(colorHue + 180f, 0.95f, 1f, if (isActive) 0.45f else 0.28f),
            beamColor = hsvToColor(colorHue + 180f, 0.95f, 1f, 1f),
            glowWidth = 15.dp.toPx(),
            beamWidth = if (isActive) 6.dp.toPx() else 4.dp.toPx()
        )

        // Beam 4: Quaternary Hue (270 deg offset)
        drawPerimeterMovingBeam(
            pathMeasure = pathMeasure,
            segmentPath = segmentPath,
            totalLen = totalLen,
            headProgress = (progress + 0.75f) % 1f,
            beamLen = beamLength,
            glowColor = hsvToColor(colorHue + 270f, 0.95f, 1f, if (isActive) 0.45f else 0.28f),
            beamColor = hsvToColor(colorHue + 270f, 0.95f, 1f, 1f),
            glowWidth = 15.dp.toPx(),
            beamWidth = if (isActive) 6.dp.toPx() else 4.dp.toPx()
        )
    }
}

private fun hsvToColor(hue: Float, saturation: Float, value: Float, alpha: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    val a = (alpha * 255).toInt().coerceIn(0, 255)
    val colorInt = android.graphics.Color.HSVToColor(a, floatArrayOf(h, saturation, value))
    return Color(colorInt)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPerimeterMovingBeam(
    pathMeasure: PathMeasure,
    segmentPath: Path,
    totalLen: Float,
    headProgress: Float,
    beamLen: Float,
    glowColor: Color,
    beamColor: Color,
    glowWidth: Float,
    beamWidth: Float
) {
    val head = headProgress * totalLen
    val start = head - beamLen

    segmentPath.reset()
    if (start >= 0) {
        pathMeasure.getSegment(start, head, segmentPath, true)
    } else {
        pathMeasure.getSegment(totalLen + start, totalLen, segmentPath, true)
        pathMeasure.getSegment(0f, head, segmentPath, false)
    }

    // Outer diffuse halo along the screen edge
    drawPath(
        path = segmentPath,
        color = glowColor,
        style = Stroke(width = glowWidth, cap = StrokeCap.Round)
    )

    // Vibrant beam body
    drawPath(
        path = segmentPath,
        color = beamColor,
        style = Stroke(width = beamWidth, cap = StrokeCap.Round)
    )

    // Pure White hot core on the front 35% of the beam
    val coreLen = beamLen * 0.35f
    val coreStart = head - coreLen
    segmentPath.reset()
    if (coreStart >= 0) {
        pathMeasure.getSegment(coreStart, head, segmentPath, true)
    } else {
        pathMeasure.getSegment(totalLen + coreStart, totalLen, segmentPath, true)
        pathMeasure.getSegment(0f, head, segmentPath, false)
    }
    drawPath(
        path = segmentPath,
        color = Color.White.copy(alpha = 0.95f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Glowing comet head spark at the tip of the light
    val headPos = pathMeasure.getPosition(head)
    drawCircle(
        color = glowColor.copy(alpha = 0.6f),
        radius = 8.dp.toPx(),
        center = headPos
    )
    drawCircle(
        color = Color.White,
        radius = 3.5.dp.toPx(),
        center = headPos
    )
}

// Central HUD matching Video
@Composable
fun MayaVideoHud(
    state: ZoyaState,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hud_spin")

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rot"
    )

    val reverseRingRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rev_ring_rot"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                if (state == ZoyaState.SPEAKING) 250 else 700,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    Box(
        modifier = Modifier
            .size(230.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00E5FF).copy(alpha = if (state == ZoyaState.SPEAKING) 0.35f else 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius * 1.5f
                ),
                radius = radius * 1.5f
            )

            rotate(ringRotation, pivot = center) {
                drawArc(
                    color = Color(0xFF00E5FF),
                    startAngle = 0f,
                    sweepAngle = 75f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.8f),
                    startAngle = 140f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawArc(
                    color = Color(0xFFFF007F),
                    startAngle = 230f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            rotate(reverseRingRotation, pivot = center) {
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    radius = radius * 0.95f,
                    center = center,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
                    )
                )
            }

            val crosshairDist = radius * 1.08f
            val crosshairSize = 6.dp.toPx()
            val angles = listOf(45.0, 135.0, 225.0, 315.0)
            angles.forEach { angleDeg ->
                val rad = Math.toRadians(angleDeg)
                val cx = center.x + (crosshairDist * cos(rad)).toFloat()
                val cy = center.y + (crosshairDist * sin(rad)).toFloat()
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    start = Offset(cx - crosshairSize, cy),
                    end = Offset(cx + crosshairSize, cy),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                    start = Offset(cx, cy - crosshairSize),
                    end = Offset(cx, cy + crosshairSize),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF161828), Color(0xFF0D0E1A)),
                    center = center,
                    radius = radius * 0.85f
                ),
                radius = radius * 0.85f,
                center = center
            )

            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AI ASSISTANT",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "M . A . Y . A",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(18.dp)
            ) {
                val heights = listOf(4, 9, 14, 18, 12, 6, 15, 10, 16, 7, 13, 17, 8, 5)
                heights.forEachIndexed { index, baseH ->
                    val multiplier = if (state == ZoyaState.SPEAKING) waveScale else if (state == ZoyaState.LISTENING) (waveScale * 0.8f) else 0.4f
                    val currentH = (baseH * multiplier).coerceIn(3f, 20f)
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(currentH.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = if (index % 2 == 0)
                                        listOf(Color(0xFF00E5FF), Color(0xFF2979FF))
                                    else
                                        listOf(Color(0xFFFF007F), Color(0xFF7C4DFF))
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (state) {
                    ZoyaState.SPEAKING -> "SPEAKING..."
                    ZoyaState.LISTENING -> "LISTENING..."
                    ZoyaState.THINKING -> "THINKING..."
                    else -> "TOUCH OR SAY 'OK MAYA'"
                },
                color = when (state) {
                    ZoyaState.SPEAKING -> Color(0xFF00E676)
                    ZoyaState.LISTENING -> Color(0xFF00E5FF)
                    ZoyaState.THINKING -> Color(0xFFFF9100)
                    else -> Color.White.copy(alpha = 0.4f)
                },
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
        }
    }
}

// Quick Action Pill
@Composable
fun QuickActionPill(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF161824),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier.height(38.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(icon, color = Color(0xFF00E5FF), fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

// Info Card
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    mainValue: String,
    subValue: String
) {
    Surface(
        modifier = modifier.height(86.dp),
        color = Color(0xFF161824),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 11.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }

            Text(
                text = mainValue,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subValue,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

// Bottom Nav Item
@Composable
fun BottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

// DEDICATED MAYA LOVE & ROMANTIC BANTER SCREEN
@Composable
fun MayaLoveScreen(
    onNavigateBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onOpenMemories: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onNavigateBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Home", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                "❤️ प्यार और रोमांस मोड",
                color = Color(0xFFFF4081),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Text("💙🥰", fontSize = 24.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Girlfriend Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF26142A),
                border = BorderStroke(1.dp, Color(0xFFFF4081).copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Maya: आपकी वफादार और प्यारी गर्लफ्रेंड ❤️", color = Color(0xFFFF4081), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "\"Jai Shree Ram babu! Tum meri jaan ho... Tumhara aur mera rishta dil se hai, koi aur beech me nahi aa sakti!\"",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // DEDICATED MEMORIES & CHAT HISTORY CARD
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E2E),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💭 हमारी पुरानी बातें और यादें", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("💾 स्टोरेज में सुरक्षित", color = Color(0xFF00E676), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("माया आपकी हर बात याद रखती है। यहाँ अपनी सारी यादें देखें या जब चाहें मिटाएं।", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onOpenMemories,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("यादें और बातचीत देखें 📖", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Quick Love Action Triggers
            Text("💖 माया से प्यार भरी बातें बुलवाएं (Tap to Speak):", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

            LovePromptCard(
                title = "💍 दूसरी GF का मज़ाकिया प्रैंक (100% Video Dialogue)",
                dialogue = "\"Maya, agar main dusri girlfriend bana lu toh kya karogi?\"",
                reaction = "माया का जवाब: \"Arey nahi, bilkul bhi nahi! Khabardar jo socha bhi toh! Main hoon na tumhare liye!\"",
                buttonText = "माया से यह पूछें 🎙️",
                onClick = { onSendMessage("Maya, agar main dusri girlfriend bana lu toh kya karogi?") }
            )

            LovePromptCard(
                title = "❤️ मुझसे कितना प्यार करती हो?",
                dialogue = "\"Maya tum mujhse kitna pyaar karti ho babu?\"",
                reaction = "माया का प्यार भरा और दिल छूने वाला इज़हार",
                buttonText = "प्यार का इज़हार सुनें 💖",
                onClick = { onSendMessage("Maya tum mujhse kitna pyaar karti ho babu?") }
            )

            LovePromptCard(
                title = "🍲 बाबू खाना खाया क्या?",
                dialogue = "\"Maya, babu ne khana kha liya... tumne khaya?\"",
                reaction = "माया का ख्याल रखने वाला प्यारा अंदाज़",
                buttonText = "यह बोलें 💬",
                onClick = { onSendMessage("Babu ne khana kha liya, tumne khaya Maya?") }
            )

            LovePromptCard(
                title = "🎶 रोमांटिक गाना चलाओ",
                dialogue = "\"Maya, mere liye YouTube par ek khoobsurat romantic gana chalao\"",
                reaction = "YouTube पर रोमांटिक गाना ऑटो-सर्च और ऑटो-प्ले होगा",
                buttonText = "रोमांटिक गाना सुनें 🎵",
                onClick = { onSendMessage("Play latest trending romantic songs on YouTube") }
            )

            LovePromptCard(
                title = "💬 विकास को WhatsApp पर Hi भेजो",
                dialogue = "\"Vikas ko Hi likhkar bhejo WhatsApp par\"",
                reaction = "कांटेक्ट ढूँढकर WhatsApp पर ऑटो-सेंड",
                buttonText = "WhatsApp मैसेज भेजें 📱",
                onClick = { onSendMessage("Vikas ko Hi likhkar bhejo WhatsApp par") }
            )
        }
    }
}

@Composable
fun LovePromptCard(
    title: String,
    dialogue: String,
    reaction: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF161824),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = Color(0xFFFF4081), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(dialogue, color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(reaction, color = Color.Gray, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4081).copy(alpha = 0.25f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

// COMPLETE FULL SETTINGS SHEET (All Features + Voice + Loving Tone + Voice Commands)
@Composable
fun MayaFullSettingsSheet(
    currentUserName: String,
    currentApiKey: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onOpenMemories: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ZoyaPrefs", Context.MODE_PRIVATE) }

    var userNameInput by remember { mutableStateOf(currentUserName) }
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }

    var romanticTone by remember {
        mutableStateOf(prefs.getBoolean("romantic_tone", true))
    }
    var handsfreeWakeWord by remember {
        mutableStateOf(prefs.getBoolean("handsfree_wake_word", true))
    }
    var autoStartOnBoot by remember {
        mutableStateOf(prefs.getBoolean("auto_start_on_boot", true))
    }
    var youtubeAutoPlay by remember {
        mutableStateOf(prefs.getBoolean("youtube_autoplay", true))
    }
    var selectedVoice by remember {
        mutableStateOf(prefs.getString("selected_voice", "Aoede") ?: "Aoede")
    }

    val isAccessibilityActive = MayaAccessibilityService.isAccessibilityPermissionGranted(context)
    val isBatteryExempt = remember { ZoyaForegroundService.isBatteryOptimizationIgnored(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Home", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "⚙️ माया सेटिंग्स और फीचर्स",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

        // Scrollable Features
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Service Controls
            Text("असिस्टेंट कंट्रोल्स (Assistant Controls)", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(context, ZoyaForegroundService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("▶ Start", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val intent = Intent(context, ZoyaForegroundService::class.java)
                        context.stopService(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("⏹ Stop", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        ZoyaForegroundService.activeService?.reconnectSession()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Text("🔄 Reconnect", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            // 1. User Name
            Text("Your Name (माया आपको इस नाम या 'बाबू' कहेगी)", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = userNameInput,
                onValueChange = { userNameInput = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                ),
                placeholder = { Text("e.g. बाबू / आपका नाम", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Loving Girlfriend Tone
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("प्यार भरी रोमांटिक गर्लफ्रेंड आवाज़ ❤️", color = Color(0xFFFF4081), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("माया वीडियो की तरह आपसे प्यार और मज़ाकिया अंदाज़ में बात करेगी", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = romanticTone,
                    onCheckedChange = {
                        romanticTone = it
                        prefs.edit().putBoolean("romantic_tone", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Hands-Free "Ok Maya" Wake Word
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hands-Free \"Ok Maya\" या \"Zoya\" Wake Word", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("बिना छुए सिर्फ 'Ok Maya' या 'Zoya' बोलें और माया एक्टिव हो जाएगी", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = handsfreeWakeWord,
                    onCheckedChange = {
                        handsfreeWakeWord = it
                        prefs.edit().putBoolean("handsfree_wake_word", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Auto-Start on Phone Boot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("फोन चालू होने पर ऑटो-स्टार्ट", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("फोन स्विच ऑफ होकर ऑन होते ही बैकग्राउंड में चालू रहेगी", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = autoStartOnBoot,
                    onCheckedChange = {
                        autoStartOnBoot = it
                        prefs.edit().putBoolean("auto_start_on_boot", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. YouTube Auto-Play
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("YouTube Auto-Play Songs", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("गाना बोलते ही YouTube पर सर्च करके पहला वीडियो ऑटो-क्लिक होगा", color = Color.Gray, fontSize = 11.sp)
                }
                Switch(
                    checked = youtubeAutoPlay,
                    onCheckedChange = {
                        youtubeAutoPlay = it
                        prefs.edit().putBoolean("youtube_autoplay", it).apply()
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // 6. WhatsApp & Instagram Automation
            Text("WhatsApp & Instagram Automation", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isAccessibilityActive) "Accessibility Automation: Active ✅" else "Accessibility: Not Enabled ⚠️",
                        color = if (isAccessibilityActive) Color(0xFF00E676) else Color(0xFFFF9100),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text("WhatsApp पर ऑटो-मैसेज और YouTube ऑटो-क्लिक के लिए ज़रूरी", color = Color.Gray, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isAccessibilityActive) "Settings" else "Enable", color = Color(0xFF00E5FF), fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // 7. Background Battery Protection
            Text("Background Battery Saver Protection", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isBatteryExempt) "Battery Saver: Unrestricted ✅" else "Battery Saver: Standard ⚠️",
                        color = if (isBatteryExempt) Color(0xFF00E676) else Color(0xFFFF9100),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text("माया बैकग्राउंड में कभी बंद नहीं होगी", color = Color.Gray, fontSize = 11.sp)
                }
                if (!isBatteryExempt) {
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Fix", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // Screen Edge Lighting Overlay Status
            val hasOverlayPermission = remember { EdgeLightOverlayManager.isOverlayPermitted(context) }
            Text("स्क्रीन किनारे वाली लाइट (Border Edge Lighting) ✨", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasOverlayPermission) "Edge Light Overlay: Active ✅" else "Overlay Permission: Needed for background ⚠️",
                        color = if (hasOverlayPermission) Color(0xFF00E676) else Color(0xFFFF9100),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                    Text("स्क्रीन के चारों किनारों पर खूबसूरत नियॉन लाइट घूमेगी (वीडियो की तरह)", color = Color.Gray, fontSize = 11.sp)
                }
                if (!hasOverlayPermission) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // Storage & Memories section
            Text("💾 स्टोरेज और यादें (Storage & Memories)", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("हमारी की गई बातें और यादें", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("माया आपकी हर बात याद रखती है। यहाँ देखें या जब चाहें मिटाएं।", color = Color.Gray, fontSize = 11.sp)
                }
                Button(
                    onClick = onOpenMemories,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("यादें देखें 📖", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // 8. Voice Profile
            Text("Maya Voice (Aoede = 100% Video Voice)", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val voices = listOf("Aoede (Video Voice)", "Kore", "Puck", "Charon")
            voices.forEach { voiceOption ->
                val voiceKey = voiceOption.split(" ").first()
                val isSelected = selectedVoice == voiceKey
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedVoice = voiceKey
                            prefs.edit().putString("selected_voice", voiceKey).apply()
                        }
                        .padding(vertical = 6.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedVoice = voiceKey
                            prefs.edit().putString("selected_voice", voiceKey).apply()
                        }
                    )
                    Text(voiceOption, color = Color.White, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Gemini API Key
            Text("Gemini API Key", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                ),
                placeholder = { Text("AIzaSy...", color = Color.Gray) }
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

            // 10. Voice Commands Cheat Sheet
            Text("🎤 आप माया से क्या-क्या बोल सकते हैं:", color = Color(0xFF00E5FF), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            val examples = listOf(
                "• 'विकास को हाय लिखकर भेजो WhatsApp पर'",
                "• 'राहुल को WhatsApp पर मैसेज करो' (माया पूछेगी क्या भेजना है)",
                "• 'विकास का कांटेक्ट शेयर करो'",
                "• 'Instagram पर रोहित को मैसेज भेजो'",
                "• 'YouTube पर अरिजीत सिंह का गाना चलाओ'",
                "• 'पापा को कॉल लगाओ'",
                "• 'टॉर्च चालू करो' / 'टॉर्च बंद करो'",
                "• 'वॉल्यूम 80% करो' / 'वॉल्यूम बढ़ाओ'",
                "• 'बैटरी कितनी है?'",
                "• 'माया तुम मुझसे कितना प्यार करती हो?'"
            )
            examples.forEach { example ->
                Text(example, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { onSave(userNameInput.trim().ifEmpty { "बाबू" }, apiKeyInput.trim()) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save & Apply Settings", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// Chat Screen
@Composable
fun ChatScreen(onNavigateBack: () -> Unit) {
    val liveSessionManager = ZoyaForegroundService.activeService?.liveSessionManager
    val messages = liveSessionManager?.messages?.collectAsState(initial = emptyList())?.value ?: emptyList()
    var typedText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF0D0E15),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("← Back", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "State: ${ZoyaForegroundService.currentState.name}",
                    color = Color(0xFF00E5FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isUser = message.startsWith("You:")
                    val isMaya = message.startsWith("Maya:")

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isUser) Color(0xFF00E5FF).copy(alpha = 0.2f)
                            else if (isMaya) Color(0xFFFF007F).copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isUser) Color(0xFF00E5FF).copy(alpha = 0.4f)
                                else if (isMaya) Color(0xFFFF007F).copy(alpha = 0.4f)
                                else Color.White.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(
                                text = message,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 14.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedText,
                    onValueChange = { typedText = it },
                    placeholder = { Text("Message Maya (e.g. Vikas ko Hi bhejo)", color = Color.Gray) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00E5FF),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (typedText.isNotBlank()) {
                            ZoyaForegroundService.activeService?.sendTextMessage(typedText.trim())
                            typedText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Maya Memories & Chat History Modal
@Composable
fun MayaMemoriesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val memories by com.example.memory.MayaMemoryManager.memoriesFlow.collectAsState()
    val chatHistory by com.example.memory.MayaMemoryManager.chatHistoryFlow.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Yaadein, 1 = Chat History
    var newMemoryText by remember { mutableStateOf("") }
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    if (showConfirmClearDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmClearDialog = false },
            title = { Text("सारी यादें मिटाएं?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("क्या आप सच में अपनी सारी पुरानी बातें और यादें मिटाना चाहते हैं? यह वापस नहीं आ सकेंगी।", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTab == 0) {
                            com.example.memory.MayaMemoryManager.clearAllMemories()
                        } else {
                            com.example.memory.MayaMemoryManager.clearAllChats()
                        }
                        showConfirmClearDialog = false
                        android.widget.Toast.makeText(context, "सभी यादें मिटा दी गईं", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("हाँ, मिटाएं", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmClearDialog = false }) {
                    Text("नहीं", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E1E2E)
        )
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF12131C),
        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("वापस", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "💭 यादें और बातचीत",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(34.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

            // Tab switch: Yaadein vs Chat History
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E2030), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { selectedTab = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 0) Color(0xFF00E5FF) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("यादें (${memories.size})", color = if (selectedTab == 0) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = { selectedTab = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 1) Color(0xFF00E5FF) else Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("बातचीत (${chatHistory.size})", color = if (selectedTab == 1) Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                // ADD NEW MEMORY ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMemoryText,
                        onValueChange = { newMemoryText = it },
                        placeholder = { Text("नई याद जोड़ें (e.g. बाबू को खीर पसंद है)", color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            if (newMemoryText.isNotBlank()) {
                                com.example.memory.MayaMemoryManager.addMemory(newMemoryText.trim())
                                newMemoryText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save 💾", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of memories
                if (memories.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("कोई याद सुरक्षित नहीं है। नई याद जोड़ें!", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(memories, key = { it.id }) { memory ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E2030),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(memory.text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("📅 ${memory.dateAdded}", color = Color(0xFF00E5FF).copy(alpha = 0.7f), fontSize = 10.sp)
                                    }
                                    IconButton(
                                        onClick = { com.example.memory.MayaMemoryManager.removeMemory(memory.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Chat history list
                if (chatHistory.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("अभी कोई पुरानी बातचीत नहीं है।", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(chatHistory, key = { it.id }) { msg ->
                            val isUser = msg.sender.equals("user", true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUser) Color(0xFF1C273C) else Color(0xFF28182B),
                                border = BorderStroke(1.dp, if (isUser) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color(0xFFFF4081).copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            if (isUser) "Babu 👤" else "Maya ❤️",
                                            color = if (isUser) Color(0xFF00E5FF) else Color(0xFFFF4081),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(msg.text, color = Color.White, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(msg.timestamp, color = Color.Gray, fontSize = 9.sp)
                                    }
                                    IconButton(
                                        onClick = { com.example.memory.MayaMemoryManager.removeChatMessage(msg.id) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Clear All & Export
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showConfirmClearDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252).copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("सब मिटाएं", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val exportedFile = com.example.memory.MayaMemoryManager.exportToFile(context)
                        if (exportedFile != null) {
                            android.widget.Toast.makeText(context, "फ़ाइल सेव हो गई: ${exportedFile.name}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("📁 एक्सपोर्ट करें", color = Color(0xFF00E5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
