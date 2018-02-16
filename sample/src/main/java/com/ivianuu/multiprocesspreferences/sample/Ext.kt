package com.ivianuu.multiprocesspreferences.sample

import android.util.Log

fun Any.d(message: () -> String) {
    Log.d(this::class.java.simpleName, message())
}