package com.letsdrive.app.data.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.letsdrive.app.MainActivity
import com.letsdrive.app.data.location.AndroidLocationHolder

actual object PlatformNotifier {
    private const val CHANNEL_ID = "lets_drive_separation_alerts"
    private const val CHANNEL_NAME = "Alertas de Distância da Rota"
    private const val NOTIFICATION_SEPARATION_ID = 4101
    private const val NOTIFICATION_MEETING_POINT_ID = 4102

    actual fun requestPermissionIfNeeded() {
        val ctx = AndroidLocationHolder.appContext ?: return
        ensureChannel(ctx)
    }

    actual fun notifySeparationAlert(
        participantName: String,
        distanceKm: Double,
        thresholdKm: Double,
        meetingPointName: String?
    ) {
        dispatchNotification(
            notificationId = NOTIFICATION_SEPARATION_ID,
            title = NotificationMessages.separationTitle(participantName),
            body = NotificationMessages.separationBody(participantName, distanceKm, thresholdKm, meetingPointName)
        )
    }

    actual fun notifyMeetingPointConfirmed(
        meetingPointName: String,
        destinationName: String
    ) {
        dispatchNotification(
            notificationId = NOTIFICATION_MEETING_POINT_ID,
            title = NotificationMessages.meetingPointTitle(),
            body = NotificationMessages.meetingPointBody(meetingPointName, destinationName)
        )
    }

    private fun ensureChannel(context: Context): NotificationManager? {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas quando alguém ultrapassa a distância máxima configurada na rota."
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        return manager
    }

    private fun hasPostNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun dispatchNotification(
        notificationId: Int,
        title: String,
        body: String
    ) {
        val ctx = AndroidLocationHolder.appContext ?: return
        if (!hasPostNotificationPermission(ctx)) return
        val manager = ensureChannel(ctx) ?: return

        runCatching {
            val intent = Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(ctx, notificationId, intent, pendingFlags)

            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(notificationId, notification)
        }
    }
}

