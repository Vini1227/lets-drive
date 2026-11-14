package com.letsdrive.app.data.repository

import com.letsdrive.app.data.remote.SupabaseConfig
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val auth = SupabaseConfig.auth

    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus

    val currentUser: UserInfo?
        get() = auth.currentUserOrNull()

    /**
     * Cadastro real via Supabase Auth com e-mail, senha e metadados de perfil (nome e veículo).
     */
    suspend fun signUpWithEmail(
        emailInput: String,
        passwordInput: String,
        displayName: String,
        vehicleLabel: String
    ): Result<UserInfo?> {
        return runCatching {
            auth.signUpWith(Email) {
                email = emailInput.trim()
                password = passwordInput
                data = buildJsonObject {
                    put("display_name", displayName.trim().ifBlank { "Caio" })
                    put("vehicle_label", vehicleLabel.trim().ifBlank { "Fiat Uno" })
                }
            }
            auth.currentUserOrNull()
        }
    }

    /**
     * Login real via Supabase Auth com e-mail e senha.
     */
    suspend fun signInWithEmail(
        emailInput: String,
        passwordInput: String
    ): Result<UserInfo?> {
        return runCatching {
            auth.signInWith(Email) {
                email = emailInput.trim()
                password = passwordInput
            }
            auth.currentUserOrNull()
        }
    }

    /**
     * Encerra a sessão do usuário.
     */
    suspend fun signOut(): Result<Unit> {
        return runCatching {
            auth.signOut()
        }
    }
}

