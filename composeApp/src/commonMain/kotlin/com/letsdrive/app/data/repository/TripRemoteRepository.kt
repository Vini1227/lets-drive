package com.letsdrive.app.data.repository

import com.letsdrive.app.data.model.ParticipantDto
import com.letsdrive.app.data.model.ParticipantLocationDto
import com.letsdrive.app.data.model.SeparationCheckResultDto
import com.letsdrive.app.data.model.TripDto
import com.letsdrive.app.data.model.UserProfileDto
import com.letsdrive.app.data.remote.SupabaseConfig
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TripRemoteRepository {

    private val postgrest get() = SupabaseConfig.postgrest

    /**
     * Cria uma nova viagem no banco PostgreSQL PostGIS.
     */
    suspend fun createTrip(trip: TripDto): Result<TripDto> {
        return runCatching {
            postgrest.from("trips")
                .insert(trip) {
                    select()
                }
                .decodeSingle<TripDto>()
        }
    }

    /**
     * Busca uma viagem ativa através do código de convite (Ex: #AB12CD34).
     */
    suspend fun getTripByInviteCode(inviteCode: String): Result<TripDto?> {
        return runCatching {
            val normalized = inviteCode.trim().uppercase().let {
                if (it.startsWith("#")) it else "#$it"
            }
            postgrest.from("trips")
                .select {
                    filter {
                        eq("invite_code", normalized)
                    }
                }
                .decodeSingleOrNull<TripDto>()
        }
    }

    /**
     * Busca uma viagem pelo ID para sincronizar limite de separação e status.
     */
    suspend fun getTripById(tripId: String): Result<TripDto?> {
        return runCatching {
            postgrest.from("trips")
                .select {
                    filter {
                        eq("id", tripId)
                    }
                }
                .decodeSingleOrNull<TripDto>()
        }
    }

    /**
     * Registra ou atualiza o participante na viagem sem duplicar o par (trip_id, user_id).
     */
    suspend fun joinTrip(participant: ParticipantDto): Result<ParticipantDto> {
        return runCatching {
            postgrest.from("participants")
                .upsert(participant) {
                    onConflict = "trip_id,user_id"
                    select()
                }
                .decodeSingle<ParticipantDto>()
        }
    }

    /**
     * Remove o participante da viagem ao sair ou encerrar a sala.
     */
    suspend fun leaveTrip(participantId: String): Result<Unit> {
        return runCatching {
            postgrest.from("participants")
                .delete {
                    filter {
                        eq("id", participantId)
                    }
                }
        }
    }

    /**
     * Lista todos os participantes vinculados à viagem.
     */
    suspend fun getTripParticipants(tripId: String): Result<List<ParticipantDto>> {
        return runCatching {
            postgrest.from("participants")
                .select {
                    filter {
                        eq("trip_id", tripId)
                    }
                }
                .decodeList<ParticipantDto>()
        }
    }

    /**
     * Lista todas as localizações geoespaciais atuais dos participantes da viagem.
     */
    suspend fun getTripLocations(tripId: String): Result<List<ParticipantLocationDto>> {
        return runCatching {
            postgrest.from("participant_locations")
                .select {
                    filter {
                        eq("trip_id", tripId)
                    }
                }
                .decodeList<ParticipantLocationDto>()
        }
    }

    /**
     * Atualiza a localização geoespacial do participante (PostGIS Point 4326).
     */
    suspend fun updateLocation(location: ParticipantLocationDto): Result<Unit> {
        return runCatching {
            postgrest.from("participant_locations")
                .upsert(location)
        }
    }

    /**
     * Atualiza o raio máximo de separação (`max_separation_meters`) no banco PostGIS.
     */
    suspend fun updateMaxSeparationMeters(tripId: String, maxSeparationMeters: Double): Result<Unit> {
        return runCatching {
            postgrest.from("trips")
                .update(
                    {
                        set("max_separation_meters", maxSeparationMeters)
                    }
                ) {
                    filter {
                        eq("id", tripId)
                    }
                }
        }
    }

    /**
     * Atualiza o status da viagem (`ACTIVE`, `COMPLETED`, `CANCELLED`).
     */
    suspend fun updateTripStatus(tripId: String, status: String): Result<Unit> {
        return runCatching {
            postgrest.from("trips")
                .update(
                    {
                        set("status", status)
                    }
                ) {
                    filter {
                        eq("id", tripId)
                    }
                }
        }
    }

    /**
     * Lista o histórico real de viagens criadas ou participadas pelo usuário no Supabase.
     */
    suspend fun getUserTrips(userId: String): Result<List<TripDto>> {
        return runCatching {
            val createdTrips = postgrest.from("trips")
                .select {
                    filter {
                        eq("creator_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<TripDto>()

            val participations = postgrest.from("participants")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<ParticipantDto>()

            val joinedIds = participations.map { it.tripId }.distinct()
            val joinedTrips = if (joinedIds.isNotEmpty()) {
                postgrest.from("trips")
                    .select {
                        filter {
                            isIn("id", joinedIds)
                        }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<TripDto>()
            } else {
                emptyList()
            }

            (createdTrips + joinedTrips)
                .distinctBy { it.id ?: it.inviteCode }
                .sortedByDescending { it.createdAt.orEmpty() }
        }
    }

    /**
     * Atualiza o perfil do participante na sala ativa em tempo real.
     */
    suspend fun updateParticipantProfile(
        participantId: String,
        displayName: String,
        vehicleLabel: String,
        role: String
    ): Result<Unit> {
        return runCatching {
            postgrest.from("participants")
                .update(
                    {
                        set("display_name", displayName)
                        set("vehicle_or_status_label", vehicleLabel)
                        set("role", role)
                    }
                ) {
                    filter {
                        eq("id", participantId)
                    }
                }
        }
    }

    /**
     * Salva ou atualiza o perfil persistente do usuário na tabela `user_profiles`.
     */
    suspend fun upsertUserProfile(profile: UserProfileDto): Result<Unit> {
        return runCatching {
            postgrest.from("user_profiles")
                .upsert(profile) {
                    onConflict = "user_id"
                }
        }
    }

    /**
     * Recupera o perfil persistente do usuário na tabela `user_profiles`.
     */
    suspend fun getUserProfile(userId: String): Result<UserProfileDto?> {
        return runCatching {
            postgrest.from("user_profiles")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<UserProfileDto>()
        }
    }

    /**
     * Executa a função geoespacial `check_trip_separation` no PostGIS (ST_Distance e ST_DWithin)
     * e retorna a lista de participantes que excederam o limite configurado de separação.
     */
    suspend fun checkTripSeparation(
        tripId: String,
        leaderParticipantId: String
    ): Result<List<SeparationCheckResultDto>> {
        return runCatching {
            val params = buildJsonObject {
                put("p_trip_id", tripId)
                put("p_leader_participant_id", leaderParticipantId)
            }
            postgrest.rpc("check_trip_separation", params)
                .decodeList<SeparationCheckResultDto>()
        }
    }

    /**
     * Fluxo contínuo de sincronização de posições da viagem ativa no Supabase.
     */
    fun observeTripLocations(tripId: String, intervalMs: Long = 3000L): Flow<List<ParticipantLocationDto>> = flow {
        while (true) {
            if (SupabaseConfig.isConfigured && tripId.isNotBlank() && !tripId.startsWith("trip-")) {
                val res = getTripLocations(tripId)
                res.getOrNull()?.let { emit(it) }
            }
            delay(intervalMs)
        }
    }
}
