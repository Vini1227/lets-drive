package com.letsdrive.app.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

object SupabaseConfig {

    // Configurações do projeto Supabase
    var supabaseUrl: String = "https://your-project-id.supabase.co"
    var supabaseKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.dummy_anon_key"

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
        supabaseUrl = url.trim()
        supabaseKey = key.trim()
        _client = null // Recria o cliente com as novas credenciais
    }

    val auth get() = client.auth
    val postgrest get() = client.postgrest
    val realtime get() = client.realtime
}
