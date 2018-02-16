package com.ivianuu.multiprocesspreferences.sample

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.ivianuu.multiprocesspreferences.MultiProcessSharedPreferences

/**
 * @author Manuel Wrage (IVIanuu)
 */
class SomeService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs by lazy {
        MultiProcessSharedPreferences.create(this,
            MyPreferenceProvider.AUTHORITY, MyPreferenceProvider.PREF_NAME)
    }

        override fun onCreate() {
        super.onCreate()

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String) {
        d { "key $key changed " +
                "value is ${prefs.all[key]}" }
    }

}