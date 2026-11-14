package com.letsdrive.app.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.letsdrive.app.model.LatLng
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

object AndroidLocationHolder {
    var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun hasLocationPermission(): Boolean {
        val ctx = appContext ?: return false
        val fine = ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }
}

actual class PlatformLocationTracker actual constructor() {

    @SuppressLint("MissingPermission")
    actual fun observeLocationUpdates(intervalMs: Long): Flow<LocationSample> = callbackFlow {
        val context = AndroidLocationHolder.appContext
        val locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        var receivedRealGps = false

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                receivedRealGps = true
                val speedKmh = if (location.hasSpeed()) {
                    (location.speed * 3.6f).roundToInt().coerceAtLeast(0)
                } else 68
                trySend(
                    LocationSample(
                        coordinates = LatLng(location.latitude, location.longitude),
                        speedKmh = speedKmh,
                        accuracyMeters = if (location.hasAccuracy()) location.accuracy else 6f,
                        isHardwareGps = true
                    )
                )
            }

            @Deprecated("Deprecated in Android API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (locationManager != null && AndroidLocationHolder.hasLocationPermission()) {
            runCatching {
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (lastKnown != null) {
                    receivedRealGps = true
                    val speedKmh = if (lastKnown.hasSpeed()) (lastKnown.speed * 3.6f).roundToInt() else 72
                    trySend(
                        LocationSample(
                            coordinates = LatLng(lastKnown.latitude, lastKnown.longitude),
                            speedKmh = speedKmh,
                            accuracyMeters = lastKnown.accuracy,
                            isHardwareGps = true
                        )
                    )
                }

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        intervalMs,
                        2f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        intervalMs,
                        5f,
                        listener,
                        Looper.getMainLooper()
                    )
                }
            }
        }

        // Loop de telemetria contínua (caso o GPS esteja em ambiente fechado sem satélite no momento)
        val fallbackJob = launch {
            var step = 0
            while (isActive) {
                delay(intervalMs)
                if (!receivedRealGps) {
                    step++
                    val progress = (step % 40) / 40.0
                    val lat = -9.6498 + (-9.5120 - (-9.6498)) * progress * 0.35
                    val lon = -35.7089 + (-35.6810 - (-35.7089)) * progress * 0.35
                    val simulatedSpeed = 84 + ((step % 5) - 2) * 2
                    trySend(
                        LocationSample(
                            coordinates = LatLng(lat, lon),
                            speedKmh = simulatedSpeed,
                            accuracyMeters = 8.0f,
                            isHardwareGps = false
                        )
                    )
                }
            }
        }

        awaitClose {
            fallbackJob.cancel()
            runCatching {
                locationManager?.removeUpdates(listener)
            }
        }
    }
}

