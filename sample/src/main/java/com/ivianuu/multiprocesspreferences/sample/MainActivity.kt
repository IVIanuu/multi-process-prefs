package com.ivianuu.multiprocesspreferences.sample

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import com.ivianuu.multiprocesspreferences.MultiProcessSharedPreferences
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val prefs by lazy {
        MultiProcessSharedPreferences.create(this, MyPreferenceProvider.AUTHORITY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, SomeService::class.java))

        apply.setOnClickListener {
            val key = key_input.text?.toString()
            val value = value_input.text?.toString()
            if (key != null && key.isNotEmpty()
                && value != null) {
                val editor = prefs.edit()
                if (value.isNotEmpty()) {
                    editor.putString(key, value)
                } else {
                    editor.remove(key)
                }

                editor.apply()
            } else {
                Toast.makeText(this, "Invalid input!", Toast.LENGTH_SHORT).show()
            }
        }

        Handler().postDelayed({
            prefs.edit().clear().apply()
            prefs.edit()
                .putBoolean("boolean", true)
                .putFloat("float", 1f)
                .putInt("int", 13555)
                .putLong("long", 2455)
                .putString("string", "stwwgwagwwgw")
                .putStringSet("string_set", setOf("ggee", "kmgngg", "ynkdngdg"))
                .apply()
        }, 3000)
    }
}
