package com.letsdrive.app.data.notification

private fun jsRequestNotificationPermission(): Unit = js(
    "(() => { try { if (typeof Notification !== 'undefined' && Notification.permission === 'default') { Notification.requestPermission(); } } catch (e) {} })()"
)

private fun jsDispatchNotification(title: String, body: String): Unit = js(
    "(() => { try { if (typeof navigator !== 'undefined' && navigator.vibrate) { navigator.vibrate([200, 100, 200]); } try { var AudioCtx = window.AudioContext || window.webkitAudioContext; if (AudioCtx) { var ctx = new AudioCtx(); var osc = ctx.createOscillator(); var gain = ctx.createGain(); osc.type = 'sine'; osc.frequency.value = 880; gain.gain.value = 0.08; osc.connect(gain); gain.connect(ctx.destination); osc.start(); osc.stop(ctx.currentTime + 0.18); } } catch (audioErr) {} if (typeof Notification !== 'undefined') { if (Notification.permission === 'granted') { new Notification(title, { body: body, tag: 'lets-drive-alert' }); } else if (Notification.permission === 'default') { Notification.requestPermission().then(function(p) { if (p === 'granted') { new Notification(title, { body: body, tag: 'lets-drive-alert' }); } }); } } } catch (e) {} })()"
)

actual object PlatformNotifier {
    actual fun requestPermissionIfNeeded() {
        jsRequestNotificationPermission()
    }

    actual fun notifySeparationAlert(
        participantName: String,
        distanceKm: Double,
        thresholdKm: Double,
        meetingPointName: String?
    ) {
        jsDispatchNotification(
            title = NotificationMessages.separationTitle(participantName),
            body = NotificationMessages.separationBody(participantName, distanceKm, thresholdKm, meetingPointName)
        )
    }

    actual fun notifyMeetingPointConfirmed(
        meetingPointName: String,
        destinationName: String
    ) {
        jsDispatchNotification(
            title = NotificationMessages.meetingPointTitle(),
            body = NotificationMessages.meetingPointBody(meetingPointName, destinationName)
        )
    }
}

