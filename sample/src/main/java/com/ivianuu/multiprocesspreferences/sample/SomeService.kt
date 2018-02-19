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
        MultiProcessSharedPreferences.create(this)
    }
    private val rxPrefs by lazy { RxSharedPreferences.create(prefs) }

    override fun onCreate() {
        super.onCreate()

        rxPrefs.getBoolean("boolean").asObservable()
            .subscribe { d { "boolean changed $it" } }

        rxPrefs.getFloat("float").asObservable()
            .subscribe { d { "float changed $it" } }

        rxPrefs.getInteger("int").asObservable()
            .subscribe { d { "int changed $it" } }

        rxPrefs.getLong("long").asObservable()
            .subscribe { d { "long changed $it" } }

        rxPrefs.getString("string").asObservable()
            .subscribe { d { "string changed $it" } }

        rxPrefs.getStringSet("string_set").asObservable()
            .subscribe { d { "string set changed $it" } }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}