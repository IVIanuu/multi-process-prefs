package com.ivianuu.multiprocessprefs

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

class MultiProcessPrefsProvider : ContentProvider(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val preferences =
        mutableMapOf<String, SharedPreferences>()

    private val authority by lazy { "${context!!.packageName}.prefs" }
    private val uriMatcher by lazy {
        UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(
                authority,
                "$PREFS_NAME/*/",
                TYPE_ALL
            )
            addURI(
                authority,
                "$PREFS_NAME/*/$PREF_KEY/*",
                TYPE_ENTRY
            )
        }
    }
    private val contentUri by lazy { Uri.parse("content://$authority") }

    override fun onCreate() = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val type = uriMatcher.match(uri)
        val map = getSharedPreferences(uri).all

        return when (type) {
            TYPE_ALL -> {
                MatrixCursor(PROJECTION).apply {
                    map.forEach { (key, value) ->
                        newRow()
                            .add(key)
                            .add(value!!.prefType.key)
                            .add(value.serialize())
                    }
                }
            }
            TYPE_ENTRY -> {
                val key = uri.pathSegments[3]
                if (map.containsKey(key)) {
                    val value = map[key]
                    MatrixCursor(PROJECTION).apply {
                        newRow()
                            .add(key)
                            .add(value!!.prefType.key)
                            .add(value.serialize())
                    }
                } else {
                    null
                }
            }
            else -> {
                throw IllegalArgumentException("unsupported operation $uri")
            }
        }?.apply { setNotificationUri(context!!.contentResolver, uri) }
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        throw IllegalArgumentException("unsupported operation use update instead")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var count = 0
        when (uriMatcher.match(uri)) {
            TYPE_ALL -> {
                count = getSharedPreferences(uri).all.size
                getSharedPreferences(uri).edit().clear().apply()
            }
            TYPE_ENTRY -> {
                val key = uri.pathSegments[3]
                if (getSharedPreferences(uri).contains(key)) {
                    getSharedPreferences(uri).edit().remove(key).apply()
                    count = 0
                }
            }
        }

        return count
    }

    override fun update(
        uri: Uri,
        values: ContentValues,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        if (uriMatcher.match(uri) != TYPE_ENTRY) {
            throw IllegalArgumentException("unsupported operation $uri, $values")
        }

        val editor = getSharedPreferences(uri).edit()

        val key = values.getAsString(COLUMN_KEY)
        val prefType = values.getAsString(COLUMN_TYPE).toPrefType()
        val value = values.getAsString(COLUMN_VALUE).deserialize(prefType)

        editor.putAny(key, value).apply()

        return 1
    }

    override fun getType(uri: Uri): String? = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        val name = preferences.toList()
            .firstOrNull { it.second == sharedPreferences }
            ?.first

        if (name != null) {
            val uri = getUri(contentUri, key, name)
            context!!.contentResolver.notifyChange(uri, null)
        }
    }

    @Synchronized
    private fun getSharedPreferences(uri: Uri): SharedPreferences {
        val name = uri.pathSegments[1]
        return preferences.getOrPut(name) {
            context!!.getSharedPreferences(name, Context.MODE_PRIVATE).apply {
                registerOnSharedPreferenceChangeListener(this@MultiProcessPrefsProvider)
            }
        }
    }

    private companion object {
        private const val TYPE_ALL = 1
        private const val TYPE_ENTRY = 2
    }
}