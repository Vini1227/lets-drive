package com.letsdrive.app.data.local

/**
 * Armazenamento local multiplataforma (SharedPreferences no Android,
 * NSUserDefaults no iOS e localStorage no navegador).
 */
expect object PlatformStorage {
    fun getString(key: String, defaultValue: String = ""): String
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

