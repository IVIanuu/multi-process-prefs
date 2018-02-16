package com.ivianuu.multiprocesspreferences

internal object Contract {
    const val COLUMN_KEY = "key"
    const val COLUMN_TYPE = "type"
    const val COLUMN_VALUE = "value"

    val ALL_COLUMNS =
        arrayOf(Contract.COLUMN_KEY, Contract.COLUMN_TYPE, Contract.COLUMN_VALUE)

    const val TYPE_BOOLEAN = 0
    const val TYPE_FLOAT = 1
    const val TYPE_INT = 2
    const val TYPE_LONG = 3
    const val TYPE_STRING = 4
    const val TYPE_STRING_SET = 5
    const val TYPE_NULL = 6
}