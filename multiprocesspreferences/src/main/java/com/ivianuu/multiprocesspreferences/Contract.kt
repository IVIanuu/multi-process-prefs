package com.ivianuu.multiprocesspreferences

/**
 * @author Manuel Wrage (IVIanuu)
 */
internal object Contract {

    const val FIELD_KEY = "key"
    const val FIELD_VALUE = "value"
    val PROJECTION = arrayOf(FIELD_KEY, FIELD_VALUE)

    const val PREFERENCES_ENTITY = "preferences"
    const val PREFERENCE_ENTITY = "preference"
}