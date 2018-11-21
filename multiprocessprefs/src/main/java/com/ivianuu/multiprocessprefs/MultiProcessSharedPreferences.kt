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
import android.os.Looper
import android.util.Base64
import org.json.JSONObject
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Multi process [SharedPreferences] which are talking to a [MultiProcessPrefsProvider]
 */
class MultiProcessSharedPreferences private constructor(
    private val context: Context,
    val packageName: String,
    val name: String
) : SharedPreferences {

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun deliverSelfNotifications() = false

        override fun onChange(selfChange: Boolean, uri: Uri): Unit = lock.withLock {
            val name = uri.pathSegments[0]

            // not related to us
            if (this@MultiProcessSharedPreferences.name != name) return

            val changeId = uri.pathSegments[2]

            // self change?
            if (pendingChanges.contains(changeId)) {
                pendingChanges.remove(changeId)
                return
            }

            val key = uri.pathSegments[1]

            // someone else cleared the all values so just clear the map
            if (key == KEY_ALL) {
                map.clear()
                return
            }

            // get the new value
            val decodedNewValue = String(Base64.decode(uri.pathSegments[3], Base64.DEFAULT))
            val newValueJson = JSONObject(decodedNewValue)
            val prefType = PrefType.valueOf(uri.pathSegments[4])
            val newValue = newValueJson.optString(KEY_VALUE)?.deserialize(prefType)

            val oldValue = map[key]

            // no op
            if (oldValue == newValue) return

            // reflect the change in our local map
            if (newValue != null) {
                map[key] = newValue
            } else {
                map.remove(key)
            }

            // notify listeners
            listeners.toList().forEach {
                it.onSharedPreferenceChanged(this@MultiProcessSharedPreferences, key)
            }
        }
    }
    private val listeners =
        mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private val uri = Uri.parse("content://$packageName.prefs/$name")

    private val pendingChanges = mutableSetOf<String>()

    private val map = mutableMapOf<String, Any>()

    private val lock = ReentrantLock()

    init {
        reloadAll()
        context.contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun getAll(): Map<String, *> = lock.withLock { map.toMap() }

    override fun getString(key: String, defaultValue: String) =
        getValue(key, defaultValue)

    override fun getStringSet(key: String, defaultValue: Set<String>) =
        getValue(key, defaultValue)

    override fun getInt(key: String, defaultValue: Int) =
        getValue(key, defaultValue)

    override fun getLong(key: String, defaultValue: Long) =
        getValue(key, defaultValue)

    override fun getFloat(key: String, defaultValue: Float) =
        getValue(key, defaultValue)

    override fun getBoolean(key: String, defaultValue: Boolean) =
        getValue(key, defaultValue)

    override fun contains(key: String) = lock.withLock { map.contains(key) }

    override fun edit(): SharedPreferences.Editor = MultiProcessEditor()

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener): Unit =
        lock.withLock {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener): Unit =
        lock.withLock {
            listeners.remove(listener)
        }

    private fun reloadAll(): Unit = lock.withLock {
        val values = mutableMapOf<String, Any>()

        val c = context.contentResolver.query(
            uri, PROJECTION,
            null, null, null
        )

        if (c != null) {
            try {
                while (c.moveToNext()) {
                    val key = c.getString(c.getColumnIndexOrThrow(KEY_KEY))
                    val prefType = PrefType.valueOf(c.getString(c.getColumnIndexOrThrow(KEY_TYPE)))
                    val value =
                        c.getString(c.getColumnIndexOrThrow(KEY_VALUE)).deserialize(prefType)
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

        map.clear()
        map.putAll(values)
    }

    private fun <T> getValue(key: String, defaultValue: T) =
        lock.withLock { map.getOrElse(key) { defaultValue } as T }

    private inner class MultiProcessEditor : SharedPreferences.Editor {

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
            lock.withLock {
                clear = true
                values.clear()
            }
        }

        override fun commit() = lock.withLock {
            if (clear) {
                val changeId = UUID.randomUUID().toString()
                val contentValues = ContentValues()
                contentValues.put(KEY_ACTION, Action.CLEAR.toString())
                contentValues.put(KEY_CHANGE_ID, changeId)

                map.clear()

                context.contentResolver.update(uri, contentValues, null, null)
            }

            val changedKeys = mutableSetOf<String>()

            values.forEach { (key, value) ->
                // "this" means that the value should be removed
                if (value != this) {
                    val changeId = UUID.randomUUID().toString()
                    pendingChanges.add(changeId)

                    val contentValues = ContentValues()

                    contentValues.put(KEY_ACTION, Action.PUT.toString())
                    contentValues.put(KEY_CHANGE_ID, changeId)
                    contentValues.put(KEY_KEY, key)
                    contentValues.put(KEY_VALUE, value.serialize())
                    contentValues.put(KEY_TYPE, value.prefType.toString())

                    if (map[key] != value) {
                        changedKeys.add(key)
                    }

                    map[key] = value

                    context.contentResolver.update(uri, contentValues, null, null)
                } else {
                    if (map.contains(key)) {
                        val changeId = UUID.randomUUID().toString()
                        pendingChanges.add(changeId)

                        val contentValues = ContentValues()

                        contentValues.put(KEY_ACTION, Action.REMOVE.toString())
                        contentValues.put(KEY_CHANGE_ID, changeId)
                        contentValues.put(KEY_KEY, key)

                        changedKeys.add(key)
                        map.remove(key)
                        context.contentResolver.update(uri, contentValues, null, null)
                    }
                }
            }

            if (changedKeys.isNotEmpty()) {
                val listeners = listeners.toList()
                changedKeys.forEach { key ->
                    listeners.forEach {
                        it.onSharedPreferenceChanged(
                            this@MultiProcessSharedPreferences,
                            key
                        )
                    }
                }
            }

            true
        }

        override fun apply() {
            commit()
        }

        private fun putValue(key: String, value: Any) = apply {
            lock.withLock { values[key] = value }
        }
    }

    companion object {
        private val instances =
            mutableMapOf<String, MultiProcessSharedPreferences>()

        private val instancesLock = ReentrantLock()

        /**
         * Returns a new [MultiProcessSharedPreferences] instance
         */
        operator fun invoke(
            context: Context,
            packageName: String = context.packageName,
            preferencesName: String = packageName + "_preferences" // default name
        ) = instancesLock.withLock {
            instances.getOrPut(preferencesName) {
                MultiProcessSharedPreferences(
                    context.applicationContext, packageName, preferencesName
                )
            }
        }
    }
}