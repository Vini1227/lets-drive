package com.letsdrive.app.model

enum class TripStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class ParticipantRole(val label: String) {
    MOVING_VEHICLE("Em movimento"),
    STATIONARY_HOST("Aguardando no local"),
    WALKING_TRANSIT("A pé / Transporte")
}

enum class ParticipantStatus {
    ON_ROUTE,
    SEPARATED,
    WAITING_AT_DESTINATION
}

data class LatLng(
    val latitude: Double,
    val longitude: Double
)

data class Participant(
    val id: String,
    val displayName: String,
    val vehicleLabel: String,
    val role: ParticipantRole = ParticipantRole.MOVING_VEHICLE,
    val colorHex: Long,
    val isCurrentUser: Boolean = false,
    val isHost: Boolean = false,
    val location: LatLng,
    val speedKmh: Int,
    val distanceToDestinationKm: Double,
    val distanceFromLeaderKm: Double = 0.0,
    val etaMinutes: Int,
    val lastUpdatedSecondsAgo: Int,
    val status: ParticipantStatus = ParticipantStatus.ON_ROUTE,
    val routeNote: String? = null
)

data class MeetingPointSuggestion(
    val id: String,
    val name: String,
    val description: String,
    val address: String,
    val coordinates: LatLng,
    val reason: String,
    val aiAdvisoryNote: String,
    val participantEtas: List<ParticipantMeetingEta>,
    val isConfirmedByUser: Boolean = false
)

data class ParticipantMeetingEta(
    val participantId: String,
    val participantName: String,
    val distanceKm: Double,
    val etaMinutes: Int
)

data class HistoricalTripItem(
    val id: String,
    val dateLabel: String,
    val originAddress: String,
    val destinationAddress: String,
    val isCompleted: Boolean
)

data class Trip(
    val id: String,
    val name: String,
    val inviteCode: String,
    val inviteLink: String,
    val originAddress: String,
    val destinationName: String,
    val destinationAddress: String,
    val destinationCoordinates: LatLng,
    val maxSeparationDistanceKm: Double = 2.0,
    val status: TripStatus,
    val participants: List<Participant>,
    val activeSeparationAlert: RouteSeparationAlert? = null,
    val suggestedMeetingPoints: List<MeetingPointSuggestion> = emptyList(),
    val selectedMeetingPointId: String? = null
)

data class RouteSeparationAlert(
    val separatedParticipantId: String,
    val separatedParticipantName: String,
    val currentDistanceKm: Double,
    val configuredThresholdKm: Double,
    val detectedReason: String,
    val timestampLabel: String
)
