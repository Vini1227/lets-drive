package com.letsdrive.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripDto(
    val id: String? = null,
    @SerialName("invite_code") val inviteCode: String,
    val name: String,
    @SerialName("origin_address") val originAddress: String,
    @SerialName("destination_name") val destinationName: String,
    @SerialName("destination_point") val destinationPoint: String? = null,
    @SerialName("max_separation_meters") val maxSeparationMeters: Double = 2000.0,
    @SerialName("creator_id") val creatorId: String,
    val status: String = "ACTIVE",
    @SerialName("selected_meeting_point_id") val selectedMeetingPointId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ParticipantDto(
    val id: String? = null,
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("vehicle_or_status_label") val vehicleOrStatusLabel: String,
    val role: String = "MOVING_VEHICLE",
    @SerialName("color_hex") val colorHex: String = "#38F0FC",
    @SerialName("is_sharing_location") val isSharingLocation: Boolean = true,
    @SerialName("joined_at") val joinedAt: String? = null
)

@Serializable
data class ParticipantLocationDto(
    @SerialName("participant_id") val participantId: String,
    @SerialName("trip_id") val tripId: String,
    val coords: String, // Formato PostGIS WKT: "SRID=4326;POINT(-35.7089 -9.6498)"
    @SerialName("speed_kmh") val speedKmh: Int = 0,
    @SerialName("is_separated") val isSeparated: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SeparationCheckResultDto(
    @SerialName("participant_id") val participantId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("distance_meters") val distanceMeters: Double,
    @SerialName("exceeded_threshold") val exceededThreshold: Boolean
)

@Serializable
data class UserProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("vehicle_label") val vehicleLabel: String,
    @SerialName("default_role") val defaultRole: String = "MOVING_VEHICLE",
    @SerialName("updated_at") val updatedAt: String? = null
)

