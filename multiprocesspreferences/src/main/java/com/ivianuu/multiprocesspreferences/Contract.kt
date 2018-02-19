package com.ivianuu.multiprocesspreferences

internal object Contract {
    const val AUTHORITY = "com.ivianuu.multiprocesspreferences.prefs"

    const val COLUMN_KEY = "key"
    const val COLUMN_TYPE = "type"
    const val COLUMN_VALUE = "value"

    val ALL_COLUMNS =
        arrayOf(
            COLUMN_KEY,
            COLUMN_TYPE,
            COLUMN_VALUE
        )

    const val TYPE_BOOLEAN = 0
    const val TYPE_FLOAT = 1
    const val TYPE_INT = 2
    const val TYPE_LONG = 3
    const val TYPE_STRING = 4
    const val TYPE_STRING_SET = 5
    const val TYPE_NULL = 6
}