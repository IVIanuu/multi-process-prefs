package com.ivianuu.multiprocesspreferences.sample

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.f2prateek.rx.preferences2.RxSharedPreferences
import com.ivianuu.multiprocesspreferences.MultiProcessSharedPreferences
import com.ivianuu.multiprocesspreferences.MultiProcessSharedPreferencesProvider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val prefs by lazy {
        MultiProcessSharedPreferences.create(this)
    }
    private val rxPrefs by lazy {
        RxSharedPreferences.create(prefs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, SomeService::class.java))

        some_switch.setOnClickListener {
            prefs.edit()
                .putBoolean("boolean", some_switch.isChecked)
                .putFloat("float", prefs.getFloat("float", 0f) + 1f)
                .putInt("int", prefs.getInt("int", 0) + 1)
                .putLong("long", prefs.getLong("long", 0L) + 1L)
                .apply()
        }

        apply.setOnClickListener {
            val key = key_input.text?.toString()
            val value = value_input.text?.toString()
            if (key != null && key.isNotEmpty()
                && value != null) {
                val editor = prefs.edit()
                if (value.isNotEmpty()) {
                    editor.putString(key, value)

                    val set = prefs.getStringSet("string_set", emptySet())!!.toMutableSet()
                    set.add(value)
                    editor.putStringSet("string_set", set)
                } else {
                    editor.remove(key)
                }

                editor.apply()
            } else {
                Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show()
            }
        }

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
}
