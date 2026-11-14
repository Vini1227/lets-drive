package com.letsdrive.app.state

import com.letsdrive.app.data.local.PlatformStorage
import com.letsdrive.app.data.location.GeoSpatialCalculator
import com.letsdrive.app.data.location.PlatformLocationTracker
import com.letsdrive.app.data.location.RouteSearchService
import com.letsdrive.app.data.model.*
import com.letsdrive.app.data.notification.PlatformNotifier
import com.letsdrive.app.data.remote.SupabaseConfig
import com.letsdrive.app.data.repository.AuthRepository
import com.letsdrive.app.data.repository.TripRemoteRepository
import com.letsdrive.app.data.share.PlatformShareAndQr
import com.letsdrive.app.data.share.QrCodeEncoder
import com.letsdrive.app.model.*
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
    val areNotificationsEnabled: Boolean = true,
    val isHardwareGpsActive: Boolean = false,
    val isSimulatingSeparation: Boolean = false,
    val focusedParticipantId: String? = "u1",
    val confirmedMeetingPointName: String? = null,
    val isAuthLoading: Boolean = false,
    val authErrorMessage: String? = null,
    val authInfoMessage: String? = null,
    val isSupabaseConnected: Boolean = false,
    val supabaseProjectUrl: String = "",
    val currentTrip: Trip = buildTripState(
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

private fun buildTripState(
    tripId: String = "trip-local",
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
    simOffsetProgress: Double = 0.0,
    confirmedMeetingPointId: String? = null,
    remoteParticipants: List<Participant> = emptyList(),
    remoteAlert: RouteSeparationAlert? = null,
    originCoords: LatLng = LatLng(-9.6498, -35.7089),
    destinationCoords: LatLng = LatLng(-9.5120, -35.6810),
    routeGeometry: List<LatLng> = emptyList(),
    intermediateStopAddress: String? = null,
    intermediateStopCoords: LatLng? = null
): Trip {

    // Se o usuário escolheu "Aguardando no local", ele permanece fixo no destino
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

    val referenceLeaderLoc = if (userRole == ParticipantRole.STATIONARY_HOST) {
        LatLng(-9.6498, -35.7089)
    } else {
        effectiveUserLocation
    }

    val userRoleLabel = when (userRole) {
        ParticipantRole.STATIONARY_HOST -> "No destino • Aguardando grupo"
        ParticipantRole.WALKING_TRANSIT -> "$userVehicleName • Em deslocamento"
        ParticipantRole.MOVING_VEHICLE -> "$userVehicleName • Em movimento"
    }

    val currentUserParticipant = Participant(
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
            confirmedMeetingPointId != null -> "Rumo ao Ponto de Reencontro"
            else -> "Em deslocamento • $effectiveUserSpeed km/h"
        }
    )

    // Lista real de outros participantes do Supabase; em teste individual sem membros, "Veículo Teste" se isSeparated=true
    val otherParticipants = if (remoteParticipants.isNotEmpty()) {
        remoteParticipants
    } else if (isSeparated) {
        val minDeltaLat = ((maxDistKm + 1.5) / 111.0).coerceAtLeast(0.0295)
        val simLocation = LatLng(
            latitude = referenceLeaderLoc.latitude + minDeltaLat + (simOffsetProgress * 0.002),
            longitude = referenceLeaderLoc.longitude - 0.0185
        )
        val simDistFromLeaderKm = GeoSpatialCalculator.distanceKm(referenceLeaderLoc, simLocation)
        val simDistToDestKm = GeoSpatialCalculator.distanceKm(simLocation, destinationCoords)
        val simSpeed = 82
        val simEta = GeoSpatialCalculator.estimateEtaMinutes(simDistToDestKm, simSpeed)
        val isSimAlert = GeoSpatialCalculator.isSeparatedBeyondThreshold(
            a = referenceLeaderLoc,
            b = simLocation,
            thresholdKm = maxDistKm
        )
        listOf(
            Participant(
                id = "sim-test",
                displayName = "Veículo Teste",
                vehicleLabel = "Simulação de Desvio • Em movimento",
                role = ParticipantRole.MOVING_VEHICLE,
                colorHex = if (isSimAlert) 0xFFFF7E10 else 0xFF38F0FC,
                isCurrentUser = false,
                isHost = false,
                location = simLocation,
                speedKmh = simSpeed,
                distanceToDestinationKm = simDistToDestKm,
                distanceFromLeaderKm = simDistFromLeaderKm,
                etaMinutes = simEta,
                lastUpdatedSecondsAgo = 1,
                status = if (isSimAlert) ParticipantStatus.SEPARATED else ParticipantStatus.ON_ROUTE,
                routeNote = if (isSimAlert) {
                    "Afastou-se $simDistFromLeaderKm km (Limite: $maxDistKm km)"
                } else {
                    "A $simDistFromLeaderKm km na rota • $simSpeed km/h"
                }
            )
        )
    } else {
        emptyList()
    }

    val participants = listOf(currentUserParticipant) + otherParticipants
    val separatedPeer = otherParticipants.firstOrNull { it.status == ParticipantStatus.SEPARATED }

    val activeAlert = remoteAlert ?: separatedPeer?.let { sep ->
        RouteSeparationAlert(
            separatedParticipantId = sep.id,
            separatedParticipantName = sep.displayName,
            currentDistanceKm = sep.distanceFromLeaderKm,
            configuredThresholdKm = maxDistKm,
            detectedReason = "Distância geodésica (${sep.distanceFromLeaderKm} km) ultrapassou o limite de $maxDistKm km (PostGIS ST_DWithin)",
            timestampLabel = "Tempo real"
        )
    }

    val meetingPoints = if (separatedPeer != null || activeAlert != null || confirmedMeetingPointId != null) {
        val convergenceCoords = GeoSpatialCalculator.computeConvergenceMeetingPoint(
            leader = referenceLeaderLoc,
            separated = separatedPeer?.location ?: referenceLeaderLoc,
            destination = destinationCoords
        )
        val participantEtas = participants.map { p ->
            if (p.role == ParticipantRole.STATIONARY_HOST) {
                ParticipantMeetingEta(
                    participantId = p.id,
                    participantName = "${p.displayName} (No destino)",
                    distanceKm = 0.0,
                    etaMinutes = 0
                )
            } else {
                val distToMp = GeoSpatialCalculator.distanceKm(p.location, convergenceCoords)
                ParticipantMeetingEta(
                    participantId = p.id,
                    participantName = "${p.displayName} (${p.vehicleLabel.substringBefore("•").trim()})",
                    distanceKm = distToMp,
                    etaMinutes = GeoSpatialCalculator.estimateEtaMinutes(distToMp, p.speedKmh)
                )
            }
        }
        listOf(
            MeetingPointSuggestion(
                id = "mp1",
                name = "Ponto de Convergência da Rota",
                description = "Ponto de reencontro seguro antes de $destination",
                address = "Junção estratégica rumo a $destination",
                coordinates = convergenceCoords,
                reason = "Convergência geoespacial entre o grupo e quem se afastou",
                aiAdvisoryNote = if (activeAlert != null) {
                    "${activeAlert.separatedParticipantName} ultrapassou o limite configurado de $maxDistKm km (está a ${activeAlert.currentDistanceKm} km via PostGIS ST_Distance). Este ponto fica no encontro das rotas."
                } else {
                    "Ponto de reencontro selecionado pelo grupo a caminho de $destination."
                },
                participantEtas = participantEtas
            )
        )
    } else {
        emptyList()
    }

    return Trip(
        id = tripId,
        name = "Rota até $destination",
        inviteCode = inviteCode,
        inviteLink = "https://letsdrive.app/join/${inviteCode.removePrefix("#")}",
        originAddress = origin,
        destinationName = destination,
        destinationAddress = destination,
        originCoordinates = originCoords,
        destinationCoordinates = destinationCoords,
        intermediateStopAddress = intermediateStopAddress,
        intermediateStopCoordinates = intermediateStopCoords,
        routeGeometry = routeGeometry,
        maxSeparationDistanceKm = maxDistKm,
        status = TripStatus.ACTIVE,
        participants = participants,
        activeSeparationAlert = activeAlert,
        suggestedMeetingPoints = meetingPoints,
        selectedMeetingPointId = confirmedMeetingPointId ?: (if (activeAlert != null) "mp1" else null)
    )
}

class TripStateHolder {

    private companion object {
        const val KEY_IS_LOGGED_IN = "auth_is_logged_in"
        const val KEY_USER_NAME = "auth_user_name"
        const val KEY_USER_HANDLE = "auth_user_handle"
        const val KEY_USER_VEHICLE = "auth_user_vehicle"
        const val KEY_USER_EMAIL = "auth_user_email"
        const val KEY_USER_ROLE = "auth_user_role"
        const val KEY_MAX_SEPARATION_KM = "pref_max_separation_km"
        const val KEY_NOTIFICATIONS_ENABLED = "pref_notifications_enabled"
        const val KEY_DEVICE_USER_ID = "auth_device_user_id"
        const val KEY_RECENT_TRIPS_JSON = "pref_recent_trips_json"
    }

    private val authRepo = AuthRepository()
    private val tripRepo = TripRemoteRepository()
    private val locationTracker = PlatformLocationTracker()
    private val routeSearchService = RouteSearchService()
    private val historyJson = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var authSessionJob: Job? = null
    private var routeRefreshJob: Job? = null
    private var activeRemoteTripId: String? = null
    private var activeRemoteParticipantId: String? = null
    private var cachedRemoteParticipants: List<Participant> = emptyList()
    private var cachedRemoteAlert: RouteSeparationAlert? = null
    private var lastKnownUserLocation: LatLng = LatLng(-9.6498, -35.7089)
    private var lastKnownSpeedKmh: Int = 88
    private var cachedOriginCoords: LatLng = LatLng(-9.6498, -35.7089)
    private var cachedDestCoords: LatLng = LatLng(-9.5120, -35.6810)
    private var cachedStopAddress: String? = null
    private var cachedStopCoords: LatLng? = null
    private var hasReverseGeocodedInitialGps: Boolean = false
    private var cachedRouteGeometry: List<LatLng> = routeSearchService.buildFallbackRouteGeometry(
        cachedOriginCoords,
        cachedDestCoords
    )
    private var telemetryTick: Int = 0
    private var lastNotifiedAlertKey: String? = null

    private val _uiState = MutableStateFlow(
        AppUiState(
            isSupabaseConnected = SupabaseConfig.isConfigured,
            supabaseProjectUrl = SupabaseConfig.supabaseUrl
        )
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        restorePersistedSession()
        if (_uiState.value.areNotificationsEnabled) {
            PlatformNotifier.requestPermissionIfNeeded()
        }
        observeAuthSessionStatus()
        startRealtimeLocationAndGeospatialLoop()
        refreshRouteGeometry(
            originText = _uiState.value.currentTrip.originAddress,
            destinationText = _uiState.value.currentTrip.destinationName
        )
        syncUserProfileFromRemote()
        syncTripHistoryFromRemote()
        PlatformShareAndQr.consumeInitialInviteCodeFromLaunch()?.let { launchCode ->
            joinTripWithCode(launchCode, _uiState.value.userRole)
        }
    }

    private fun refreshRouteGeometry(
        originText: String,
        destinationText: String,
        extraStopText: String? = cachedStopAddress,
        explicitDestCoords: LatLng? = null
    ) {
        routeRefreshJob?.cancel()
        routeRefreshJob = scope.launch {
            val resolvedOrigin = routeSearchService.resolveCoordinatesAsync(originText, default = lastKnownUserLocation)
            val resolvedDest = explicitDestCoords
                ?: routeSearchService.resolveCoordinatesAsync(destinationText, default = cachedDestCoords)
            val cleanStop = extraStopText?.trim()?.takeIf { it.isNotBlank() }
            val resolvedStop = cleanStop?.let {
                val midFallback = LatLng(
                    (resolvedOrigin.latitude + resolvedDest.latitude) / 2.0,
                    (resolvedOrigin.longitude + resolvedDest.longitude) / 2.0
                )
                routeSearchService.resolveCoordinatesAsync(it, default = midFallback)
            }
            val metrics = routeSearchService.calculateRouteByCoordinates(
                originCoords = resolvedOrigin,
                destCoords = resolvedDest,
                stopCoords = resolvedStop,
                stopAddress = cleanStop
            )
            cachedOriginCoords = metrics.originCoordinates
            cachedDestCoords = metrics.destinationCoordinates
            cachedStopAddress = metrics.intermediateStopAddress
            cachedStopCoords = metrics.intermediateStopCoordinates
            cachedRouteGeometry = metrics.routeGeometry
            _uiState.update { state ->
                state.copy(
                    currentTrip = rebuildTripForState(
                        state = state,
                        origin = originText,
                        destination = destinationText,
                        userLocation = lastKnownUserLocation
                    )
                )
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        val atIdx = trimmed.indexOf('@')
        if (atIdx <= 0) return false
        val domain = trimmed.substring(atIdx + 1)
        return domain.contains('.') && !domain.startsWith('.') && !domain.endsWith('.')
    }

    private fun metadataValue(user: UserInfo?, key: String): String? {
        return user?.userMetadata?.get(key)?.toString()?.trim('"')
            ?.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun persistProfileFields(
        name: String,
        handle: String,
        vehicle: String,
        role: ParticipantRole
    ) {
        PlatformStorage.putString(KEY_USER_NAME, name)
        PlatformStorage.putString(KEY_USER_HANDLE, handle)
        PlatformStorage.putString(KEY_USER_VEHICLE, vehicle)
        PlatformStorage.putString(KEY_USER_ROLE, role.name)
    }

    private fun persistSession(
        name: String,
        handle: String,
        vehicle: String,
        email: String,
        role: ParticipantRole
    ) {
        PlatformStorage.putBoolean(KEY_IS_LOGGED_IN, true)
        persistProfileFields(name, handle, vehicle, role)
        PlatformStorage.putString(KEY_USER_EMAIL, email)
    }

    private fun persistRecentTrips(trips: List<HistoricalTripItem>) {
        runCatching {
            PlatformStorage.putString(KEY_RECENT_TRIPS_JSON, historyJson.encodeToString(trips))
        }
    }

    private fun loadPersistedRecentTrips(): List<HistoricalTripItem>? {
        val raw = PlatformStorage.getString(KEY_RECENT_TRIPS_JSON)
        if (raw.isBlank()) return null
        return runCatching {
            historyJson.decodeFromString<List<HistoricalTripItem>>(raw)
        }.getOrNull()
    }

    private fun formatIsoDateToLabel(iso: String?): String {
        val raw = iso?.trim().orEmpty()
        return runCatching {
            if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
                val yy = raw.substring(2, 4)
                val mm = raw.substring(5, 7)
                val dd = raw.substring(8, 10)
                "$dd/$mm/$yy"
            } else {
                "Hoje"
            }
        }.getOrDefault("Hoje")
    }

    private fun upsertHistoryEntry(
        existing: List<HistoricalTripItem>,
        entry: HistoricalTripItem,
        moveToTop: Boolean
    ): List<HistoricalTripItem> {
        val matchIdx = existing.indexOfFirst { item ->
            item.id == entry.id ||
                (entry.inviteCode.isNotBlank() && item.inviteCode == entry.inviteCode)
        }
        return if (matchIdx == -1) {
            listOf(entry) + existing
        } else if (moveToTop) {
            listOf(entry) + existing.filterIndexed { index, _ -> index != matchIdx }
        } else {
            existing.mapIndexed { index, item -> if (index == matchIdx) entry else item }
        }
    }

    private fun mergeHistoryEntries(
        remote: List<HistoricalTripItem>,
        current: List<HistoricalTripItem>
    ): List<HistoricalTripItem> {
        val remoteIds = remote.map { it.id }.toSet()
        val remoteCodes = remote.mapNotNull { item -> item.inviteCode.takeIf { it.isNotBlank() } }.toSet()
        val remainingLocal = current.filter { local ->
            local.id !in remoteIds &&
                (local.inviteCode.isBlank() || local.inviteCode !in remoteCodes)
        }
        return remote + remainingLocal
    }

    private fun clearPersistedSession() {
        PlatformStorage.remove(KEY_IS_LOGGED_IN)
        PlatformStorage.remove(KEY_USER_NAME)
        PlatformStorage.remove(KEY_USER_HANDLE)
        PlatformStorage.remove(KEY_USER_VEHICLE)
        PlatformStorage.remove(KEY_USER_EMAIL)
        PlatformStorage.remove(KEY_USER_ROLE)
    }

    private fun getOrCreateUserId(): String {
        authRepo.currentUser?.id?.takeIf { it.isNotBlank() }?.let { return it }
        val saved = PlatformStorage.getString(KEY_DEVICE_USER_ID)
        if (saved.isNotBlank()) return saved
        val generated = generateUuidV4()
        PlatformStorage.putString(KEY_DEVICE_USER_ID, generated)
        return generated
    }

    private fun generateUuidV4(): String {
        val bytes = ByteArray(16) { Random.nextInt(256).toByte() }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x40).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()
        val hex = bytes.joinToString("") { b ->
            val v = b.toInt() and 0xFF
            val hi = (v ushr 4).toString(16)
            val lo = (v and 0x0F).toString(16)
            "$hi$lo"
        }
        return "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20, 32)}"
    }

    private fun rebuildTripForState(
        state: AppUiState,
        origin: String = state.currentTrip.originAddress,
        destination: String = state.currentTrip.destinationName,
        inviteCode: String = state.currentTrip.inviteCode,
        maxDistKm: Double = state.maxSeparationDistanceKm,
        isSeparated: Boolean = state.isSimulatingSeparation,
        userDisplayName: String = "Você (${state.userName})",
        userVehicleName: String = state.userVehicle,
        userRole: ParticipantRole = state.userRole,
        userLocation: LatLng = lastKnownUserLocation,
        userSpeedKmh: Int = lastKnownSpeedKmh,
        confirmedMeetingPointId: String? = if (state.confirmedMeetingPointName != null) "mp1" else null,
        simOffsetProgress: Double = 0.0
    ): Trip {
        return buildTripState(
            tripId = activeRemoteTripId ?: "trip-local",
            origin = origin,
            destination = destination,
            inviteCode = inviteCode,
            maxDistKm = maxDistKm,
            isSeparated = isSeparated,
            userDisplayName = userDisplayName,
            userVehicleName = userVehicleName,
            userRole = userRole,
            userLocation = userLocation,
            userSpeedKmh = userSpeedKmh,
            simOffsetProgress = simOffsetProgress,
            confirmedMeetingPointId = confirmedMeetingPointId,
            remoteParticipants = cachedRemoteParticipants,
            remoteAlert = cachedRemoteAlert,
            originCoords = cachedOriginCoords,
            destinationCoords = cachedDestCoords,
            routeGeometry = cachedRouteGeometry,
            intermediateStopAddress = cachedStopAddress,
            intermediateStopCoords = cachedStopCoords
        )
    }

    private suspend fun leaveRemoteParticipantIfNeeded(participantId: String?) {
        if (SupabaseConfig.isConfigured && !participantId.isNullOrBlank()) {
            tripRepo.leaveTrip(participantId)
        }
    }

    private suspend fun syncRemoteTripSnapshot(
        tripId: String,
        leaderParticipantId: String,
        currentUserLoc: LatLng
    ) {
        val participantsRes = tripRepo.getTripParticipants(tripId).getOrNull() ?: return
        val locationsRes = tripRepo.getTripLocations(tripId).getOrNull().orEmpty()
        val separationRes = tripRepo.checkTripSeparation(tripId, leaderParticipantId).getOrNull().orEmpty()
        val remoteTripDto = tripRepo.getTripById(tripId).getOrNull()

        val currentState = _uiState.value
        val remoteMaxKm = remoteTripDto
            ?.maxSeparationMeters
            ?.let { (it / 1000.0).coerceAtLeast(0.5) }
            ?: currentState.maxSeparationDistanceKm

        val locationsByParticipant = locationsRes.associateBy { it.participantId }
        val separationByParticipant = separationRes.associateBy { it.participantId }
        val myUserId = getOrCreateUserId()
        val destCoords = currentState.currentTrip.destinationCoordinates

        val mappedPeers = participantsRes
            .filter { dto ->
                val pid = dto.id
                (pid == null || pid != leaderParticipantId) && dto.userId != myUserId
            }
            .mapIndexed { idx, dto ->
                val pid = dto.id ?: "peer-${dto.userId}-$idx"
                val role = ParticipantRole.entries.firstOrNull { it.name == dto.role }
                    ?: ParticipantRole.MOVING_VEHICLE
                val locDto = locationsByParticipant[pid]
                val parsedCoords = locDto?.coords?.let { GeoSpatialCalculator.parsePostGisEwkt(it) }
                val effectiveCoords = when {
                    role == ParticipantRole.STATIONARY_HOST -> destCoords
                    parsedCoords != null -> parsedCoords
                    else -> currentUserLoc
                }
                val speed = if (role == ParticipantRole.STATIONARY_HOST) 0 else (locDto?.speedKmh ?: 0)
                val sepCheck = separationByParticipant[pid]
                val distFromLeaderKm = when {
                    role == ParticipantRole.STATIONARY_HOST -> {
                        GeoSpatialCalculator.distanceKm(currentUserLoc, destCoords)
                    }
                    sepCheck != null -> {
                        ((sepCheck.distanceMeters / 1000.0) * 10.0).roundToInt() / 10.0
                    }
                    parsedCoords != null -> {
                        GeoSpatialCalculator.distanceKm(currentUserLoc, parsedCoords)
                    }
                    else -> 0.0
                }
                val distToDestKm = if (role == ParticipantRole.STATIONARY_HOST) {
                    0.0
                } else {
                    GeoSpatialCalculator.distanceKm(effectiveCoords, destCoords)
                }
                val eta = if (role == ParticipantRole.STATIONARY_HOST) {
                    0
                } else {
                    GeoSpatialCalculator.estimateEtaMinutes(distToDestKm, speed)
                }
                val isExceeded = if (role == ParticipantRole.STATIONARY_HOST) {
                    false
                } else {
                    sepCheck?.exceededThreshold
                        ?: (locDto?.isSeparated == true || (parsedCoords != null && GeoSpatialCalculator.isSeparatedBeyondThreshold(currentUserLoc, parsedCoords, remoteMaxKm)))
                }
                val status = when {
                    role == ParticipantRole.STATIONARY_HOST -> ParticipantStatus.WAITING_AT_DESTINATION
                    isExceeded -> ParticipantStatus.SEPARATED
                    else -> ParticipantStatus.ON_ROUTE
                }
                val colorHex = when {
                    isExceeded -> 0xFFFF7E10
                    role == ParticipantRole.STATIONARY_HOST -> 0xFF8B5CF6
                    else -> 0xFF38F0FC
                }
                val note = when {
                    role == ParticipantRole.STATIONARY_HOST -> "Parado no destino (acompanhando chegada)"
                    parsedCoords == null -> "Conectado na sala • Aguardando sinal GPS"
                    isExceeded -> "Afastou-se $distFromLeaderKm km (Limite: $remoteMaxKm km)"
                    else -> "A $distFromLeaderKm km do grupo • $speed km/h"
                }

                Participant(
                    id = pid,
                    displayName = dto.displayName,
                    vehicleLabel = "${dto.vehicleOrStatusLabel} • ${role.label}",
                    role = role,
                    colorHex = colorHex,
                    isCurrentUser = false,
                    isHost = remoteTripDto?.creatorId == dto.userId,
                    location = effectiveCoords,
                    speedKmh = speed,
                    distanceToDestinationKm = distToDestKm,
                    distanceFromLeaderKm = distFromLeaderKm,
                    etaMinutes = eta,
                    lastUpdatedSecondsAgo = 2,
                    status = status,
                    routeNote = note
                )
            }

        cachedRemoteParticipants = mappedPeers
        val separatedPeer = mappedPeers.firstOrNull { it.status == ParticipantStatus.SEPARATED }
        cachedRemoteAlert = separatedPeer?.let { sep ->
            RouteSeparationAlert(
                separatedParticipantId = sep.id,
                separatedParticipantName = sep.displayName,
                currentDistanceKm = sep.distanceFromLeaderKm,
                configuredThresholdKm = remoteMaxKm,
                detectedReason = "Distância geodésica (${sep.distanceFromLeaderKm} km) ultrapassou o limite de $remoteMaxKm km (PostGIS ST_DWithin)",
                timestampLabel = "Tempo real"
            )
        }

        _uiState.update { state ->
            state.copy(
                maxSeparationDistanceKm = remoteMaxKm,
                currentTrip = rebuildTripForState(
                    state = state,
                    maxDistKm = remoteMaxKm,
                    userLocation = currentUserLoc
                )
            )
        }
        checkAndDispatchSeparationNotification(_uiState.value.currentTrip)
    }

    private fun restorePersistedSession() {
        val savedMaxKm = PlatformStorage.getString(KEY_MAX_SEPARATION_KM).toDoubleOrNull() ?: 2.0
        val savedNotifications = PlatformStorage.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        val isLoggedIn = PlatformStorage.getBoolean(KEY_IS_LOGGED_IN, false)
        val savedTrips = loadPersistedRecentTrips()

        val savedName = PlatformStorage.getString(KEY_USER_NAME, "Caio").ifBlank { "Caio" }
        val defaultHandle = if (isLoggedIn) savedName else "Username"
        val savedHandle = PlatformStorage.getString(KEY_USER_HANDLE, defaultHandle).ifBlank { defaultHandle }
        val savedVehicle = PlatformStorage.getString(KEY_USER_VEHICLE, "Fiat Uno").ifBlank { "Fiat Uno" }
        val savedEmail = PlatformStorage.getString(KEY_USER_EMAIL, "")
        val savedRoleRaw = PlatformStorage.getString(KEY_USER_ROLE, ParticipantRole.MOVING_VEHICLE.name)
        val savedRole = ParticipantRole.entries.firstOrNull { it.name == savedRoleRaw }
            ?: ParticipantRole.MOVING_VEHICLE

        _uiState.update { state ->
            val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation
            val updatedTrip = rebuildTripForState(
                state = state,
                maxDistKm = savedMaxKm,
                userDisplayName = "Você ($savedName)",
                userVehicleName = savedVehicle,
                userRole = savedRole,
                userLocation = currentUserLoc
            )
            state.copy(
                currentScreen = if (isLoggedIn) AppScreen.HOME else state.currentScreen,
                userName = savedName,
                userHandle = savedHandle,
                userVehicle = savedVehicle,
                userEmail = savedEmail,
                userRole = savedRole,
                areNotificationsEnabled = savedNotifications,
                maxSeparationDistanceKm = savedMaxKm,
                recentTrips = savedTrips ?: state.recentTrips,
                currentTrip = updatedTrip
            )
        }
    }

    fun syncTripHistoryFromRemote() {
        if (!SupabaseConfig.isConfigured) return
        scope.launch {
            val remoteTrips = tripRepo.getUserTrips(getOrCreateUserId()).getOrNull() ?: return@launch
            val remoteItems = remoteTrips.map { dto ->
                HistoricalTripItem(
                    id = dto.id ?: dto.inviteCode,
                    dateLabel = formatIsoDateToLabel(dto.createdAt),
                    originAddress = dto.originAddress,
                    destinationAddress = dto.destinationName,
                    isCompleted = dto.status != "CANCELLED",
                    inviteCode = dto.inviteCode
                )
            }
            _uiState.update { state ->
                val merged = mergeHistoryEntries(remoteItems, state.recentTrips)
                persistRecentTrips(merged)
                state.copy(recentTrips = merged)
            }
        }
    }

    private fun syncUserProfileFromRemote() {
        if (!SupabaseConfig.isConfigured) return
        scope.launch {
            val profile = tripRepo.getUserProfile(getOrCreateUserId()).getOrNull() ?: return@launch
            val cleanName = profile.displayName.trim().ifBlank { return@launch }
            val cleanVehicle = profile.vehicleLabel.trim().ifBlank { _uiState.value.userVehicle }
            val cleanRole = ParticipantRole.entries.firstOrNull { it.name == profile.defaultRole }
                ?: _uiState.value.userRole

            persistProfileFields(cleanName, cleanName, cleanVehicle, cleanRole)
            _uiState.update { state ->
                state.copy(
                    userName = cleanName,
                    userHandle = cleanName,
                    userVehicle = cleanVehicle,
                    userRole = cleanRole,
                    currentTrip = rebuildTripForState(
                        state = state,
                        userDisplayName = "Você ($cleanName)",
                        userVehicleName = cleanVehicle,
                        userRole = cleanRole,
                        userLocation = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation
                    )
                )
            }
        }
    }

    private fun observeAuthSessionStatus() {
        authSessionJob?.cancel()
        if (!SupabaseConfig.isConfigured) return

        authSessionJob = scope.launch {
            authRepo.sessionStatus.collect { status ->
                if (!SupabaseConfig.isConfigured) return@collect
                if (status is SessionStatus.Authenticated) {
                    val user = status.session.user
                    val current = _uiState.value
                    val email = user?.email.orEmpty().ifBlank { current.userEmail }
                    val fallbackName = email.substringBefore("@").ifBlank { current.userName }
                    val name = metadataValue(user, "display_name")
                        ?: metadataValue(user, "full_name")
                        ?: metadataValue(user, "name")
                        ?: fallbackName
                    val vehicle = metadataValue(user, "vehicle_label") ?: current.userVehicle
                    val role = metadataValue(user, "user_role")
                        ?.let { raw -> ParticipantRole.entries.firstOrNull { it.name == raw } }
                        ?: current.userRole

                    persistSession(name, name, vehicle, email, role)
                    _uiState.update { state ->
                        val nextScreen = if (
                            state.currentScreen == AppScreen.WELCOME ||
                            state.currentScreen == AppScreen.LOGIN ||
                            state.currentScreen == AppScreen.REGISTER
                        ) {
                            AppScreen.HOME
                        } else {
                            state.currentScreen
                        }
                        val currentUserLoc = state.currentTrip.participants.firstOrNull()?.location
                            ?: lastKnownUserLocation
                        val updatedTrip = rebuildTripForState(
                            state = state,
                            userDisplayName = "Você ($name)",
                            userVehicleName = vehicle,
                            userRole = role,
                            userLocation = currentUserLoc
                        )
                        state.copy(
                            isAuthLoading = false,
                            authErrorMessage = null,
                            authInfoMessage = null,
                            userName = name,
                            userHandle = name,
                            userVehicle = vehicle,
                            userEmail = email,
                            userRole = role,
                            currentScreen = nextScreen,
                            currentTrip = updatedTrip
                        )
                    }
                }
            }
        }
    }

    /**
     * Etapa 2: Loop contínuo de rastreamento GPS multiplataforma + Sincronização PostGIS + Verificação de Distância.
     */
    private fun startRealtimeLocationAndGeospatialLoop() {
        scope.launch {
            locationTracker.observeLocationUpdates(intervalMs = 2500L).collect { sample ->
                telemetryTick++
                val currentState = _uiState.value
                lastKnownUserLocation = sample.coordinates
                lastKnownSpeedKmh = sample.speedKmh

                if (currentState.isSharingLocation) {
                    val simWave = ((telemetryTick % 10) - 5) * 0.15
                    val updatedTrip = rebuildTripForState(
                        state = currentState,
                        userLocation = sample.coordinates,
                        userSpeedKmh = sample.speedKmh,
                        simOffsetProgress = simWave
                    )

                    _uiState.update { state ->
                        state.copy(
                            isHardwareGpsActive = sample.isHardwareGps,
                            currentTrip = updatedTrip
                        )
                    }
                    checkAndDispatchSeparationNotification(_uiState.value.currentTrip)
                }

                // Ao obter o primeiro sinal real de GPS, preenche automaticamente a origem via geocodificação reversa
                if (sample.isHardwareGps && !hasReverseGeocodedInitialGps) {
                    hasReverseGeocodedInitialGps = true
                    scope.launch {
                        val gpsOriginAddress = routeSearchService.reverseGeocode(sample.coordinates)
                        cachedOriginCoords = sample.coordinates
                        _uiState.update { st ->
                            if (st.currentTrip.originAddress == "Rua Massaranduba, 161") {
                                st.copy(
                                    currentTrip = rebuildTripForState(
                                        state = st,
                                        origin = gpsOriginAddress,
                                        userLocation = sample.coordinates,
                                        userSpeedKmh = sample.speedKmh
                                    )
                                )
                            } else st
                        }
                        if (_uiState.value.currentTrip.originAddress == gpsOriginAddress) {
                            refreshRouteGeometry(
                                originText = gpsOriginAddress,
                                destinationText = _uiState.value.currentTrip.destinationName,
                                extraStopText = cachedStopAddress
                            )
                        }
                    }
                }

                // Se conectado ao Supabase PostGIS e em uma sala remota ativa, envia posição e sincroniza grupo em tempo real
                val remoteTripId = activeRemoteTripId
                val remotePartId = activeRemoteParticipantId
                if (SupabaseConfig.isConfigured && remoteTripId != null && remotePartId != null) {
                    if (currentState.isSharingLocation) {
                        val isAlertActive = _uiState.value.currentTrip.activeSeparationAlert != null
                        tripRepo.updateLocation(
                            ParticipantLocationDto(
                                participantId = remotePartId,
                                tripId = remoteTripId,
                                coords = GeoSpatialCalculator.toPostGisEwkt(sample.coordinates),
                                speedKmh = sample.speedKmh,
                                isSeparated = isAlertActive
                            )
                        )
                    }
                    syncRemoteTripSnapshot(
                        tripId = remoteTripId,
                        leaderParticipantId = remotePartId,
                        currentUserLoc = sample.coordinates
                    )
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update {
            it.copy(
                currentScreen = screen,
                authErrorMessage = null,
                authInfoMessage = null
            )
        }
    }

    fun clearAuthError() {
        _uiState.update { it.copy(authErrorMessage = null, authInfoMessage = null) }
    }

    fun loginWithEmail(emailInput: String, passwordInput: String) {
        val cleanEmail = emailInput.trim()
        if (!isValidEmail(cleanEmail)) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Digite um e-mail válido (ex: nome@dominio.com).",
                    authInfoMessage = null
                )
            }
            return
        }
        if (passwordInput.length < 6) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "A senha deve ter pelo menos 6 caracteres.",
                    authInfoMessage = null
                )
            }
            return
        }

        val cleanUser = cleanEmail.substringBefore("@").ifBlank { "Caio" }

        if (!SupabaseConfig.isConfigured) {
            val current = _uiState.value
            persistSession(cleanUser, cleanUser, current.userVehicle, cleanEmail, current.userRole)
            _uiState.update {
                it.copy(
                    authErrorMessage = null,
                    authInfoMessage = null,
                    userName = cleanUser,
                    userHandle = cleanUser,
                    userEmail = cleanEmail,
                    currentScreen = AppScreen.HOME
                )
            }
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authInfoMessage = null) }
        scope.launch {
            val result = authRepo.signInWithEmail(cleanEmail, passwordInput)
            result.onSuccess { user ->
                val current = _uiState.value
                val metaName = metadataValue(user, "display_name") ?: cleanUser
                val metaVehicle = metadataValue(user, "vehicle_label") ?: current.userVehicle
                val metaRole = metadataValue(user, "user_role")
                    ?.let { raw -> ParticipantRole.entries.firstOrNull { it.name == raw } }
                    ?: current.userRole
                persistSession(metaName, metaName, metaVehicle, cleanEmail, metaRole)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = null,
                        authInfoMessage = null,
                        userName = metaName,
                        userHandle = metaName,
                        userVehicle = metaVehicle,
                        userEmail = cleanEmail,
                        userRole = metaRole,
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
        val cleanName = nameInput.trim()
        val cleanEmail = emailInput.trim()
        val cleanVehicle = vehicleInput.trim().ifBlank { "Fiat Uno" }

        if (cleanName.isBlank()) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Informe seu nome para continuar o cadastro.",
                    authInfoMessage = null
                )
            }
            return
        }
        if (!isValidEmail(cleanEmail)) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Digite um e-mail válido (ex: nome@dominio.com).",
                    authInfoMessage = null
                )
            }
            return
        }
        if (passwordInput.length < 6) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "A senha deve ter pelo menos 6 caracteres.",
                    authInfoMessage = null
                )
            }
            return
        }

        if (!SupabaseConfig.isConfigured) {
            persistSession(cleanName, cleanName, cleanVehicle, cleanEmail, _uiState.value.userRole)
            _uiState.update { it.copy(userEmail = cleanEmail, authErrorMessage = null, authInfoMessage = null) }
            updateProfile(cleanName, cleanVehicle, _uiState.value.userRole)
            navigateTo(AppScreen.HOME)
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authInfoMessage = null) }
        scope.launch {
            val result = authRepo.signUpWithEmail(cleanEmail, passwordInput, cleanName, cleanVehicle)
            result.onSuccess {
                val role = _uiState.value.userRole
                persistSession(cleanName, cleanName, cleanVehicle, cleanEmail, role)
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = null,
                        authInfoMessage = null,
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

    fun loginWithGoogle() {
        if (!SupabaseConfig.isConfigured) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Conecte o Supabase nas Configurações (após entrar com e-mail local) para autenticar com Google OAuth.",
                    authInfoMessage = null
                )
            }
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authInfoMessage = null) }
        scope.launch {
            val result = authRepo.signInWithGoogle()
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Conclua o login com Google na janela aberta para entrar."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = err.message ?: "Erro ao iniciar login com o Google"
                    )
                }
            }
        }
    }

    fun resetPassword(emailInput: String) {
        val cleanEmail = emailInput.trim()
        if (!isValidEmail(cleanEmail)) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Digite um e-mail válido para recuperar a senha.",
                    authInfoMessage = null
                )
            }
            return
        }

        if (!SupabaseConfig.isConfigured) {
            _uiState.update {
                it.copy(
                    authErrorMessage = "Conecte o Supabase nas Configurações para enviar o e-mail de redefinição.",
                    authInfoMessage = null
                )
            }
            return
        }

        _uiState.update { it.copy(isAuthLoading = true, authErrorMessage = null, authInfoMessage = null) }
        scope.launch {
            val result = authRepo.sendPasswordResetEmail(cleanEmail)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authInfoMessage = "Enviamos um link de redefinição de senha para $cleanEmail."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authErrorMessage = err.message ?: "Erro ao enviar e-mail de recuperação"
                    )
                }
            }
        }
    }

    fun logoutUser() {
        clearPersistedSession()
        val previousPartId = activeRemoteParticipantId
        activeRemoteTripId = null
        activeRemoteParticipantId = null
        cachedRemoteParticipants = emptyList()
        cachedRemoteAlert = null
        cachedStopAddress = null
        cachedStopCoords = null
        lastNotifiedAlertKey = null

        if (SupabaseConfig.isConfigured) {
            scope.launch {
                leaveRemoteParticipantIfNeeded(previousPartId)
                authRepo.signOut()
            }
        }

        _uiState.update { state ->
            val resetTrip = buildTripState(
                origin = state.currentTrip.originAddress,
                destination = state.currentTrip.destinationName,
                inviteCode = "#AB12CD34",
                maxDistKm = state.maxSeparationDistanceKm,
                isSeparated = false,
                userDisplayName = "Você",
                userVehicleName = "Fiat Uno",
                userRole = ParticipantRole.MOVING_VEHICLE,
                userLocation = lastKnownUserLocation,
                confirmedMeetingPointId = null
            )
            state.copy(
                currentScreen = AppScreen.WELCOME,
                userName = "Caio",
                userHandle = "Username",
                userVehicle = "Fiat Uno",
                userEmail = "",
                userRole = ParticipantRole.MOVING_VEHICLE,
                isAuthLoading = false,
                authErrorMessage = null,
                authInfoMessage = null,
                isSimulatingSeparation = false,
                confirmedMeetingPointName = null,
                currentTrip = resetTrip
            )
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
        observeAuthSessionStatus()
    }

    fun loginUser(usernameInput: String) {
        val clean = usernameInput.trim().ifBlank { "Caio" }
        val current = _uiState.value
        persistSession(clean, clean, current.userVehicle, current.userEmail, current.userRole)
        _uiState.update {
            it.copy(
                userName = clean,
                userHandle = clean,
                authErrorMessage = null,
                authInfoMessage = null,
                currentScreen = AppScreen.HOME
            )
        }
    }

    fun confirmDestinationAndGenerateRoom(
        origin: String,
        destination: String,
        maxDistanceKm: Double,
        role: ParticipantRole,
        extraStop: String? = null
    ) {
        val randomSuffix = Random.nextInt(1000, 9999)
        val code = "#AB12$randomSuffix"
        val cleanOrigin = origin.ifBlank { "Rua Massaranduba, 161" }
        val cleanDest = destination.ifBlank { "Praia da Palma, Alagoas" }
        val cleanStop = extraStop?.trim()?.takeIf { it.isNotBlank() }
        val previousPartId = activeRemoteParticipantId
        activeRemoteTripId = null
        activeRemoteParticipantId = null
        cachedRemoteParticipants = emptyList()
        cachedRemoteAlert = null
        cachedStopAddress = cleanStop
        lastNotifiedAlertKey = null

        val originCoords = routeSearchService.resolveCoordinates(cleanOrigin, default = lastKnownUserLocation)
        val destCoords = routeSearchService.resolveCoordinates(cleanDest, default = cachedDestCoords)
        val stopCoords = cleanStop?.let {
            routeSearchService.resolveCoordinates(
                it,
                default = LatLng(
                    (originCoords.latitude + destCoords.latitude) / 2.0,
                    (originCoords.longitude + destCoords.longitude) / 2.0
                )
            )
        }
        cachedOriginCoords = originCoords
        cachedDestCoords = destCoords
        cachedStopCoords = stopCoords
        cachedRouteGeometry = routeSearchService.buildFallbackRouteGeometry(originCoords, destCoords, stopCoords)

        _uiState.update { state ->
            val newHistoryEntry = HistoricalTripItem(
                id = "h-$randomSuffix",
                dateLabel = "Hoje",
                originAddress = cleanOrigin,
                destinationAddress = cleanDest,
                isCompleted = true,
                inviteCode = code
            )
            val updatedHistory = upsertHistoryEntry(state.recentTrips, newHistoryEntry, moveToTop = true)
            persistRecentTrips(updatedHistory)
            state.copy(
                userRole = role,
                maxSeparationDistanceKm = maxDistanceKm,
                confirmedMeetingPointName = null,
                recentTrips = updatedHistory,
                currentTrip = buildTripState(
                    origin = cleanOrigin,
                    destination = cleanDest,
                    inviteCode = code,
                    maxDistKm = maxDistanceKm,
                    isSeparated = false,
                    userDisplayName = "Você (${state.userName})",
                    userVehicleName = state.userVehicle,
                    userRole = role,
                    userLocation = lastKnownUserLocation,
                    userSpeedKmh = lastKnownSpeedKmh,
                    originCoords = cachedOriginCoords,
                    destinationCoords = cachedDestCoords,
                    intermediateStopAddress = cleanStop,
                    intermediateStopCoords = cachedStopCoords,
                    routeGeometry = cachedRouteGeometry
                ),
                isSimulatingSeparation = false,
                currentScreen = AppScreen.ROOM_CREATED
            )
        }

        scope.launch {
            leaveRemoteParticipantIfNeeded(previousPartId)
            val metrics = routeSearchService.calculateRoute(
                originText = cleanOrigin,
                destinationText = cleanDest,
                extraStopText = cleanStop
            )
            cachedOriginCoords = metrics.originCoordinates
            cachedDestCoords = metrics.destinationCoordinates
            cachedStopAddress = metrics.intermediateStopAddress ?: cleanStop
            cachedStopCoords = metrics.intermediateStopCoordinates ?: cachedStopCoords
            cachedRouteGeometry = metrics.routeGeometry

            _uiState.update { state ->
                val current = state.currentTrip
                if (current != null && current.inviteCode == code) {
                    state.copy(
                        currentTrip = current.copy(
                            originCoordinates = metrics.originCoordinates,
                            destinationCoordinates = metrics.destinationCoordinates,
                            intermediateStopAddress = cachedStopAddress,
                            intermediateStopCoordinates = cachedStopCoords,
                            routeGeometry = metrics.routeGeometry
                        )
                    )
                } else {
                    state
                }
            }

            // Se o Supabase estiver conectado, grava a viagem e o anfitrião na base PostGIS
            if (SupabaseConfig.isConfigured) {
                val userId = getOrCreateUserId()
                val state = _uiState.value
                val createdTrip = tripRepo.createTrip(
                    TripDto(
                        inviteCode = code,
                        name = "Rota até $cleanDest",
                        originAddress = cleanOrigin,
                        destinationName = cleanDest,
                        destinationPoint = GeoSpatialCalculator.toPostGisEwkt(metrics.destinationCoordinates),
                        maxSeparationMeters = maxDistanceKm * 1000.0,
                        creatorId = userId
                    )
                ).getOrNull()

                if (createdTrip?.id != null) {
                    activeRemoteTripId = createdTrip.id
                    _uiState.update { st ->
                        val remoteHistoryItem = HistoricalTripItem(
                            id = createdTrip.id,
                            dateLabel = formatIsoDateToLabel(createdTrip.createdAt),
                            originAddress = cleanOrigin,
                            destinationAddress = cleanDest,
                            isCompleted = createdTrip.status != "CANCELLED",
                            inviteCode = code
                        )
                        val updatedHistory = upsertHistoryEntry(st.recentTrips, remoteHistoryItem, moveToTop = false)
                        persistRecentTrips(updatedHistory)
                        val updatedTrip = if (st.currentTrip.inviteCode == code) {
                            st.currentTrip.copy(id = createdTrip.id)
                        } else {
                            st.currentTrip
                        }
                        st.copy(
                            recentTrips = updatedHistory,
                            currentTrip = updatedTrip
                        )
                    }
                    val joinedPart = tripRepo.joinTrip(
                        ParticipantDto(
                            tripId = createdTrip.id,
                            userId = userId,
                            displayName = state.userName,
                            vehicleOrStatusLabel = state.userVehicle,
                            role = role.name
                        )
                    ).getOrNull()
                    val joinedId = joinedPart?.id
                    activeRemoteParticipantId = joinedId
                    if (joinedId != null) {
                        tripRepo.updateLocation(
                            ParticipantLocationDto(
                                participantId = joinedId,
                                tripId = createdTrip.id,
                                coords = GeoSpatialCalculator.toPostGisEwkt(lastKnownUserLocation),
                                speedKmh = if (role == ParticipantRole.STATIONARY_HOST) 0 else lastKnownSpeedKmh,
                                isSeparated = false
                            )
                        )
                        syncRemoteTripSnapshot(createdTrip.id, joinedId, lastKnownUserLocation)
                    }
                }
            }
        }
    }

    fun shareCurrentTripInvite() {
        val trip = _uiState.value.currentTrip
        PlatformShareAndQr.shareTripInvite(
            inviteCode = trip.inviteCode,
            destinationName = trip.destinationName,
            inviteLink = trip.inviteLink
        )
    }

    fun joinTripWithCode(codeInput: String, role: ParticipantRole) {
        val formattedCode = QrCodeEncoder.extractInviteCode(codeInput)
            ?: codeInput.trim().uppercase().let {
                if (it.isBlank()) "#AB12CD34"
                else if (it.startsWith("#")) it else "#$it"
            }
        val previousPartId = activeRemoteParticipantId
        activeRemoteTripId = null
        activeRemoteParticipantId = null
        cachedRemoteParticipants = emptyList()
        cachedRemoteAlert = null
        cachedStopAddress = null
        cachedStopCoords = null
        lastNotifiedAlertKey = null

        _uiState.update { state ->
            state.copy(
                userRole = role,
                confirmedMeetingPointName = null,
                isSimulatingSeparation = false,
                currentTrip = buildTripState(
                    origin = state.currentTrip.originAddress,
                    destination = state.currentTrip.destinationName,
                    inviteCode = formattedCode,
                    maxDistKm = state.maxSeparationDistanceKm,
                    isSeparated = false,
                    userDisplayName = "Você (${state.userName})",
                    userVehicleName = state.userVehicle,
                    userRole = role,
                    userLocation = lastKnownUserLocation,
                    userSpeedKmh = lastKnownSpeedKmh
                ),
                currentScreen = AppScreen.SHARED_MAP
            )
        }

        if (SupabaseConfig.isConfigured) {
            scope.launch {
                leaveRemoteParticipantIfNeeded(previousPartId)
                val foundTrip = tripRepo.getTripByInviteCode(formattedCode).getOrNull()
                if (foundTrip?.id != null) {
                    activeRemoteTripId = foundTrip.id
                    val userId = getOrCreateUserId()
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
                    val joinedId = joined?.id
                    activeRemoteParticipantId = joinedId

                    _uiState.update { st ->
                        val parsedDestCoords = foundTrip.destinationPoint?.let { GeoSpatialCalculator.parsePostGisEwkt(it) }
                        if (parsedDestCoords != null) {
                            cachedDestCoords = parsedDestCoords
                            cachedRouteGeometry = routeSearchService.buildFallbackRouteGeometry(
                                cachedOriginCoords,
                                cachedDestCoords
                            )
                        }
                        val joinedHistoryItem = HistoricalTripItem(
                            id = foundTrip.id,
                            dateLabel = formatIsoDateToLabel(foundTrip.createdAt),
                            originAddress = foundTrip.originAddress,
                            destinationAddress = foundTrip.destinationName,
                            isCompleted = foundTrip.status != "CANCELLED",
                            inviteCode = foundTrip.inviteCode
                        )
                        val updatedHistory = upsertHistoryEntry(st.recentTrips, joinedHistoryItem, moveToTop = true)
                        persistRecentTrips(updatedHistory)
                        st.copy(
                            maxSeparationDistanceKm = maxKm,
                            recentTrips = updatedHistory,
                            currentTrip = buildTripState(
                                tripId = foundTrip.id,
                                origin = foundTrip.originAddress,
                                destination = foundTrip.destinationName,
                                inviteCode = foundTrip.inviteCode,
                                maxDistKm = maxKm,
                                isSeparated = st.isSimulatingSeparation,
                                userDisplayName = "Você (${st.userName})",
                                userVehicleName = st.userVehicle,
                                userRole = role,
                                userLocation = lastKnownUserLocation,
                                userSpeedKmh = lastKnownSpeedKmh,
                                remoteParticipants = cachedRemoteParticipants,
                                remoteAlert = cachedRemoteAlert,
                                originCoords = cachedOriginCoords,
                                destinationCoords = cachedDestCoords,
                                routeGeometry = cachedRouteGeometry
                            )
                        )
                    }

                    refreshRouteGeometry(
                        originText = foundTrip.originAddress,
                        destinationText = foundTrip.destinationName,
                        explicitDestCoords = foundTrip.destinationPoint?.let { GeoSpatialCalculator.parsePostGisEwkt(it) }
                    )

                    if (joinedId != null) {
                        tripRepo.updateLocation(
                            ParticipantLocationDto(
                                participantId = joinedId,
                                tripId = foundTrip.id,
                                coords = GeoSpatialCalculator.toPostGisEwkt(lastKnownUserLocation),
                                speedKmh = if (role == ParticipantRole.STATIONARY_HOST) 0 else lastKnownSpeedKmh,
                                isSeparated = false
                            )
                        )
                        syncRemoteTripSnapshot(foundTrip.id, joinedId, lastKnownUserLocation)
                    }
                }
            }
        }
    }

    fun updateMaxSeparationThreshold(newMaxKm: Double) {
        PlatformStorage.putString(KEY_MAX_SEPARATION_KM, newMaxKm.toString())
        _uiState.update { state ->
            state.copy(
                maxSeparationDistanceKm = newMaxKm,
                currentTrip = rebuildTripForState(
                    state = state,
                    maxDistKm = newMaxKm,
                    userLocation = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation
                )
            )
        }
        checkAndDispatchSeparationNotification(_uiState.value.currentTrip)

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
            val nextMeetingPointName = if (!nextSeparated) null else state.confirmedMeetingPointName
            val updatedTrip = rebuildTripForState(
                state = state,
                isSeparated = nextSeparated,
                userLocation = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation,
                confirmedMeetingPointId = if (nextSeparated) nextMeetingPointName?.let { "mp1" } else null
            )
            state.copy(
                isSimulatingSeparation = nextSeparated,
                confirmedMeetingPointName = nextMeetingPointName,
                currentTrip = updatedTrip
            )
        }
        checkAndDispatchSeparationNotification(_uiState.value.currentTrip)
    }

    fun confirmMeetingPoint(meetingPointName: String) {
        _uiState.update { state ->
            state.copy(
                confirmedMeetingPointName = meetingPointName,
                currentTrip = rebuildTripForState(
                    state = state,
                    userLocation = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation,
                    confirmedMeetingPointId = "mp1"
                )
            )
        }
        if (_uiState.value.areNotificationsEnabled) {
            PlatformNotifier.notifyMeetingPointConfirmed(
                meetingPointName = meetingPointName,
                destinationName = _uiState.value.currentTrip.destinationName
            )
        }
    }

    private fun checkAndDispatchSeparationNotification(trip: Trip) {
        val alert = trip.activeSeparationAlert
        if (alert == null) {
            lastNotifiedAlertKey = null
            return
        }
        if (!_uiState.value.areNotificationsEnabled) return

        val key = "${trip.id}:${alert.separatedParticipantId}"
        if (key != lastNotifiedAlertKey) {
            lastNotifiedAlertKey = key
            PlatformNotifier.notifySeparationAlert(
                participantName = alert.separatedParticipantName,
                distanceKm = alert.currentDistanceKm,
                thresholdKm = alert.configuredThresholdKm,
                meetingPointName = trip.suggestedMeetingPoints.firstOrNull()?.name
            )
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        PlatformStorage.putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled)
        _uiState.update { it.copy(areNotificationsEnabled = enabled) }
        if (enabled) {
            PlatformNotifier.requestPermissionIfNeeded()
        }
    }

    fun sendTestSeparationNotification() {
        PlatformNotifier.requestPermissionIfNeeded()
        val state = _uiState.value
        val activeAlert = state.currentTrip.activeSeparationAlert
        val thresholdKm = state.maxSeparationDistanceKm
        val participantName = activeAlert?.separatedParticipantName ?: "Veículo Teste"
        val distanceKm = activeAlert?.currentDistanceKm ?: (((thresholdKm + 1.4) * 10.0).roundToInt() / 10.0)
        PlatformNotifier.notifySeparationAlert(
            participantName = participantName,
            distanceKm = distanceKm,
            thresholdKm = thresholdKm,
            meetingPointName = state.currentTrip.suggestedMeetingPoints.firstOrNull()?.name
                ?: "Ponto de Convergência da Rota"
        )
    }

    fun completeCurrentTrip() {
        finishCurrentTrip(
            tripStatus = TripStatus.COMPLETED,
            remoteStatus = "COMPLETED",
            isCompleted = true,
            targetScreen = AppScreen.HISTORY
        )
    }

    fun cancelCurrentTrip() {
        cachedStopAddress = null
        cachedStopCoords = null
        finishCurrentTrip(
            tripStatus = TripStatus.CANCELLED,
            remoteStatus = "CANCELLED",
            isCompleted = false,
            targetScreen = AppScreen.HOME
        )
    }

    private fun finishCurrentTrip(
        tripStatus: TripStatus,
        remoteStatus: String,
        isCompleted: Boolean,
        targetScreen: AppScreen
    ) {
        val remoteTripId = activeRemoteTripId
        val previousPartId = activeRemoteParticipantId
        val finishedTrip = _uiState.value.currentTrip

        activeRemoteTripId = null
        activeRemoteParticipantId = null
        cachedRemoteParticipants = emptyList()
        cachedRemoteAlert = null
        lastNotifiedAlertKey = null

        _uiState.update { state ->
            val targetId = remoteTripId ?: finishedTrip.id
            val existingEntry = state.recentTrips.firstOrNull {
                it.id == targetId ||
                    (finishedTrip.inviteCode.isNotBlank() && it.inviteCode == finishedTrip.inviteCode)
            }
            val updatedEntry = existingEntry?.copy(isCompleted = isCompleted)
                ?: HistoricalTripItem(
                    id = targetId,
                    dateLabel = "Hoje",
                    originAddress = finishedTrip.originAddress,
                    destinationAddress = finishedTrip.destinationName,
                    isCompleted = isCompleted,
                    inviteCode = finishedTrip.inviteCode
                )
            val updatedHistory = upsertHistoryEntry(
                state.recentTrips,
                updatedEntry,
                moveToTop = existingEntry == null
            )
            persistRecentTrips(updatedHistory)
            state.copy(
                isSimulatingSeparation = false,
                confirmedMeetingPointName = null,
                recentTrips = updatedHistory,
                currentTrip = finishedTrip.copy(status = tripStatus),
                currentScreen = targetScreen
            )
        }

        if (SupabaseConfig.isConfigured) {
            scope.launch {
                if (remoteTripId != null) {
                    tripRepo.updateTripStatus(remoteTripId, remoteStatus)
                }
                leaveRemoteParticipantIfNeeded(previousPartId)
                syncTripHistoryFromRemote()
            }
        }
    }

    fun updateProfile(name: String, vehicle: String, role: ParticipantRole) {
        val current = _uiState.value
        val newName = name.trim().ifBlank { current.userName }
        val newVehicle = vehicle.trim().ifBlank { current.userVehicle }

        if (PlatformStorage.getBoolean(KEY_IS_LOGGED_IN, false)) {
            persistSession(newName, newName, newVehicle, current.userEmail, role)
        } else {
            persistProfileFields(newName, newName, newVehicle, role)
        }

        _uiState.update { state ->
            state.copy(
                userName = newName,
                userHandle = newName,
                userVehicle = newVehicle,
                userRole = role,
                currentTrip = rebuildTripForState(
                    state = state,
                    userDisplayName = "Você ($newName)",
                    userVehicleName = newVehicle,
                    userRole = role,
                    userLocation = state.currentTrip.participants.firstOrNull()?.location ?: lastKnownUserLocation
                )
            )
        }

        if (SupabaseConfig.isConfigured) {
            val activePartId = activeRemoteParticipantId
            scope.launch {
                tripRepo.upsertUserProfile(
                    UserProfileDto(
                        userId = getOrCreateUserId(),
                        displayName = newName,
                        vehicleLabel = newVehicle,
                        defaultRole = role.name
                    )
                )
                if (activePartId != null) {
                    tripRepo.updateParticipantProfile(
                        participantId = activePartId,
                        displayName = newName,
                        vehicleLabel = newVehicle,
                        role = role.name
                    )
                }
                if (authRepo.currentUser != null) {
                    authRepo.updateUserMetadata(newName, newVehicle, role.name)
                }
            }
        }
    }

    fun toggleLocationSharing(enabled: Boolean) {
        _uiState.update { it.copy(isSharingLocation = enabled) }
    }

    fun focusParticipant(participantId: String) {
        _uiState.update { it.copy(focusedParticipantId = participantId) }
    }
}
