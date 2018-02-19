package com.ivianuu.multiprocesspreferences

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