package com.ivianuu.multiprocesspreferences.sample

import com.ivianuu.multiprocesspreferences.MultiProcessPreferenceProvider

class MyPreferenceProvider : MultiProcessPreferenceProvider(AUTHORITY, arrayOf(PREF_NAME)) {
    companion object {
        const val AUTHORITY = "com.ivianuu.multiprocesspreferences.sample.prefs"
        const val PREF_NAME = "prefs"
    }
}