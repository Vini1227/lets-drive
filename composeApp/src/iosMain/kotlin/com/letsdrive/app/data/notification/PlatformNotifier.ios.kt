package com.letsdrive.app.data.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionAlert
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

actual object PlatformNotifier {
    private val foregroundDelegate = object : NSObject(), UNUserNotificationCenterDelegateProtocol {
        override fun userNotificationCenter(
            center: UNUserNotificationCenter,
            willPresentNotification: UNNotification,
            withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
        ) {
            withCompletionHandler(UNNotificationPresentationOptionAlert or UNNotificationPresentationOptionSound)
        }
    }

    private var delegateConfigured = false

    private fun ensureDelegate(): UNUserNotificationCenter {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        if (!delegateConfigured) {
            center.delegate = foregroundDelegate
            delegateConfigured = true
        }
        return center
    }

    actual fun requestPermissionIfNeeded() {
        val center = ensureDelegate()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { _, _ -> }
    }

    actual fun notifySeparationAlert(
        participantName: String,
        distanceKm: Double,
        thresholdKm: Double,
        meetingPointName: String?
    ) {
        scheduleNotification(
            identifier = "lets-drive-separation-alert",
            title = NotificationMessages.separationTitle(participantName),
            body = NotificationMessages.separationBody(participantName, distanceKm, thresholdKm, meetingPointName)
        )
    }

    actual fun notifyMeetingPointConfirmed(
        meetingPointName: String,
        destinationName: String
    ) {
        scheduleNotification(
            identifier = "lets-drive-meeting-point",
            title = NotificationMessages.meetingPointTitle(),
            body = NotificationMessages.meetingPointBody(meetingPointName, destinationName)
        )
    }

    private fun scheduleNotification(
        identifier: String,
        title: String,
        body: String
    ) {
        val center = ensureDelegate()
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = identifier,
            content = content,
            trigger = null
        )
        center.addNotificationRequest(request) { _ -> }
    }
}

