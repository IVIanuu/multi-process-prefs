package com.ivianuu.multiprocesspreferences

internal fun Any.serialized(): Any {
    return when (this) {
        is Boolean -> if (this) 1 else 0
        is Set<*> -> serializedToString()
        else -> this
    }
}

internal fun Any?.deserialized(expectedType: Int): Any? {
    return try {
        when(expectedType) {
            Contract.TYPE_NULL -> null
            Contract.TYPE_BOOLEAN -> this == 1
            Contract.TYPE_FLOAT -> this
            Contract.TYPE_INT -> this
            Contract.TYPE_LONG -> this
            Contract.TYPE_STRING -> this
            Contract.TYPE_STRING_SET -> (this as String).deserializedToStringSet()
            else -> throw IllegalArgumentException("Unknown type: " + expectedType)
        }
    } catch (e: ClassCastException) {
        throw IllegalArgumentException("Expected type $expectedType, got $this", e)
    }
}

fun Set<*>.serializedToString(): String {
    val sb = StringBuilder()
    for (value in (this as Set<String>)) {
        sb.append(value.replace("\\", "\\\\").replace(";", "\\;"))
        sb.append(';')
    }

    return sb.toString()
}

fun String.deserializedToStringSet(): Set<String> {
    val stringSet = HashSet<String>()
    val sb = StringBuilder()
    var i = 0

    while (i < length) {
        val c = get(i)
        when (c) {
            '\\' -> {
                val next = get(++i)
                sb.append(next)
            }
            ';' -> {
                stringSet.add(sb.toString())
                sb.setLength(0)
            }
            else -> sb.append(c)
        }
        ++i
    }

    return stringSet
}