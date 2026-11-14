package com.letsdrive.app.data.local

import android.content.Context
import android.content.SharedPreferences
import com.letsdrive.app.data.location.AndroidLocationHolder

actual object PlatformStorage {
    private const val PREFS_NAME = "lets_drive_prefs"
    private val memoryFallback = mutableMapOf<String, Any>()

    private val prefs: SharedPreferences?
        get() = AndroidLocationHolder.appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    actual fun getString(key: String, defaultValue: String): String {
        val p = prefs ?: return (memoryFallback[key] as? String) ?: defaultValue
        return p.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        val p = prefs
        if (p != null) {
            p.edit().putString(key, value).apply()
        } else {
            memoryFallback[key] = value
        }
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val p = prefs ?: return (memoryFallback[key] as? Boolean) ?: defaultValue
        return p.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        val p = prefs
        if (p != null) {
            p.edit().putBoolean(key, value).apply()
        } else {
            memoryFallback[key] = value
        }
    }

    actual fun remove(key: String) {
        val p = prefs
        if (p != null) {
            p.edit().remove(key).apply()
        } else {
            memoryFallback.remove(key)
        }
    }
}

