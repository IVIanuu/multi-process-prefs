package com.ivianuu.multiprocesspreferences

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import java.lang.ref.WeakReference
import java.util.*

/**
 * Multi process shared preferences
 */
class MultiProcessSharedPreferences private constructor(
    private val context: Context,
    prefName: String
): SharedPreferences {

    private val handler = Handler()

    private val baseUri = Uri.parse("content://${context.packageName}.prefs")
        .buildUpon()
        .appendPath(prefName)
        .build()

    private val listeners = WeakHashMap<SharedPreferences.OnSharedPreferenceChangeListener, PreferenceContentObserver>()

    override fun contains(key: String): Boolean {
        return containsKey(key)
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return querySingle(key, defValue, Contract.TYPE_BOOLEAN)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return querySingle(key, defValue, Contract.TYPE_FLOAT)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return querySingle(key, defValue, Contract.TYPE_INT)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return querySingle(key, defValue, Contract.TYPE_LONG)
    }

    override fun getString(key: String, defValue: String?): String? {
        return querySingle(key, defValue, Contract.TYPE_STRING)
    }

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        return querySingle(key, defValues, Contract.TYPE_STRING_SET)
    }

    override fun getAll(): Map<String, Any> {
        return queryAll()
    }

    override fun edit(): SharedPreferences.Editor {
        return Editor(this)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        if (listeners.containsKey(listener)) return
        val observer = PreferenceContentObserver(listener, handler, this)
        listeners[listener] = observer
        context.contentResolver.registerContentObserver(baseUri, true, observer)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        val observer = listeners.remove(listener)
        if (observer != null) {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }

    private fun query(uri: Uri, columns: Array<String>): Cursor? {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, columns, null, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (cursor == null) {
            throw IllegalAccessException("query() failed or returned null cursor")
        }

        return cursor
    }

    private fun bulkInsert(uri: Uri, values: Array<ContentValues>): Boolean {
        val count: Int
        try {
            count = context.contentResolver.bulkInsert(uri, values)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return count == values.size
    }

    private fun <T> querySingle(key: String, defValue: T, expectedType: Int): T {
        val uri = baseUri.buildUpon().appendPath(key).build()
        val columns = arrayOf(Contract.COLUMN_TYPE, Contract.COLUMN_VALUE)
        val cursor = query(uri, columns) ?: return defValue

        cursor.use {
            if (!cursor.moveToFirst()) {
                return defValue
            }

            val typeCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_TYPE)
            val type = cursor.getInt(typeCol)
            if (type == Contract.TYPE_NULL) {
                return defValue
            } else if (type != expectedType) {
                throw ClassCastException("Preference type mismatch")
            }

            val valueCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_VALUE)
            return getValue(cursor, typeCol, valueCol) as T
        }
    }

    private fun queryAll(): Map<String, Any> {
        val uri = baseUri.buildUpon().appendPath("").build()
        val columns = arrayOf(
            Contract.COLUMN_KEY,
            Contract.COLUMN_TYPE,
            Contract.COLUMN_VALUE
        )
        val cursor = query(uri, columns) ?: return emptyMap()
        cursor.use {
            val map = HashMap<String, Any>()

            val keyCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_KEY)
            val typeCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_TYPE)
            val valueCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_VALUE)

            while (cursor.moveToNext()) {
                val key = cursor.getString(keyCol)
                map[key] = getValue(cursor, typeCol, valueCol)
            }
            return map
        }
    }

    private fun containsKey(key: String): Boolean {
        val uri = baseUri.buildUpon().appendPath(key).build()
        val columns = arrayOf(Contract.COLUMN_TYPE)
        val cursor = query(uri, columns) ?: return false

        cursor.use {
            if (!cursor.moveToFirst()) {
                return false
            }

            val typeCol = cursor.getColumnIndexOrThrow(Contract.COLUMN_TYPE)
            return cursor.getInt(typeCol) != Contract.TYPE_NULL
        }
    }

    private fun getValue(cursor: Cursor, typeCol: Int, valueCol: Int): Any {
        val expectedType = cursor.getInt(typeCol)
        return when (expectedType) {
            Contract.TYPE_STRING -> cursor.getString(valueCol)
            Contract.TYPE_STRING_SET -> cursor.getString(valueCol).deserializedToStringSet()
            Contract.TYPE_INT -> cursor.getInt(valueCol)
            Contract.TYPE_LONG -> cursor.getLong(valueCol)
            Contract.TYPE_FLOAT -> cursor.getFloat(valueCol)
            Contract.TYPE_BOOLEAN -> cursor.getInt(valueCol) != 0
            else -> throw AssertionError("Invalid expected type: " + expectedType)
        }
    }


    private class Editor(private val preferences: MultiProcessSharedPreferences): SharedPreferences.Editor {

        private val values = ArrayList<ContentValues>()

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_BOOLEAN).put(
                Contract.COLUMN_VALUE, if (value) 1 else 0)
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_FLOAT).put(
                Contract.COLUMN_VALUE, value)
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_INT).put(Contract.COLUMN_VALUE, value)
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_LONG).put(
                Contract.COLUMN_VALUE, value)
            return this
        }

        override fun putString(key: String, value: String): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_STRING).put(
                Contract.COLUMN_VALUE, value)
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>
        ): SharedPreferences.Editor {
            createAddOp(key, Contract.TYPE_STRING_SET).put(
                Contract.COLUMN_VALUE, values.serializedToString())
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            createRemoveOp(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            createRemoveOp("")
            return this
        }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            val uri = preferences.baseUri.buildUpon().appendPath("").build()
            return preferences.bulkInsert(uri, values.toTypedArray())
        }

        private fun createAddOp(key: String, type: Int): ContentValues {
            val values = createContentValues(key, type)
            this.values.add(values)
            return values
        }

        private fun createRemoveOp(key: String): ContentValues {
            // Note: Remove operations are inserted at the beginning
            // of the list (this preserves the SharedPreferences behavior
            // that all removes are performed before any adds)
            val values = createContentValues(key, Contract.TYPE_NULL)
            values.putNull(Contract.COLUMN_VALUE)
            this.values.add(0, values)
            return values
        }

        private fun createContentValues(key: String, type: Int): ContentValues {
            val values = ContentValues(4)
            values.put(Contract.COLUMN_KEY, key)
            values.put(Contract.COLUMN_TYPE, type)
            return values
        }
    }

    private class PreferenceContentObserver (
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
        handler: Handler,
        private val preferences: MultiProcessSharedPreferences
    ) : ContentObserver(handler) {
        private val mListener: WeakReference<SharedPreferences.OnSharedPreferenceChangeListener> =
            WeakReference<SharedPreferences.OnSharedPreferenceChangeListener>(listener)

        override fun deliverSelfNotifications(): Boolean {
            return true
        }

        override fun onChange(selfChange: Boolean, uri: Uri) {
            val prefKey = uri.lastPathSegment

            val listener = mListener.get()
            if (listener == null) {
                preferences.context.contentResolver.unregisterContentObserver(this)
            } else {
                listener.onSharedPreferenceChanged(preferences, prefKey)
            }
        }
    }

    companion object {

        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            prefName: String = context.packageName + "_preferences" // default name
        ): SharedPreferences {
            return MultiProcessSharedPreferences(context, prefName)
        }

    }
}