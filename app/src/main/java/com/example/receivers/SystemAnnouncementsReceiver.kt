package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.live.MayaSpeaker

import com.example.settings.UserSettingsManager

/**
 * SystemAnnouncementsReceiver
 *
 * Real-time spoken announcements matching reference assistant:
 * 1. Flight / Airplane mode state change ("Flight mode lag gaya hai" / "Flight mode hat chuka hai").
 * 2. Smart Battery & Charging Assistant (announces plug in, 100% full battery warning, low battery alert).
 */
class SystemAnnouncementsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SystemAnnouncements"
        private var isFullBatteryAnnounced = false
        private var isLowBatteryAnnounced = false

        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                if (!UserSettingsManager.getAirplaneAnnouncementEnabled(context)) return
                val isAirplaneOn = intent.getBooleanExtra("state", false)
                if (isAirplaneOn) {
                    MayaSpeaker.speak("मालिक, फ्लाइट मोड लग गया है। अब इंटरनेट और कॉलिंग काम नहीं करेगी।")
                } else {
                    MayaSpeaker.speak("मालिक, फ्लाइट मोड हट चुका है। अब इंटरनेट और नेटवर्क चालू हैं।")
                }
            }

            Intent.ACTION_POWER_CONNECTED -> {
                if (!UserSettingsManager.getChargingAnnouncementEnabled(context)) return
                val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                if (level > 0) {
                    MayaSpeaker.speak("चार्जिंग शुरू हो गई है बाबू! बैटरी $level% है।")
                } else {
                    MayaSpeaker.speak("चार्जिंग शुरू हो गई है बाबू!")
                }
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                if (!UserSettingsManager.getChargingAnnouncementEnabled(context)) return
                isFullBatteryAnnounced = false
                MayaSpeaker.speak("चार्जर निकाल दिया गया है।")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                if (!UserSettingsManager.getChargingAnnouncementEnabled(context)) return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

                if (level > 0 && scale > 0) {
                    val batteryPct = (level * 100) / scale

                    // 100% full charge alert
                    if (batteryPct >= 100 && isCharging && !isFullBatteryAnnounced) {
                        isFullBatteryAnnounced = true
                        MayaSpeaker.speak("बैटरी 100% फुल चार्ज हो गई है बाबू! कृपया फोन को चार्जर से निकाल दीजिए।")
                    }

                    // Low battery (<15%) warning
                    if (batteryPct in 1..14 && !isCharging && !isLowBatteryAnnounced) {
                        isLowBatteryAnnounced = true
                        MayaSpeaker.speak("चेतावनी! बैटरी 15% से कम हो गई है बाबू। कृपया चार्जर लगा दीजिए।")
                    } else if (batteryPct >= 20) {
                        isLowBatteryAnnounced = false
                    }
                }
            }
        }
    }
}
