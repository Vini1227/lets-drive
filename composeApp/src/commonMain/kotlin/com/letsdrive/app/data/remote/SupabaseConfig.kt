package com.letsdrive.app.data.remote

import com.letsdrive.app.data.local.PlatformStorage
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

object SupabaseConfig {

    private const val KEY_SUPABASE_URL = "supabase_url"
    private const val KEY_SUPABASE_KEY = "supabase_anon_key"
    private const val DEFAULT_SUPABASE_URL = "https://your-project-id.supabase.co"
    private const val DEFAULT_SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"

    // Configurações do projeto Supabase (carregadas do armazenamento local quando salvo)
    var supabaseUrl: String = PlatformStorage.getString(KEY_SUPABASE_URL, DEFAULT_SUPABASE_URL)
    var supabaseKey: String = PlatformStorage.getString(KEY_SUPABASE_KEY, DEFAULT_SUPABASE_KEY)

    val isConfigured: Boolean
        get() = supabaseUrl.startsWith("https://") &&
            !supabaseUrl.contains("your-project-id") &&
            !supabaseKey.contains("dummy")

    private var _client: SupabaseClient? = null

    val client: SupabaseClient
        get() {
            val current = _client
            if (current != null) return current

            val created = createSupabaseClient(
                supabaseUrl = supabaseUrl,
                supabaseKey = supabaseKey
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
            }
            _client = created
            return created
        }

    fun updateCredentials(url: String, key: String) {
        supabaseUrl = url.trim().ifBlank { DEFAULT_SUPABASE_URL }
        supabaseKey = key.trim().ifBlank { DEFAULT_SUPABASE_KEY }
        PlatformStorage.putString(KEY_SUPABASE_URL, supabaseUrl)
        PlatformStorage.putString(KEY_SUPABASE_KEY, supabaseKey)
        _client = null // Recria o cliente com as novas credenciais
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val realtime get() = client.realtime
}
