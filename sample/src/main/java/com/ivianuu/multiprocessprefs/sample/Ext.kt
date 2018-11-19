package com.ivianuu.multiprocessprefs.sample

import android.util.Log

fun Any.d(message: () -> String) {
    Log.d("MultiProcessTest: ${this::class.java.simpleName}", message())
}