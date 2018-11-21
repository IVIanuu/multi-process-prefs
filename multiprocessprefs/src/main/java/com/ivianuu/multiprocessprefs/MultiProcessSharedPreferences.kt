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

package com.ivianuu.multiprocessprefs

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class MultiProcessSharedPreferences private constructor(
    private val context: Context,
    val name: String
) : SharedPreferences {

    private val observer = object : ContentObserver(Handler()) {
        override fun deliverSelfNotifications() = false

        override fun onChange(selfChange: Boolean, uri: Uri) {
            val name = uri.pathSegments[1]
            if (this@MultiProcessSharedPreferences.name == name) {
                val key = uri.lastPathSegment
                listeners.toList().forEach {
                    it.onSharedPreferenceChanged(this@MultiProcessSharedPreferences, key)
                }
            }
        }
    }
    private val listeners =
        mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private val contentUri = Uri.parse("content://${context.packageName}.prefs")

    init {
        val uri = contentUri
            .buildUpon()
            .appendPath(PREFS_NAME)
            .build()

        context.contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun getAll(): Map<String, *> {
        val values = mutableMapOf<String, Any>()

        val c = context.contentResolver.query(
            getAllUri(contentUri, name), PROJECTION,
            null, null, null
        )

        if (c != null) {
            try {
                while (c.moveToNext()) {
                    val key = c.getString(c.getColumnIndexOrThrow(COLUMN_KEY))
                    val prefType = c.getString(c.getColumnIndexOrThrow(COLUMN_TYPE)).toPrefType()
                    val value =
                        c.getString(c.getColumnIndexOrThrow(COLUMN_VALUE)).deserialize(prefType)
                    values[key] = value
                }
            } finally {
                try {
                    c.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Ignore
                }
            }
        }

        return values
    }

    override fun getString(key: String, defaultValue: String) =
        getValue(key, defaultValue, PrefType.STRING)

    override fun getStringSet(key: String, defaultValue: Set<String>) =
        getValue(key, defaultValue, PrefType.STRING_SET)

    override fun getInt(key: String, defaultValue: Int) =
        getValue(key, defaultValue, PrefType.INT)

    override fun getLong(key: String, defaultValue: Long) =
        getValue(key, defaultValue, PrefType.LONG)

    override fun getFloat(key: String, defaultValue: Float) =
        getValue(key, defaultValue, PrefType.FLOAT)

    override fun getBoolean(key: String, defaultValue: Boolean) =
        getValue(key, defaultValue, PrefType.BOOLEAN)

    override fun contains(key: String) = all.containsKey(key)

    override fun edit(): SharedPreferences.Editor = MultiProcessEditor(context, name, contentUri)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(listener)
    }

    private fun <T> getValue(
        key: String,
        defaultValue: T,
        requiredPrefType: PrefType
    ): T {
        val c = context.contentResolver.query(
            getUri(contentUri, key, name), PROJECTION, null, null, null
        )

        try {
            return if (c != null && c.moveToFirst()) {
                val prefType = c.getString(c.getColumnIndexOrThrow(COLUMN_TYPE)).toPrefType()

                if (prefType == requiredPrefType) {
                    c.getString(c.getColumnIndexOrThrow(COLUMN_VALUE))
                        .deserialize(prefType) as T
                } else {
                    defaultValue // todo maybe throw to mirror the original prefs behavior
                }
            } else {
                defaultValue
            }
        } finally {
            if (c != null) {
                try {
                    c.close()
                } catch (e: Exception) {
                    // Ignore
                }

            }
        }
    }

    private class MultiProcessEditor(
        private val context: Context,
        private val name: String,
        private val contentUri: Uri
    ) : SharedPreferences.Editor {

        private val values = mutableMapOf<String, Any>()
        private var clear = false

        override fun putString(key: String, value: String) = putValue(key, value)

        override fun putStringSet(key: String, value: Set<String>) = putValue(key, value)

        override fun putInt(key: String, value: Int) = putValue(key, value)

        override fun putLong(key: String, value: Long) = putValue(key, value)

        override fun putFloat(key: String, value: Float) = putValue(key, value)

        override fun putBoolean(key: String, value: Boolean) = putValue(key, value)

        override fun remove(key: String) = putValue(key, this)

        override fun clear() = apply {
            clear = true
            values.clear()
        }

        override fun commit(): Boolean {
            if (clear) {
                val uri = getAllUri(contentUri, name)
                context.contentResolver.delete(uri, null, null)
            }

            values.forEach { (key, value) ->
                val uri = getUri(contentUri, key, name)

                // "this" means that the value should be removed
                if (value != this) {
                    val contentValues = ContentValues()
                    contentValues.put(COLUMN_KEY, key)
                    contentValues.put(COLUMN_VALUE, value.serialize())
                    contentValues.put(COLUMN_TYPE, value.prefType.key)
                    context.contentResolver.update(uri, contentValues, null, null)
                } else {
                    context.contentResolver.delete(uri, null, null)
                }
            }

            return true
        }

        override fun apply() {
            commit()
        }

        private fun putValue(key: String, value: Any) = apply {
            values[key] = value
        }
    }

    companion object {
        private val instances =
            mutableMapOf<String, MultiProcessSharedPreferences>()

        @Synchronized
        operator fun invoke(
            context: Context,
            name: String = context.packageName + "_preferences" // default name
        ) = instances.getOrPut(name) {
            MultiProcessSharedPreferences(context.applicationContext, name)
        }
    }
}