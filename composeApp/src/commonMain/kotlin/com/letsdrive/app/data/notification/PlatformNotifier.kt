package com.letsdrive.app.data.notification

internal object NotificationMessages {
    fun separationTitle(participantName: String): String =
        "Alerta de separação: $participantName"

    fun separationBody(
        participantName: String,
        distanceKm: Double,
        thresholdKm: Double,
        meetingPointName: String?
    ): String {
        val base = "$participantName está a $distanceKm km do grupo (limite: $thresholdKm km)."
        return if (!meetingPointName.isNullOrBlank()) {
            "$base Ponto de reencontro sugerido: $meetingPointName."
        } else {
            "$base Verifique o mapa compartilhado."
        }
    }

    fun meetingPointTitle(): String = "Ponto de Reencontro Confirmado"

    fun meetingPointBody(meetingPointName: String, destinationName: String): String =
        "$meetingPointName definido para o grupo rumo a $destinationName."
}

expect object PlatformNotifier {
    fun requestPermissionIfNeeded()
    fun notifySeparationAlert(
        participantName: String,
        distanceKm: Double,
        thresholdKm: Double,
        meetingPointName: String? = null
    )
    fun notifyMeetingPointConfirmed(
        meetingPointName: String,
        destinationName: String
    )
}

