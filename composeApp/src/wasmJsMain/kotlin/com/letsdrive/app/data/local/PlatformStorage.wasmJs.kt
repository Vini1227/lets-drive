package com.letsdrive.app.data.local

import kotlinx.browser.window

actual object PlatformStorage {
    private val memoryFallback = mutableMapOf<String, String>()

    actual fun getString(key: String, defaultValue: String): String {
        val stored = runCatching { window.localStorage.getItem(key) }.getOrNull()
        return stored ?: memoryFallback[key] ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        memoryFallback[key] = value
        runCatching { window.localStorage.setItem(key, value) }
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val raw = runCatching { window.localStorage.getItem(key) }.getOrNull()
            ?: memoryFallback[key]
        return when (raw) {
            "true" -> true
            "false" -> false
            else -> defaultValue
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        val raw = if (value) "true" else "false"
        memoryFallback[key] = raw
        runCatching { window.localStorage.setItem(key, raw) }
    }

    actual fun remove(key: String) {
        memoryFallback.remove(key)
        runCatching { window.localStorage.removeItem(key) }
    }
}

