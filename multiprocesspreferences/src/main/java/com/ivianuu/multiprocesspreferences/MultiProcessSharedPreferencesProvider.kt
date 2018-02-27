package com.ivianuu.multiprocesspreferences

import android.content.*
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import com.ivianuu.multiprocesspreferences.Contract.FIELD_KEY
import com.ivianuu.multiprocesspreferences.Contract.FIELD_VALUE
import com.ivianuu.multiprocesspreferences.Contract.PREFERENCES_ENTITY
import com.ivianuu.multiprocesspreferences.Contract.PREFERENCE_ENTITY
import com.ivianuu.multiprocesspreferences.Contract.PROJECTION
import com.ivianuu.multiprocesspreferences.Util.decodePath
import com.ivianuu.multiprocesspreferences.Util.encodePath
import com.ivianuu.multiprocesspreferences.Util.marshallSet
import com.ivianuu.multiprocesspreferences.Util.unmarshallSet
import org.json.JSONException
import java.util.*

class MultiProcessSharedPreferencesProvider : ContentProvider() {

    private val preferences = HashMap<String, SharedPreferences>()

    private lateinit var authority: String
    private lateinit var uriMatcher: UriMatcher
    private lateinit var contentUri: Uri

    override fun onCreate(): Boolean {
        authority = context.packageName + ".prefs"

        uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(
                authority,
                PREFERENCES_ENTITY + "/*/" + PREFERENCE_ENTITY,
                PREFERENCES_DATA
            )
            addURI(
                authority,
                "$PREFERENCES_ENTITY/*/$PREFERENCE_ENTITY/*",
                PREFERENCES_DATA_ID
            )
        }

        contentUri = Uri.parse("content://" + authority)

        return false
    }

    override fun query(
        uri: Uri, projection: Array<String>?,
        selection: String?, selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor? {

        var c: MatrixCursor? = null
        val match = uriMatcher.match(uri)
        val map: Map<String, *>
        when (match) {
            PREFERENCES_DATA -> {
                map = getSharedPreferences(uri).all
                c = MatrixCursor(PROJECTION)
                for (key in map.keys) {
                    val row = c.newRow()
                    row.add(key)
                    val value = map[key]
                    if (value is Set<*>) {
                        row.add(marshallSet(value as Set<String>))
                    } else {
                        row.add(value)
                    }
                }
            }
            PREFERENCES_DATA_ID -> {
                val key = decodePath(uri.pathSegments[3])
                map = getSharedPreferences(uri).all
                if (map.containsKey(key)) {
                    c = MatrixCursor(PROJECTION)
                    val row = c.newRow()
                    row.add(key)
                    val value = map[key]
                    if (value is Set<*>) {
                        row.add(marshallSet(value as Set<String>))
                    } else {
                        row.add(value)
                    }
                }
            }
        }

        if (c != null) {
            c.setNotificationUri(context.contentResolver, uri)
        }
        return c
    }

    override fun insert(uri: Uri, values: ContentValues): Uri? {
        var key: String? = null
        val match = uriMatcher.match(uri)
        var count = 0
        when (match) {
            PREFERENCES_DATA -> {
                val editor = getSharedPreferences(uri).edit()
                key = values.get(FIELD_KEY) as String
                val value = values.get(FIELD_VALUE)
                if (value != null) {
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Long -> editor.putLong(key, value)
                        is Int -> editor.putInt(key, value)
                        is Float -> editor.putFloat(key, value)
                        else -> // Test if the preference is a json array
                            try {
                                editor.putStringSet(key, unmarshallSet(value as String))
                            } catch (e: JSONException) {
                                editor.putString(key, value as String)
                            }
                    }
                } else {
                    editor.remove(key)
                }
                editor.apply()
                count = 1
            }
        }

        // Notify
        if (count > 0 && key != null) {
            val notifyUri = uri.buildUpon().appendPath(encodePath(key)).build()
            notifyChange(notifyUri)
            return notifyUri
        }
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        var count = 0
        when (uriMatcher.match(uri)) {
            PREFERENCES_DATA -> {
                count = getSharedPreferences(uri).all.size
                getSharedPreferences(uri).edit().clear().apply()
            }
            PREFERENCES_DATA_ID -> {
                val key = decodePath(uri.pathSegments[3])
                if (getSharedPreferences(uri).contains(key)) {
                    getSharedPreferences(uri).edit().remove(key).apply()
                    count = 0
                }
            }
        }

        if (count > 0) {
            notifyChange(uri)
        }
        return count
    }

    override fun update(
        uri: Uri, values: ContentValues,
        selection: String?, selectionArgs: Array<String>?
    ): Int {
        var count = 0
        val match = uriMatcher.match(uri)
        when (match) {
            PREFERENCES_DATA_ID -> {
                val editor = getSharedPreferences(uri).edit()
                val key = decodePath(uri.pathSegments[3])
                val value = values.get(FIELD_VALUE)
                if (value != null) {
                    when (value) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Long -> editor.putLong(key, value)
                        is Int -> editor.putInt(key, value)
                        is Float -> editor.putFloat(key, value)
                        else ->
                            // Test if the preference is a json array
                            try {
                                editor.putStringSet(key, unmarshallSet(value as String))
                            } catch (e: JSONException) {
                                editor.putString(key, value as String)
                            }
                    }
                } else {
                    editor.remove(key)
                }
                count = 1
                editor.apply()
            }
        }

        if (count > 0) {
            notifyChange(uri)
        }
        return count
    }

    override fun getType(uri: Uri): String? {
        val match = uriMatcher.match(uri)
        return when (match) {
            PREFERENCES_DATA -> "vnd.android.cursor.dir/" + PREFERENCES_ENTITY
            else -> "vnd.android.cursor.item/" + PREFERENCES_ENTITY
        }
    }

    private fun notifyChange(uri: Uri) {
        context.contentResolver.notifyChange(uri, null)
    }

    @Synchronized private fun getSharedPreferences(uri: Uri): SharedPreferences {
        val name = decodePath(uri.pathSegments[1])
        return preferences.getOrPut(name) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE)
        }
    }

    private companion object {
        private const val PREFERENCES_DATA = 1
        private const val PREFERENCES_DATA_ID = 2
    }
}