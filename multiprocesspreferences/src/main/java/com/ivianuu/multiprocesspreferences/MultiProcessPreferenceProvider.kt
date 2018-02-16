package com.ivianuu.multiprocesspreferences

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log

/**
 * A [ContentProvider] for [SharedPreferences]
 */
abstract class MultiProcessPreferenceProvider(
    authority: String,
    private val prefNames: Array<String>
) : ContentProvider(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val baseUri = Uri.parse("content://" + authority)
    private val preferences = HashMap<String, SharedPreferences>()

    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
        addURI(authority, "*/", PREFERENCES_ID)
        addURI(authority, "*/*", PREFERENCE_ID)
    }

    override fun onCreate(): Boolean {
        // get prefs and register change listeners
        for (prefName in prefNames) {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            prefs.registerOnSharedPreferenceChangeListener(this)
            preferences[prefName] = prefs
        }

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        // If no projection is specified, we return all columns.
        val projection = projection ?: Contract.ALL_COLUMNS

        val nameKeyPair = parseUri(uri)
        val prefName = nameKeyPair.name
        val prefKey = nameKeyPair.key

        val prefs = getPreferencesOrThrow(prefName, prefKey, false)
        val prefMap = prefs.all

        val cursor = MatrixCursor(projection)
        if (isSingleKey(prefKey)) {
            val prefValue = prefMap[prefKey]
            if (prefValue != null) cursor.addRow(buildRow(projection, prefKey, prefValue))
        } else {
            for ((key, value) in prefMap) {
                if (value == null) continue
                cursor.addRow(buildRow(projection, key, value))
            }
        }

        return cursor
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        if (values == null) {
            return null
        }

        val nameKeyPair = parseUri(uri)
        val prefName = nameKeyPair.name
        val prefKey = getKeyFromUriOrValues(nameKeyPair, values)

        val prefs = getPreferencesOrThrow(prefName, prefKey, true)
        val editor = prefs.edit()

        putPreference(editor, prefKey, values)

        return if (editor.commit()) {
            getPreferenceUri(prefName, prefKey)
        } else {
            null
        }
    }

    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val nameKeyPair = parseUri(uri)
        val prefName = nameKeyPair.name
        if (isSingleKey(nameKeyPair.key)) {
            throw IllegalArgumentException("Cannot bulk insert with single key URI")
        }

        val prefs = getPreferencesByName(prefName)
        val editor = prefs.edit()

        for (value in values) {
            val prefKey = getKeyFromValues(value)
            checkAccessOrThrow(prefName, prefKey, true)
            putPreference(editor, prefKey, value)
        }

        return if (editor.commit()) {
            values.size
        } else {
            0
        }
    }

    override fun delete(uri: Uri, selection: String, selectionArgs: Array<String>): Int {
        val nameKeyPair = parseUri(uri)
        val prefName = nameKeyPair.name
        val prefKey = nameKeyPair.key

        val prefs = getPreferencesOrThrow(prefName, prefKey, true)
        val editor = prefs.edit()

        if (isSingleKey(prefKey)) {
            editor.remove(prefKey)
        } else {
            editor.clear()
        }

        return if (editor.commit()) {
            1
        } else {
            0
        }
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String,
        selectionArgs: Array<String>
    ): Int {
        return if (values == null) {
            delete(uri, selection, selectionArgs)
        } else {
            if (insert(uri, values) != null) 1 else 0
        }
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences, prefKey: String) {
        for ((prefName, value) in preferences) {
            if (value == prefs) {
                val uri = getPreferenceUri(prefName, prefKey)
                context.contentResolver.notifyChange(uri, null)
                return
            }
        }
    }

    protected fun checkAccess(prefName: String, prefKey: String, write: Boolean): Boolean {
        return true
    }

    private fun putPreference(
        editor: SharedPreferences.Editor,
        prefKey: String,
        values: ContentValues
    ) {
        val type = values.getAsInteger(Contract.COLUMN_TYPE)
                ?: throw IllegalArgumentException("Invalid or no preference type specified")

        val value = values.get(Contract.COLUMN_VALUE).deserialized(type)

        if (!isSingleKey(prefKey)) {
            if (type == Contract.TYPE_NULL) {
                editor.clear()
                return
            } else {
                throw IllegalArgumentException("Attempting to insert preference with null or empty key")
            }
        }

        when (type) {
            Contract.TYPE_NULL -> editor.remove(prefKey)
            Contract.TYPE_STRING -> editor.putString(prefKey, value as String)
            Contract.TYPE_STRING_SET -> editor.putStringSet(prefKey, (value as String).deserializedToStringSet())
            Contract.TYPE_INT -> editor.putInt(prefKey, value as Int)
            Contract.TYPE_LONG -> editor.putLong(prefKey, value as Long)
            Contract.TYPE_FLOAT -> editor.putFloat(prefKey, value as Float)
            Contract.TYPE_BOOLEAN -> editor.putBoolean(prefKey, value as Boolean)
            else -> throw IllegalArgumentException("Cannot set preference with type " + type)
        }
    }

    private fun buildRow(projection: Array<String>, key: String, value: Any): Array<Any> {
        val row = arrayOfNulls<Any>(projection.size)

        for (i in 0 until projection.size) {
            val col = projection[i]
            when (col) {
                Contract.COLUMN_KEY -> row[i] = key
                Contract.COLUMN_TYPE -> row[i] = value.getPreferenceType()
                Contract.COLUMN_VALUE -> row[i] = value.serialized()
                else -> throw IllegalArgumentException("Invalid column name: " + col)
            }
        }
        return row as Array<Any>
    }

    private fun parseUri(uri: Uri): PrefNameKeyPair {
        val match = uriMatcher.match(uri)
        if (match != PREFERENCE_ID && match != PREFERENCES_ID) {
            throw IllegalArgumentException("Invalid URI: " + uri)
        }

        val pathSegments = uri.pathSegments
        val prefName = pathSegments[0]
        val prefKey = if (match == PREFERENCE_ID) pathSegments[1] else ""
        return PrefNameKeyPair(prefName, prefKey)
    }

    private fun checkAccessOrThrow(prefName: String, prefKey: String, write: Boolean) {
        if (!checkAccess(prefName, prefKey, write)) {
            throw SecurityException("Insufficient permissions to access: $prefName/$prefKey")
        }
    }

    private fun getPreferencesByName(prefName: String): SharedPreferences {
        return preferences[prefName]
                ?: throw IllegalArgumentException("Unknown preference file name: $prefName")
    }

    private fun getPreferencesOrThrow(
        prefName: String,
        prefKey: String,
        write: Boolean
    ): SharedPreferences {
        checkAccessOrThrow(prefName, prefKey, write)
        return getPreferencesByName(prefName)
    }

    private fun getPreferenceUri(prefName: String, prefKey: String): Uri {
        val builder = baseUri.buildUpon().appendPath(prefName)
        if (isSingleKey(prefKey)) {
            builder.appendPath(prefKey)
        }
        return builder.build()
    }

    private fun isSingleKey(prefKey: String?): Boolean {
        return prefKey != null && prefKey.isNotEmpty()
    }

    private fun getKeyFromValues(values: ContentValues): String {
        var key: String? = values.getAsString(Contract.COLUMN_KEY)
        if (key == null) {
            key = ""
        }
        return key
    }

    private fun getKeyFromUriOrValues(
        nameKeyPair: PrefNameKeyPair,
        values: ContentValues
    ): String {
        val uriKey = nameKeyPair.key
        val valuesKey = getKeyFromValues(values)
        return if (uriKey.isNotEmpty() && valuesKey.isNotEmpty()) {
            // If a key is specified in both the URI and
            // ContentValues, they must match
            if (uriKey != valuesKey) {
                throw IllegalArgumentException("Conflicting keys specified in URI and ContentValues")
            }
            uriKey
        } else when {
            uriKey.isNotEmpty() -> uriKey
            valuesKey.isNotEmpty() -> valuesKey
            else -> ""
        }
    }

    private fun Any?.getPreferenceType(): Int {
        if (this == null) return Contract.TYPE_NULL

        return when(this) {
            is Boolean -> Contract.TYPE_BOOLEAN
            is Float -> Contract.TYPE_FLOAT
            is Int -> Contract.TYPE_INT
            is Long -> Contract.TYPE_LONG
            is String -> Contract.TYPE_STRING
            is Set<*> -> Contract.TYPE_STRING_SET
            else -> throw IllegalArgumentException("Unknown pref type ${this::class.java}")
        }
    }

    private companion object {
        private const val PREFERENCES_ID = 1
        private const val PREFERENCE_ID = 2
    }
}

private data class PrefNameKeyPair(
    val name: String,
    val key: String
)
