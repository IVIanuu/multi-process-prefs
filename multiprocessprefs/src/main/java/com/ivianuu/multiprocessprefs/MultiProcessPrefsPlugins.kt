package com.ivianuu.multiprocessprefs

/**
 * Global configuration
 */
object MultiProcessPrefsPlugins {
    /**
     * Whether or not device protected storage should be used by default
     */
    var useDeviceProtectedStorage = false

    /**
     * Will be called anytime on [MultiProcessPrefsProvider.checkAccess]
     */
    var checkAccessHandler: ((name: String, key: String, write: Boolean) -> Boolean)? = null
}