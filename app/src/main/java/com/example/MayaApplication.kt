package com.example

import android.app.Application
import android.util.Log
import com.example.live.MayaSpeaker

class MayaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("MayaApplication", "Application onCreate: Initializing Maya natural voice speaker...")
        MayaSpeaker.init(this)
    }
}
