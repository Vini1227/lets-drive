package com.letsdrive.app.state

import com.letsdrive.app.data.location.GeoSpatialCalculator
import com.letsdrive.app.data.location.PlatformLocationTracker
import com.letsdrive.app.data.model.*
import com.letsdrive.app.data.remote.SupabaseConfig
import com.letsdrive.app.data.repository.AuthRepository
import com.letsdrive.app.data.repository.TripRemoteRepository
import com.letsdrive.app.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.random.Random

enum class AppScreen {
    WELCOME,       // iPhone 17 - 1 (Logo Let's Drive + Entrar / Cadastrar)
    LOGIN,         // iPhone 17 - 2 (Formulário Login + Google)
    REGISTER,      // Tela de Cadastro completando o fluxo do iPhone 17 - 1 e 2
    HOME,          // iPhone 17 - 3 ("Eae Caio, pra onde vamos hoje?" + busca destino)
    JOIN_CODE,     // iPhone 17 - 8 ("Código da Viagem" + Ilustração + botão entrar)
    CONFIRM_ROUTE, // iPhone 17 - 4 (Origem + Destino + Limite Máx Distância + Confirmar Destino)
    ROOM_CREATED,  // iPhone 17 - 6 (Bottom sheet #AB12CD34 + QR Code + Iniciar/Cancelar)
    SHARED_MAP,    // iPhone 17 - 5 (Mapa Rota Neon + Carros + Alerta Distância + Ponto de Encontro)
    HISTORY,       // 30:82 ("Viagens Recentes" Concluída / Cancelada)
    SETTINGS,      // Aba Configurações (Raio máximo de separação, GPS, Modo Parado/Em Movimento)
    PROFILE        // Aba Você (Editar Nome "Caio", Veículo "Fiat Uno" ou Modo Aguardando em Casa)
}

data class AppUiState(
    val currentScreen: AppScreen = AppScreen.WELCOME,
    val userName: String = "Caio",
    val userHandle: String = "Username",
    val userVehicle: String = "Fiat Uno",
    val userEmail: String = "",
    val userRole: ParticipantRole = ParticipantRole.MOVING_VEHICLE,
    val maxSeparationDistanceKm: Double = 2.0,
    val isLocationPermissionGranted: Boolean = true,
    val isSharingLocation: Boolean = true,
    val isHardwareGpsActive: Boolean = false,
    val isSimulatingSeparation: Boolean = false,
    val focusedParticipantId: String? = "u1",
    val confirmedMeetingPointName: String? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val isSupabaseConnected: Boolean = false,
    val supabaseProjectUrl: String = "",
    val currentTrip: Trip = createDefaultTrip(
        origin = "Rua Massaranduba, 161",
        destination = "Praia da Palma, Alagoas",
        inviteCode = "#AB12CD34",
        maxDistKm = 2.0,
        isSeparated = false,
        userDisplayName = "Você",
        userVehicleName = "Fiat Uno",
        userRole = ParticipantRole.MOVING_VEHICLE,
        userLocation = LatLng(-9.6498, -35.7089),
        userSpeedKmh = 88,
        confirmedMeetingPointId = null
    ),
    val recentTrips: List<HistoricalTripItem> = listOf(
        HistoricalTripItem(
            id = "h1",
            dateLabel = "02/11/26",
            originAddress = "Rua massaranduba, 161",
            destinationAddress = "Rua da Palma 352, Alagoas",
            isCompleted = true
        ),
        HistoricalTripItem(
            id = "h2",
            dateLabel = "02/11/26",
            originAddress = "Rua massaranduba, 161",
            destinationAddress = "Rua da Palma 352, Alagoas",
            isCompleted = false
        ),
        HistoricalTripItem(
            id = "h3",
            dateLabel = "02/11/26",
            originAddress = "Rua massaranduba, 161",
            destinationAddress = "Rua da Palma 352, Alagoas",
            isCompleted = true
        ),
        HistoricalTripItem(
            id = "h4",
            dateLabel = "28/10/26",
            originAddress = "Av. Álvaro Otacílio, 2100",
            destinationAddress = "Barra de São Miguel, AL",
            isCompleted = true
        ),
        HistoricalTripItem(
            id = "h5",
            dateLabel = "19/10/26",
            originAddress = "Shopping Parque Maceió",
            destinationAddress = "Praia do Francês, AL",
            isCompleted = true
        ),
        HistoricalTripItem(
            id = "h6",
            dateLabel = "11/10/26",
            originAddress = "Rua Eng. Mário de Gusmão, 90",
            destinationAddress = "Maragogi, Alagoas",
            isCompleted = false
        ),
        HistoricalTripItem(
            id = "h7",
            dateLabel = "04/10/26",
            originAddress = "Aeroporto Zumbi dos Palmares",
            destinationAddress = "Pajuçara, Maceió",
            isCompleted = true
        )
    )
)

private fun createDefaultTrip(
    origin: String,
    destination: String,
    inviteCode: String,
    maxDistKm: Double,
    isSeparated: Boolean,
    userDisplayName: String = "Você",
    userVehicleName: String = "Fiat Uno",
    userRole: ParticipantRole = ParticipantRole.MOVING_VEHICLE,
    userLocation: LatLng = LatLng(-9.6498, -35.7089),
    userSpeedKmh: Int = 88,
    lucasOffsetProgress: Double = 0.0,
    confirmedMeetingPointId: String? = null
): Trip {
    val destinationCoords = LatLng(-9.5120, -35.6810)

    // Se o usuário escolheu "Aguardando em casa / no destino" (Regra #3 do Professor), ele fica fixo no destino
    val effectiveUserLocation = if (userRole == ParticipantRole.STATIONARY_HOST) {
        destinationCoords
    } else {
        userLocation
    }
    val effectiveUserSpeed = if (userRole == ParticipantRole.STATIONARY_HOST) 0 else userSpeedKmh
    val userDistToDest = if (userRole == ParticipantRole.STATIONARY_HOST) {
        0.0
    } else {
        GeoSpatialCalculator.distanceKm(effectiveUserLocation, destinationCoords)
    }
    val userEta = if (userRole == ParticipantRole.STATIONARY_HOST) {
        0
    } else {
        GeoSpatialCalculator.estimateEtaMinutes(userDistToDest, effectiveUserSpeed)
    }

    // Posição de Lucas: se estiver simulando desvio de rota, vai para a rota alternativa (~3.8 km de distância);
    // caso contrário, segue próximo na BR-101 (~0.9 km do líder)
    val lucasLocation = if (isSeparated) {
        LatLng(
            latitude = effectiveUserLocation.latitude + 0.0295 + (lucasOffsetProgress * 0.002),
            longitude = effectiveUserLocation.longitude - 0.0185
        )
    } else {
        LatLng(
            latitude = effectiveUserLocation.latitude + 0.0072 + (lucasOffsetProgress * 0.001),
            longitude = effectiveUserLocation.longitude + 0.0025
        )
    }

    // Cálculo Geoespacial Real (PostGIS ST_Distance / Haversine WGS-84)
    val referenceLeaderLoc = if (userRole == ParticipantRole.STATIONARY_HOST) {
        LatLng(-9.6498, -35.7089)
    } else {
        effectiveUserLocation
    }
    val lucasDistanceFromLeaderKm = GeoSpatialCalculator.distanceKm(referenceLeaderLoc, lucasLocation)
    val lucasDistToDestKm = GeoSpatialCalculator.distanceKm(lucasLocation, destinationCoords)
    val lucasSpeed = if (isSeparated) 82 else 86
    val lucasEta = GeoSpatialCalculator.estimateEtaMinutes(lucasDistToDestKm, lucasSpeed)

    // Verifica se ultrapassou o limite configurado pelo usuário (ST_DWithin)
    val isLucasAlert = GeoSpatialCalculator.isSeparatedBeyondThreshold(
        a = referenceLeaderLoc,
        b = lucasLocation,
        thresholdKm = maxDistKm
    )

    val convergenceCoords = GeoSpatialCalculator.computeConvergenceMeetingPoint(
        leader = referenceLeaderLoc,
        separated = lucasLocation,
        destination = destinationCoords
    )

    val userRoleLabel = when (userRole) {
        ParticipantRole.STATIONARY_HOST -> "No destino • Aguardando grupo"
        ParticipantRole.WALKING_TRANSIT -> "$userVehicleName • Em deslocamento"
        ParticipantRole.MOVING_VEHICLE -> "$userVehicleName • Em movimento"
    }

    val participants = listOf(
        Participant(
            id = "u1",
            displayName = userDisplayName,
            vehicleLabel = userRoleLabel,
            role = userRole,
            colorHex = if (userRole == ParticipantRole.STATIONARY_HOST) 0xFF10CEAB else 0xFF38F0FC,
            isCurrentUser = true,
            isHost = true,
            location = effectiveUserLocation,
            speedKmh = effectiveUserSpeed,
            distanceToDestinationKm = userDistToDest,
            distanceFromLeaderKm = 0.0,
            etaMinutes = userEta,
            lastUpdatedSecondsAgo = 1,
            status = if (userRole == ParticipantRole.STATIONARY_HOST) {
                ParticipantStatus.WAITING_AT_DESTINATION
            } else {
                ParticipantStatus.ON_ROUTE
            },
            routeNote = when {
                userRole == ParticipantRole.STATIONARY_HOST -> "Parado no destino (acompanhando chegada)"
                confirmedMeetingPointId != null -> "Rumo ao Ponto de Reencontro (Posto BR Paripueira)"
                else -> "BR-101 • Velocidade: ${effectiveUserSpeed} km/h"
            }
        ),
        Participant(
            id = "u2",
            displayName = "Lucas",
            vehicleLabel = "HB20 Prata • Em movimento",
            role = ParticipantRole.MOVING_VEHICLE,
            colorHex = if (isLucasAlert) 0xFFFF7E10 else 0xFF38F0FC,
            isCurrentUser = false,
            isHost = false,
            location = lucasLocation,
            speedKmh = lucasSpeed,
            distanceToDestinationKm = lucasDistToDestKm,
            distanceFromLeaderKm = lucasDistanceFromLeaderKm,
            etaMinutes = lucasEta,
            lastUpdatedSecondsAgo = 2,
            status = if (isLucasAlert) ParticipantStatus.SEPARATED else ParticipantStatus.ON_ROUTE,
            routeNote = when {
                confirmedMeetingPointId != null -> "Convergindo para Posto BR Paripueira (${lucasDistanceFromLeaderKm} km)"
                isLucasAlert -> "Afastou-se ${lucasDistanceFromLeaderKm} km (Limite: ${maxDistKm} km)"
                else -> "A ${lucasDistanceFromLeaderKm} km na BR-101 • ${lucasSpeed} km/h"
            }
        ),
        Participant(
            id = "u3",
            displayName = "Ana",
            vehicleLabel = "Em casa • Aguardando grupo",
            role = ParticipantRole.STATIONARY_HOST,
            colorHex = 0xFF10CEAB,
            isCurrentUser = false,
            isHost = false,
            location = destinationCoords,
            speedKmh = 0,
            distanceToDestinationKm = 0.0,
            distanceFromLeaderKm = userDistToDest,
            etaMinutes = 0,
            lastUpdatedSecondsAgo = 3,
            status = ParticipantStatus.WAITING_AT_DESTINATION,
            routeNote = "Parada no destino (acompanhando chegada)"
        )
    )

    val userToMpKm = GeoSpatialCalculator.distanceKm(effectiveUserLocation, convergenceCoords)
    val lucasToMpKm = GeoSpatialCalculator.distanceKm(lucasLocation, convergenceCoords)

    val meetingPoints = listOf(
        MeetingPointSuggestion(
            id = "mp1",
            name = "Posto BR Paripueira (AL-101)",
            description = "Ponto de reencontro seguro antes de $destination",
            address = "Rodovia AL-101 Norte, Trevo de Paripueira",
            coordinates = convergenceCoords,
            reason = "Convergência geoespacial entre a BR-101 e o acesso litorâneo",
            aiAdvisoryNote = "Lucas ultrapassou o limite configurado de $maxDistKm km (está a $lucasDistanceFromLeaderKm km via PostGIS ST_Distance). O Posto Paripueira fica na junção das duas rotas.",
            participantEtas = listOf(
                ParticipantMeetingEta(
                    participantId = "u1",
                    participantName = "$userDisplayName ($userVehicleName)",
                    distanceKm = userToMpKm,
                    etaMinutes = if (userRole == ParticipantRole.STATIONARY_HOST) 0 else GeoSpatialCalculator.estimateEtaMinutes(userToMpKm, effectiveUserSpeed)
                ),
                ParticipantMeetingEta(
                    participantId = "u2",
                    participantName = "Lucas (HB20)",
                    distanceKm = lucasToMpKm,
                    etaMinutes = GeoSpatialCalculator.estimateEtaMinutes(lucasToMpKm, lucasSpeed)
                ),
                ParticipantMeetingEta(
                    participantId = "u3",
                    participantName = "Ana (Aguardando no Destino)",
                    distanceKm = 0.0,
                    etaMinutes = 0
                )
            )
        )
    )

    val alert = if (isLucasAlert) {
        RouteSeparationAlert(
            separatedParticipantId = "u2",
            separatedParticipantName = "Lucas",
            currentDistanceKm = lucasDistanceFromLeaderKm,
            configuredThresholdKm = maxDistKm,
            detectedReason = "Distância geodésica (${lucasDistanceFromLeaderKm} km) ultrapassou o limite de ${maxDistKm} km (PostGIS ST_DWithin)",
            timestampLabel = "Tempo real"
        )
    } else null

    return Trip(
        id = "trip-maceio",
        name = "Rota até $destination",
        inviteCode = inviteCode,
        inviteLink = "https://letsdrive.app/join/${inviteCode.removePrefix("#")}",
        originAddress = origin,
        destinationName = destination,
        destinationAddress = destination,
        destinationCoordinates = destinationCoords,
        maxSeparationDistanceKm = maxDistKm,
        status = TripStatus.ACTIVE,
        participants = participants,
        activeSeparationAlert = alert,
        suggestedMeetingPoints = meetingPoints,
        selectedMeetingPointId = confirmedMeetingPointId ?: (if (isLucasAlert) "mp1" else null)
    )
}

class TripStateHolder {

    private val authRepo = AuthRepository()
    private val tripRepo = TripRemoteRepository()
    private val locationTracker = PlatformLocationTracker()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var activeRemoteTripId: String? = null
    private var activeRemoteParticipantId: String? = null
    private var telemetryTick: Int = 0

    private val _uiState = MutableStateFlow(
        AppUiState(
            isSupabaseConnected = SupabaseConfig.isConfigured,
            supabaseProjectUrl = SupabaseConfig.supabaseUrl
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        startRealtimeLocationAndGeospatialLoop()
    }

    /**
     * Etapa 2: Loop contínuo de rastreamento GPS multiplataforma + Sincronização PostGIS + Verificação de Distância.
     */
    private fun startRealtimeLocationAndGeospatialLoop() {
        scope.launch {
            locationTracker.observeLocationUpdates(intervalMs = 2500L).collect { sample ->
                telemetryTick++
                val currentState = _uiState.value

                if (!currentState.isSharingLocation) {
                    return@collect
                }

                val lucasWave = ((telemetryTick % 10) - 5) * 0.15
                val updatedTrip = createDefaultTrip(
                    origin = currentState.currentTrip.originAddress,
                    destination = currentState.currentTrip.destinationName,
                    inviteCode = currentState.currentTrip.inviteCode,
                    maxDistKm = currentState.maxSeparationDistanceKm,
                    isSeparated = currentState.isSimulatingSeparation,
                    userDisplayName = "Você (${currentState.userName})",
                    userVehicleName = currentState.userVehicle,
                    userRole = currentState.userRole,
                    userLocation = sample.coordinates,
                    userSpeedKmh = sample.speedKmh,
                    lucasOffsetProgress = lucasWave,
                    confirmedMeetingPointId = if (currentState.confirmedMeetingPointName != null) "mp1" else null
                )

                _uiState.update { state ->
                    state.copy(
                        isHardwareGpsActive = sample.isHardwareGps,
                        currentTrip = updatedTrip
                    )
                }

                // Se conectado ao Supabase PostGIS e em uma sala remota ativa, envia a coordenada EWKT e consulta ST_DWithin
                val remoteTripId = activeRemoteTripId
                val remotePartId = activeRemoteParticipantId
                if (SupabaseConfig.isConfigured && remoteTripId != null && remotePartId != null) {
                    val isAlertActive = updatedTrip.activeSeparationAlert != null
                    tripRepo.updateLocation(
                        ParticipantLocationDto(
                            participantId = remotePartId,
                            tripId = remoteTripId,
                            coords = GeoSpatialCalculator.toPostGisEwkt(sample.coordinates),
                            speedKmh = sample.speedKmh,
                            isSeparated = isAlertActive
                        )
                    )
                    tripRepo.checkTripSeparation(
                        tripId = remoteTripId,
                        leaderParticipantId = remotePartId
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen, authErrorMessage = null) }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authErrorMessage = null) }
    }

    fun loginWithEmail(emailInput: String, passwordInput: String) {
        val cleanEmail = emailInput.trim()
        val cleanUser = cleanEmail.substringBefore("@").ifBlank { "Caio" }

        if (!SupabaseConfig.isConfigured) {
            loginUser(cleanUser)
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
        scope.launch {
            val result = authRepo.signInWithEmail(cleanEmail, passwordInput)
            result.onSuccess { user ->
                val metaName = user?.userMetadata?.get("display_name")?.toString()?.trim('"') ?: cleanUser
                val metaVehicle = user?.userMetadata?.get("vehicle_label")?.toString()?.trim('"') ?: "Fiat Uno"
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        userName = metaName,
                        userHandle = metaName,
                        userVehicle = metaVehicle,
                        userEmail = cleanEmail,
                        currentScreen = AppScreen.HOME
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = err.message ?: "Erro ao entrar na conta"
                    )
                }
            }
        }
    }

    fun registerWithEmail(nameInput: String, emailInput: String, vehicleInput: String, passwordInput: String) {
        val cleanName = nameInput.trim().ifBlank { "Caio" }
        val cleanVehicle = vehicleInput.trim().ifBlank { "Fiat Uno" }
        val cleanEmail = emailInput.trim().ifBlank { "${cleanName.lowercase()}@letsdrive.app" }

        if (!SupabaseConfig.isConfigured) {
            updateProfile(cleanName, cleanVehicle, _uiState.value.userRole)
            navigateTo(AppScreen.HOME)
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null) }
        scope.launch {
            val result = authRepo.signUpWithEmail(cleanEmail, passwordInput, cleanName, cleanVehicle)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        userName = cleanName,
                        userHandle = cleanName,
                        userVehicle = cleanVehicle,
                        userEmail = cleanEmail,
                        currentScreen = AppScreen.HOME
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = err.message ?: "Erro ao criar conta no Supabase"
                    )
                }
            }
        }
    }

    fun updateSupabaseCredentials(url: String, key: String) {
        SupabaseConfig.updateCredentials(url, key)
        _uiState.update {
            it.copy(
                isSupabaseConnected = SupabaseConfig.isConfigured,
                supabaseProjectUrl = SupabaseConfig.supabaseUrl
            )
        }
    }

    fun loginUser(usernameInput: String) {
        _uiState.update {
            val clean = usernameInput.trim().ifBlank { "Caio" }
            it.copy(
                userName = clean,
                userHandle = clean,
                currentScreen = AppScreen.HOME
            )
        }
    }

    fun confirmDestinationAndGenerateRoom(
        origin: String,
        destination: String,
        maxDistanceKm: Double,
        role: ParticipantRole
    ) {
        val randomSuffix = Random.nextInt(1000, 9999)
        val code = "#AB12$randomSuffix"
        val cleanOrigin = origin.ifBlank { "Rua Massaranduba, 161" }
        val cleanDest = destination.ifBlank { "Praia da Palma, Alagoas" }

        _uiState.update { state ->
            val newHistoryEntry = HistoricalTripItem(
                id = "h-$randomSuffix",
                dateLabel = "Hoje",
                originAddress = cleanOrigin,
                destinationAddress = cleanDest,
                isCompleted = true
            )
            state.copy(
                userRole = role,
                maxSeparationDistanceKm = maxDistanceKm,
                confirmedMeetingPointName = null,
                recentTrips = listOf(newHistoryEntry) + state.recentTrips,
                currentTrip = createDefaultTrip(
                    origin = cleanOrigin,
                    destination = cleanDest,
                    inviteCode = code,
                    maxDistKm = maxDistanceKm,
                    isSeparated = false,
                    userDisplayName = "Você (${state.userName})",
                    userVehicleName = state.userVehicle,
                    userRole = role
                ),
                isSimulatingSeparation = false,
                currentScreen = AppScreen.ROOM_CREATED
            )
        }

        // Se o Supabase estiver conectado, grava a viagem e o anfitrião na base PostGIS
        if (SupabaseConfig.isConfigured) {
            scope.launch {
                val userId = authRepo.currentUser?.id ?: "00000000-0000-0000-0000-000000000001"
                val state = _uiState.value
                val createdTrip = tripRepo.createTrip(
                    TripDto(
                        inviteCode = code,
                        name = "Rota até $cleanDest",
                        originAddress = cleanOrigin,
                        destinationName = cleanDest,
                        destinationPoint = GeoSpatialCalculator.toPostGisEwkt(LatLng(-9.5120, -35.6810)),
                        maxSeparationMeters = maxDistanceKm * 1000.0,
                        creatorId = userId
                    )
                ).getOrNull()

                if (createdTrip?.id != null) {
                    activeRemoteTripId = createdTrip.id
                    val joinedPart = tripRepo.joinTrip(
                        ParticipantDto(
                            tripId = createdTrip.id,
                            userId = userId,
                            displayName = state.userName,
                            vehicleOrStatusLabel = state.userVehicle,
                            role = role.name
                        )
                    ).getOrNull()
                    activeRemoteParticipantId = joinedPart?.id
                }
            }
        }
    }

    fun joinTripWithCode(codeInput: String, role: ParticipantRole) {
        val formattedCode = codeInput.trim().uppercase().let {
            if (it.isBlank()) "#AB12CD34"
            else if (it.startsWith("#")) it else "#$it"
        }

        _uiState.update { state ->
            state.copy(
                userRole = role,
                confirmedMeetingPointName = null,
                currentTrip = createDefaultTrip(
                    origin = state.currentTrip.originAddress,
                    destination = state.currentTrip.destinationName,
                    inviteCode = formattedCode,
                    maxDistKm = state.maxSeparationDistanceKm,
                    isSeparated = false,
                    userDisplayName = "Você (${state.userName})",
                    userVehicleName = state.userVehicle,
                    userRole = role
                ),
                currentScreen = AppScreen.SHARED_MAP
            )
        }

        if (SupabaseConfig.isConfigured) {
            scope.launch {
                val foundTrip = tripRepo.getTripByInviteCode(formattedCode).getOrNull()
                if (foundTrip?.id != null) {
                    activeRemoteTripId = foundTrip.id
                    val userId = authRepo.currentUser?.id ?: "00000000-0000-0000-0000-000000000002"
                    val state = _uiState.value
                    val maxKm = (foundTrip.maxSeparationMeters / 1000.0).coerceAtLeast(0.5)
                    val joined = tripRepo.joinTrip(
                        ParticipantDto(
                            tripId = foundTrip.id,
                            userId = userId,
                            displayName = state.userName,
                            vehicleOrStatusLabel = state.userVehicle,
                            role = role.name
                        )
                    ).getOrNull()
                    activeRemoteParticipantId = joined?.id

                    _uiState.update { st ->
                        st.copy(
                            maxSeparationDistanceKm = maxKm,
                            currentTrip = createDefaultTrip(
                                origin = foundTrip.originAddress,
                                destination = foundTrip.destinationName,
                                inviteCode = foundTrip.inviteCode,
                                maxDistKm = maxKm,
                                isSeparated = st.isSimulatingSeparation,
                                userDisplayName = "Você (${st.userName})",
                                userVehicleName = st.userVehicle,
                                userRole = role
                            )
                        )
                    }
                }
            }
        }
    }

    fun updateMaxSeparationThreshold(newMaxKm: Double) {
        _uiState.update { state ->
            val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location ?: LatLng(-9.6498, -35.7089)
            val updatedTrip = createDefaultTrip(
                origin = state.currentTrip.originAddress,
                destination = state.currentTrip.destinationName,
                inviteCode = state.currentTrip.inviteCode,
                maxDistKm = newMaxKm,
                isSeparated = state.isSimulatingSeparation,
                userDisplayName = "Você (${state.userName})",
                userVehicleName = state.userVehicle,
                userRole = state.userRole,
                userLocation = currentUserLoc,
                confirmedMeetingPointId = if (state.confirmedMeetingPointName != null) "mp1" else null
            )
            state.copy(
                maxSeparationDistanceKm = newMaxKm,
                currentTrip = updatedTrip
            )
        }

        val remoteTripId = activeRemoteTripId
        if (SupabaseConfig.isConfigured && remoteTripId != null) {
            scope.launch {
                tripRepo.updateMaxSeparationMeters(remoteTripId, newMaxKm * 1000.0)
            }
        }
    }

    fun toggleRouteSeparationSimulation() {
        _uiState.update { state ->
            val nextSeparated = !state.isSimulatingSeparation
            val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location ?: LatLng(-9.6498, -35.7089)
            val updatedTrip = createDefaultTrip(
                origin = state.currentTrip.originAddress,
                destination = state.currentTrip.destinationName,
                inviteCode = state.currentTrip.inviteCode,
                maxDistKm = state.maxSeparationDistanceKm,
                isSeparated = nextSeparated,
                userDisplayName = "Você (${state.userName})",
                userVehicleName = state.userVehicle,
                userRole = state.userRole,
                userLocation = currentUserLoc,
                confirmedMeetingPointId = if (nextSeparated) state.confirmedMeetingPointName?.let { "mp1" } else null
            )
            state.copy(
                isSimulatingSeparation = nextSeparated,
                confirmedMeetingPointName = if (!nextSeparated) null else state.confirmedMeetingPointName,
                currentTrip = updatedTrip
            )
        }
    }

    fun confirmMeetingPoint(meetingPointName: String) {
        _uiState.update { state ->
            val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location ?: LatLng(-9.6498, -35.7089)
            val updatedTrip = createDefaultTrip(
                origin = state.currentTrip.originAddress,
                destination = state.currentTrip.destinationName,
                inviteCode = state.currentTrip.inviteCode,
                maxDistKm = state.maxSeparationDistanceKm,
                isSeparated = state.isSimulatingSeparation,
                userDisplayName = "Você (${state.userName})",
                userVehicleName = state.userVehicle,
                userRole = state.userRole,
                userLocation = currentUserLoc,
                confirmedMeetingPointId = "mp1"
            )
            state.copy(
                confirmedMeetingPointName = meetingPointName,
                currentTrip = updatedTrip
            )
        }
    }

    fun updateProfile(name: String, vehicle: String, role: ParticipantRole) {
        _uiState.update { state ->
            val newName = name.ifBlank { state.userName }
            val newVehicle = vehicle.ifBlank { state.userVehicle }
            val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location ?: LatLng(-9.6498, -35.7089)
            val updatedTrip = createDefaultTrip(
                origin = state.currentTrip.originAddress,
                destination = state.currentTrip.destinationName,
                inviteCode = state.currentTrip.inviteCode,
                maxDistKm = state.maxSeparationDistanceKm,
                isSeparated = state.isSimulatingSeparation,
                userDisplayName = "Você ($newName)",
                userVehicleName = newVehicle,
                userRole = role,
                userLocation = currentUserLoc,
                confirmedMeetingPointId = if (state.confirmedMeetingPointName != null) "mp1" else null
            )
            state.copy(
                userName = newName,
                userHandle = newName,
                userVehicle = newVehicle,
                userRole = role,
                currentTrip = updatedTrip
            )
        }
    }

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.update { it.copy(isSharingLocation = enabled) }
    }

    fun focusParticipant(participantId: String) {
        _uiState.update { it.copy(focusedParticipantId = participantId) }
    }
}
