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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.guardian.VoiceGuardianManager

/**
 * SCREENSHOT / VIDEO 03:35: Voice Guardian (Owner Voice Recognition)
 * "Sirf tumhari awaaz ka jawab de"
 * - Models selection
 * - Voice Guardian ON toggle
 * - Enrolled awaazein info
 * - "+ Owner ki awaaz record karo"
 * - "Meri awaaz test karo"
 */
@Composable
fun VoiceGuardianScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    var isGuardianOn by remember { mutableStateOf(VoiceGuardianManager.isGuardianEnabled(context)) }
    var isEnrolled by remember { mutableStateOf(VoiceGuardianManager.isOwnerEnrolled(context)) }
    var enrolledDate by remember { mutableStateOf(VoiceGuardianManager.getEnrolledDate(context)) }
    var modelName by remember { mutableStateOf(VoiceGuardianManager.getModelName(context)) }

    var isRecording by remember { mutableStateOf(false) }
    var recordProgress by remember { mutableIntStateOf(0) }
    var isTesting by remember { mutableStateOf(false) }
    var testProgress by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var matchScoreResult by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Header
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
                    text = "Voice Guardian",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "सिर्फ मालिक की आवाज़ का जवाब दे",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Model Status Card (matching 03:35 in video)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.dp, Color(0xFF281E4B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Recognition model",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modelName,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Text(
                            text = "INSTALLED & ACTIVE",
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "यह ऑन-डिवाइस न्यूरल मॉडल बिना इंटरनेट के मालिक की आवाज़ की फ्रीक्वेंसी, पिच और टिम्बर पहचानता है।",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Guardian ON/OFF Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF140F27)),
            border = BorderStroke(1.5.dp, if (isGuardianOn) Color(0xFF2563EB) else Color(0xFF281E4B)),
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
                            Text(
                                text = "Voice Guardian ON",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isGuardianOn) "सक्रिय: अपरिचित व्यक्तियों के आदेश खारिज किए जाएंगे"
                            else "बंद: कोई भी 'Ok Maya' बोलकर हुक्म दे सकता है",
                            color = if (isGuardianOn) Color(0xFF4ADE80) else Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = isGuardianOn,
                        onCheckedChange = { checked ->
                            if (checked && !isEnrolled) {
                                Toast.makeText(context, "कृपया पहले नीचे '+ Owner ki awaaz record karo' से आवाज़ रिकॉर्ड करें!", Toast.LENGTH_LONG).show()
                            }
                            isGuardianOn = checked
                            VoiceGuardianManager.setGuardianEnabled(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF1E1738)
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))

                // Enrolled Voice Status
                Text(
                    text = "Enrolled awaazein (दर्ज आवाज़ें):",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isEnrolled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Owner Fingerprint Active", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("दर्ज की गई: $enrolledDate", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            }
                        }
                        TextButton(onClick = {
                            VoiceGuardianManager.clearOwnerEnrollment(context)
                            isEnrolled = false
                            isGuardianOn = false
                            VoiceGuardianManager.setGuardianEnabled(context, false)
                            Toast.makeText(context, "पुराना फिंगरप्रिंट हटा दिया गया", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        text = "अभी तक कोई आवाज़ सेव नहीं हुई। नीचे दिए गए बटन से अपनी आवाज़ एनरोल करें।",
                        color = Color(0xFFFB923C),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recording / Progress State Banner
        if (isRecording) {
            Surface(
                color = Color(0xFF1E1B4B),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎙️ माइक में 4 सेकंड तक स्पष्ट बोलें...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "उदा. 'माया सुनो, मैं तुम्हारा मालिक हूँ और तुम्हें मुझे ही सुनना है'",
                        color = Color(0xFFC084FC),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { recordProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF281E4B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("$recordProgress%", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Testing State Banner
        if (isTesting) {
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFF34D399)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎧 बोलकर टेस्ट करें: 'Ok Maya, kaam karo'",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { testProgress / 100f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = Color(0xFF34D399),
                        trackColor = Color(0xFF065F46)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("जाँच हो रही है... $testProgress%", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Status / Result Banner
        if (statusMessage != null) {
            Surface(
                color = if (matchScoreResult != null && matchScoreResult!! >= 65) Color(0xFF065F46).copy(alpha = 0.4f)
                else Color(0xFF451A03).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (matchScoreResult != null && matchScoreResult!! >= 65) Color(0xFF10B981)
                    else Color(0xFFF97316)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (matchScoreResult != null && matchScoreResult!! >= 65) "✅" else "ℹ️",
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = statusMessage ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Action Buttons (matching video: "+ Owner ki awaaz record karo" & "Meri awaaz test karo")
        Button(
            onClick = {
                statusMessage = null
                matchScoreResult = null
                isRecording = true
                VoiceGuardianManager.enrollOwnerVoice(
                    context = context,
                    onProgress = { recordProgress = it },
                    onComplete = { success, msg ->
                        isRecording = false
                        statusMessage = msg
                        if (success) {
                            isEnrolled = true
                            enrolledDate = VoiceGuardianManager.getEnrolledDate(context)
                            isGuardianOn = true
                            VoiceGuardianManager.setGuardianEnabled(context, true)
                        }
                    }
                )
            },
            enabled = !isRecording && !isTesting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEnrolled) "🔄 Re-record Owner Voice (आवाज़ दोबारा रिकॉर्ड करें)" else "+ Owner ki awaaz record karo",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                statusMessage = null
                matchScoreResult = null
                isTesting = true
                VoiceGuardianManager.testVoiceMatch(
                    context = context,
                    onProgress = { testProgress = it },
                    onResult = { matchPct, isOwner, msg ->
                        isTesting = false
                        matchScoreResult = matchPct
                        statusMessage = msg
                    }
                )
            },
            enabled = !isRecording && !isTesting && isEnrolled,
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Icon(Icons.Default.Hearing, contentDescription = null, tint = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Meri awaaz test karo (आवाज़ टेस्ट करें)",
                color = Color(0xFF38BDF8),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
