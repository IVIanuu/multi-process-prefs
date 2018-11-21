/*
 * Copyright 2018 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.multiprocessprefs.sample

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.multiprocessprefs.MultiProcessSharedPreferences
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val prefs by lazy {
        MultiProcessSharedPreferences(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val newValue = sharedPreferences.all[key]
        d { "pref changed $key -> $newValue" }
        d { "prefs ${sharedPreferences.all}" }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, SomeService::class.java))
        startService(Intent(this, SomeOtherService::class.java))

        put_boolean.prefClickListener { key, value -> putBoolean(key, value.toBoolean()) }
        put_float.prefClickListener { key, value -> putFloat(key, value.toFloat()) }
        put_int.prefClickListener { key, value -> putInt(key, value.toInt()) }
        put_long.prefClickListener { key, value -> putLong(key, value.toLong()) }
        put_string.prefClickListener { key, value -> putString(key, value) }

        remove.setOnClickListener {
            val key = remove_key_input.text.toString()
            prefs.edit().remove(key).apply()
        }

        clear.setOnClickListener { prefs.edit().clear().apply() }

        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroy()
    }

    private fun View.prefClickListener(block: SharedPreferences.Editor.(String, String) -> Unit) {
        setOnClickListener {
            val key = put_key_input.text.toString()
            val value = put_value_input.text.toString()
            prefs.edit()
                .apply { block(key, value) }
                .apply()
            put_key_input.setText("")
            put_value_input.setText("")
        }
    }

}
