package com.ivianuu.multiprocessprefs.sample

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.ivianuu.multiprocessprefs.MultiProcessSharedPreferences

/**
 * @author Manuel Wrage (IVIanuu)
 */
class SomeOtherService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs by lazy {
        MultiProcessSharedPreferences(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val newValue = sharedPreferences.all[key]
        d { "pref changed $key -> $newValue" }
    }

    override fun onCreate() {
        super.onCreate()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

}