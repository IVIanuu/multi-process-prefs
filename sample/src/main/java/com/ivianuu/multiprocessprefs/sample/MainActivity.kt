package com.ivianuu.multiprocessprefs.sample

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.ivianuu.multiprocessprefs.MultiProcessSharedPreferences
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs by lazy {
        MultiProcessSharedPreferences(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val newValue = sharedPreferences.all[key]
        d { "pref changed $key -> $newValue" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, SomeService::class.java))
        startService(Intent(this, SomeOtherService::class.java))

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

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

}
