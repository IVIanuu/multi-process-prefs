package com.ivianuu.multiprocesspreferences

import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import org.json.JSONArray
import java.util.*

/**
 * @author Manuel Wrage (IVIanuu)
 */
internal object Util {

    fun marshallSet(set: Set<String>): String {
        val array = JSONArray()
        for (value in set) {
            array.put(value)
        }
        return array.toString()
    }

    fun unmarshallSet(value: String): Set<String> {
        val array = JSONArray(value)
        val size = array.length()
        val set = HashSet<String>(size)
        (0 until size).mapTo(set) { array.getString(it) }
        return set
    }

    fun encodePath(path: String): String {
        return String(Base64.encode(path.toByteArray(), Base64.NO_WRAP))
    }

    fun decodePath(path: String): String {
        return String(Base64.decode(path.toByteArray(), Base64.NO_WRAP))
    }

    fun resolveUri(contentUri: Uri, key: String?, prefFileName: String): Uri {
        var builder: Uri.Builder = contentUri.buildUpon()
            .appendPath(Contract.PREFERENCES_ENTITY)
            .appendPath(encodePath(prefFileName))
            .appendPath(Contract.PREFERENCE_ENTITY)
        if (key != null && key.isNotEmpty()) {
            builder = builder.appendPath(encodePath(key))
        }
        return builder.build()
    }
    
}