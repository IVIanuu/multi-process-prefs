package com.ivianuu.multiprocesspreferences

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.util.Pair
import com.ivianuu.multiprocesspreferences.Contract.FIELD_KEY
import com.ivianuu.multiprocesspreferences.Contract.FIELD_VALUE
import com.ivianuu.multiprocesspreferences.Contract.PREFERENCES_ENTITY
import com.ivianuu.multiprocesspreferences.Contract.PROJECTION
import com.ivianuu.multiprocesspreferences.Util.decodePath
import com.ivianuu.multiprocesspreferences.Util.marshallSet
import com.ivianuu.multiprocesspreferences.Util.resolveUri
import com.ivianuu.multiprocesspreferences.Util.unmarshallSet
import org.json.JSONArray
import org.json.JSONException
import java.util.*

class MultiProcessSharedPreferences constructor(
    private val context: Context,
    val name: String
) : SharedPreferences {

    private val observer = object : ContentObserver(Handler()) {
        override fun deliverSelfNotifications(): Boolean {
            return false
        }

        override fun onChange(selfChange: Boolean, uri: Uri) {
            val name = decodePath(uri.pathSegments[1])
            if (name == name) {
                val key = decodePath(uri.lastPathSegment)
                for (cb in listeners) {
                    cb.onSharedPreferenceChanged(this@MultiProcessSharedPreferences, key)
                }
            }
        }
    }
    private val listeners = ArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()
    private val contentUri = Uri.parse("content://" + context.packageName + ".prefs")
    
    init {
        val uri = contentUri 
            .buildUpon()
            .appendPath(PREFERENCES_ENTITY)
            .build()
        context.contentResolver.registerContentObserver(uri, true, observer)
    }

    override fun getAll(): Map<String, *> {
        val values = HashMap<String, Any?>()
        val c = context.contentResolver.query(
            resolveUri(contentUri,null, name), PROJECTION, null, null, null
        )
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    val key = c.getString(c.getColumnIndexOrThrow(FIELD_KEY))
                    val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
                    var value: Any? = null
                    when (type) {
                        Cursor.FIELD_TYPE_INTEGER -> value =
                                c.getLong(c.getColumnIndexOrThrow(FIELD_VALUE))
                        Cursor.FIELD_TYPE_FLOAT -> value =
                                c.getFloat(c.getColumnIndexOrThrow(FIELD_VALUE))
                        Cursor.FIELD_TYPE_STRING -> {
                            val v = c.getString(c.getColumnIndexOrThrow(FIELD_VALUE))
                            value = if (v == "true" || v == "false") {
                                v.toBoolean()
                            } else {
                                try {
                                    unmarshallSet(v)
                                } catch (e: JSONException) {
                                    v
                                }

                            }
                        }
                    }

                    values[key] = value
                }
            } finally {
                try {
                    c.close()
                } catch (e: Exception) {
                    // Ignore
                }

            }
        }
        return values
    }


    override fun getString(key: String, defValue: String?): String? {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValue
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            return if (type != Cursor.FIELD_TYPE_STRING) {
                defValue
            } else c.getString(c.getColumnIndexOrThrow(FIELD_VALUE))

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


    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValues
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            if (type != Cursor.FIELD_TYPE_STRING) {
                return defValues
            }

            try {
                val v = c.getString(c.getColumnIndexOrThrow(FIELD_VALUE))
                val array = JSONArray(v)
                val size = array.length()
                val set = HashSet<String>(size)
                (0 until size).mapTo(set) { array.getString(it) }
                return set

            } catch (e: JSONException) {
                // Ignore
            }

            return defValues
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

    override fun getInt(key: String, defValue: Int): Int {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValue
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            return if (type != Cursor.FIELD_TYPE_INTEGER) {
                defValue
            } else c.getInt(
                c.getColumnIndexOrThrow(
                    FIELD_VALUE
                )
            )

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

    override fun getLong(key: String, defValue: Long): Long {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValue
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            return if (type != Cursor.FIELD_TYPE_INTEGER) {
                defValue
            } else c.getLong(c.getColumnIndexOrThrow(FIELD_VALUE))

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

    override fun getFloat(key: String, defValue: Float): Float {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValue
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            return if (type != Cursor.FIELD_TYPE_FLOAT) {
                defValue
            } else c.getFloat(c.getColumnIndexOrThrow(FIELD_VALUE))

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

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        val c = context.contentResolver.query(
            resolveUri(contentUri, key, name), PROJECTION, null, null, null
        )
        try {
            if (c == null || !c.moveToFirst()) {
                return defValue
            }
            val type = c.getType(c.getColumnIndexOrThrow(FIELD_VALUE))
            return if (type != Cursor.FIELD_TYPE_STRING) {
                defValue
            } else java.lang.Boolean.parseBoolean(
                c.getString(
                    c.getColumnIndexOrThrow(
                        FIELD_VALUE
                    )
                )
            )

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

    override fun contains(key: String): Boolean {
        return all.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return MultiProcessEditor(context, name, contentUri)
    }

    override fun registerOnSharedPreferenceChangeListener(cb: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.add(cb)
    }

    override fun unregisterOnSharedPreferenceChangeListener(cb: SharedPreferences.OnSharedPreferenceChangeListener) {
        listeners.remove(cb)
    }

    private class MultiProcessEditor(
        private val context: Context,
        private val preferencesFileName: String,
        private val contentUri: Uri
    ) : SharedPreferences.Editor {
        private val values: MutableList<Pair<String, Any>>
        private val removedEntries: MutableSet<String>
        private var clearAllFlag: Boolean = false

        init {
            values = ArrayList()
            removedEntries = HashSet()
            clearAllFlag = false
        }

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            values.add(Pair<String, Any>(key, value as Any?))
            removedEntries.remove(key)
            return this
        }

        override fun putStringSet(key: String, values: Set<String>?): SharedPreferences.Editor {
            this.values.add(Pair<String, Any>(key, values as Any?))
            removedEntries.remove(key)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            values.add(Pair(key, value as Any))
            removedEntries.remove(key)
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            values.add(Pair(key, value as Any))
            removedEntries.remove(key)
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            values.add(Pair(key, value as Any))
            removedEntries.remove(key)
            return this
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            values.add(Pair(key, value as Any))
            removedEntries.remove(key)
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            val it = values.iterator()
            while (it.hasNext()) {
                if (it.next().first == key) {
                    it.remove()
                    break
                }
            }
            removedEntries.add(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAllFlag = true
            removedEntries.clear()
            values.clear()
            return this
        }

        override fun commit(): Boolean {
            if (clearAllFlag) {
                val uri = resolveUri(contentUri, null, preferencesFileName)
                context.contentResolver.delete(uri, null, null)
            }
            clearAllFlag = false

            val values = ContentValues()
            for (v in this.values) {
                val uri = resolveUri(contentUri, v.first, preferencesFileName)
                values.put(FIELD_KEY, v.first)
                when {
                    v.second is Boolean -> values.put(FIELD_VALUE, v.second as Boolean)
                    v.second is Long -> values.put(FIELD_VALUE, v.second as Long)
                    v.second is Int -> values.put(FIELD_VALUE, v.second as Int)
                    v.second is Float -> values.put(FIELD_VALUE, v.second as Float)
                    v.second is String -> values.put(FIELD_VALUE, v.second as String)
                    v.second is Set<*> -> values.put(
                        FIELD_VALUE,
                        marshallSet(v.second as Set<String>)
                    )
                    else -> throw IllegalArgumentException("Unsupported type for key " + v.first)
                }

                context.contentResolver.update(uri, values, null, null)
            }

            removedEntries
                .map { resolveUri(contentUri, it, preferencesFileName) }
                .forEach { context.contentResolver.delete(it, null, null) }
            return true
        }

        override fun apply() {
            commit()
        }
    }

    companion object {
        private val instances = HashMap<String, MultiProcessSharedPreferences>()

        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            name: String = context.packageName + "_preferences" // default name
        ): SharedPreferences {
            return instances.getOrPut(name) {
                MultiProcessSharedPreferences(context.applicationContext, name)
            }
        }
    }
}