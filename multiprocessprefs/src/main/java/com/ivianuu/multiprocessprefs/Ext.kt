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

import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.json.JSONArray

internal const val COLUMN_KEY = "key"
internal const val COLUMN_TYPE = "type"
internal const val COLUMN_VALUE = "value"

internal val PROJECTION = arrayOf(COLUMN_KEY, COLUMN_TYPE, COLUMN_VALUE)

internal const val PREFS_NAME = "name"
internal const val PREF_KEY = "key"

internal val Any.prefType
    get() = when (this) {
        is Boolean -> PrefType.BOOLEAN
        is Float -> PrefType.FLOAT
        is Int -> PrefType.INT
        is Long -> PrefType.LONG
        is String -> PrefType.STRING
        is Set<*> -> PrefType.STRING_SET
        else -> throw IllegalArgumentException("unsupported type ${this.javaClass.name}")
    }

internal fun Any.serialize() = when (prefType) {
    PrefType.BOOLEAN -> toString()
    PrefType.FLOAT -> toString()
    PrefType.INT -> toString()
    PrefType.LONG -> toString()
    PrefType.STRING -> toString()
    PrefType.STRING_SET -> {
        val array = JSONArray()
        (this as Set<String>).forEach { array.put(it) }
        array.toString()
    }
}

internal fun String.deserialize(prefType: PrefType): Any = when (prefType) {
    PrefType.BOOLEAN -> toBoolean()
    PrefType.FLOAT -> toFloat()
    PrefType.INT -> toInt()
    PrefType.LONG -> toLong()
    PrefType.STRING -> toString()
    PrefType.STRING_SET -> {
        val array = JSONArray(this)
        val set = mutableSetOf<String>()
        (0 until array.length())
            .mapTo(set) { array.getString(it) }
        set
    }
}

internal fun SharedPreferences.Editor.putAny(key: String, value: Any) = apply {
    when (value.prefType) {
        PrefType.BOOLEAN -> putBoolean(key, value as Boolean)
        PrefType.FLOAT -> putFloat(key, value as Float)
        PrefType.INT -> putInt(key, value as Int)
        PrefType.LONG -> putLong(key, value as Long)
        PrefType.STRING -> putString(key, value as String)
        PrefType.STRING_SET -> putStringSet(key, value as Set<String>)
    }
}

internal fun String.toPrefType() = PrefType.values().first { it.key == this }

internal enum class PrefType(val key: String) {
    BOOLEAN("boolean"),
    FLOAT("float"),
    INT("int"),
    LONG("long"),
    STRING("string"),
    STRING_SET("string_set")
}

internal fun getAllUri(contentUri: Uri, name: String): Uri = contentUri.buildUpon()
    .appendPath(PREFS_NAME)
    .appendPath(name)
    .build()

internal fun getUri(contentUri: Uri, key: String, name: String): Uri = contentUri.buildUpon()
    .appendPath(PREFS_NAME)
    .appendPath(name)
    .appendPath(PREF_KEY)
    .appendPath(key)
    .build()

internal inline fun Any.d(m: () -> String) {
    Log.d(javaClass.simpleName, m())
}