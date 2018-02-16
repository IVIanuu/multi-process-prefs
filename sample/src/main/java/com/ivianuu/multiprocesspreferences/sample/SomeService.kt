package com.ivianuu.multiprocesspreferences.sample

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.ivianuu.multiprocesspreferences.MultiProcessSharedPreferences

/**
 * @author Manuel Wrage (IVIanuu)
 */
class SomeService : Service() {

    private val prefs by lazy {
        MultiProcessSharedPreferences.create(this,
            MyPreferenceProvider.AUTHORITY, MyPreferenceProvider.PREF_NAME)
    }
    private val rxPrefs by lazy { RxSharedPreferences.create(prefs) }

    override fun onCreate() {
        super.onCreate()

        rxPrefs.getString("test").asObservable()
            .subscribe { d { "jo $it" } }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}